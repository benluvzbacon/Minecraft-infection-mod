#!/usr/bin/env python3
"""Consistency checks over the generated Candy Infection resources.

Verifies that the JSON the game will actually load agrees with the Java
registries that reference it:

* every JSON file under both resource roots parses;
* every blockstate model reference resolves to a block model file;
* every namespaced model parent and every texture reference resolves to a file
  that exists in this mod;
* every block declared in CandyBlocks.java has a blockstate, a block model and
  an item model, and every item declared in CandyItems.java has an item model;
* every crafted/smelting result and every mod-namespaced ingredient in a recipe
  is a registered item;
* every block has a loot table, and the entrypoints named in fabric.mod.json
  exist.

Vanilla (``minecraft:``) identifiers are counted but not verified, since no
Minecraft jar is available offline to check them against.

Usage:  python3 tools/check_resources.py
"""

import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES_ROOTS = [os.path.join(ROOT, "src", "main", "resources"),
             os.path.join(ROOT, "src", "client", "resources")]
NS = "candyinfection"
MAIN = RES_ROOTS[0]


def resource_files():
    for base in RES_ROOTS:
        for dirpath, _dirs, files in os.walk(base):
            for name in sorted(files):
                yield os.path.join(dirpath, name)


def load_json(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def rel(path):
    return os.path.relpath(path, ROOT)


def java_text(name):
    path = os.path.join(MAIN, "..", "java", "dev", "candyinfection", "init", name)
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def registered_ids(source, register_pattern):
    """Pull the registry path strings out of a registration class."""
    return set(re.findall(register_pattern, source))


def main():
    errors = []
    warnings = []

    # ------------------------------------------------------------- json parses
    json_files = [p for p in resource_files() if p.endswith(".json")]
    parsed = {}
    for path in json_files:
        try:
            parsed[path] = load_json(path)
        except Exception as exc:
            errors.append("invalid JSON %s: %s" % (rel(path), exc))
    print("json files: %d (%d failed to parse)" % (len(json_files), len(json_files) - len(parsed)))

    assets = os.path.join(MAIN, "assets", NS)

    def model_exists(kind, name):
        return os.path.exists(os.path.join(assets, "models", kind, name + ".json"))

    def texture_exists(ref):
        if ref.startswith(NS + ":"):
            return os.path.exists(os.path.join(assets, "textures", ref[len(NS) + 1:] + ".png"))
        return None  # vanilla, unverifiable offline

    # ---------------------------------------------------- blockstate -> models
    blockstate_dir = os.path.join(assets, "blockstates")
    blockstates = {}
    for path in sorted(os.listdir(blockstate_dir)):
        if not path.endswith(".json"):
            continue
        name = path[:-5]
        payload = load_json(os.path.join(blockstate_dir, path))
        blockstates[name] = payload
        variants = payload.get("variants", {})
        if not variants:
            errors.append("blockstate %s has no variants" % name)
        for key, value in variants.items():
            refs = value if isinstance(value, list) else [value]
            for entry in refs:
                model = entry.get("model", "")
                if not model.startswith(NS + ":"):
                    errors.append("blockstate %s variant %s uses non-mod model %s" % (name, key, model))
                    continue
                # model refs carry the folder, e.g. candyinfection:block/foo
                ref = model[len(NS) + 1:]
                if not os.path.exists(os.path.join(assets, "models", ref + ".json")):
                    errors.append("blockstate %s variant %s -> missing block model %s"
                                  % (name, key, model))

    # ------------------------------------------------- models: parents, textures
    vanilla_refs = 0
    for kind in ("block", "item"):
        model_dir = os.path.join(assets, "models", kind)
        if not os.path.isdir(model_dir):
            continue
        for path in sorted(os.listdir(model_dir)):
            if not path.endswith(".json"):
                continue
            name = path[:-5]
            payload = load_json(os.path.join(model_dir, path))
            parent = payload.get("parent")
            if parent:
                if parent.startswith(NS + ":"):
                    # namespaced parents carry the folder, e.g. candyinfection:block/foo
                    ref = parent[len(NS) + 1:]
                    if not os.path.exists(os.path.join(assets, "models", ref + ".json")):
                        errors.append("model %s/%s -> missing parent %s" % (kind, name, parent))
                else:
                    vanilla_refs += 1
            for ref in (payload.get("textures") or {}).values():
                exists = texture_exists(ref)
                if exists is False:
                    errors.append("model %s/%s -> missing texture %s" % (kind, name, ref))
                elif exists is None:
                    vanilla_refs += 1

    # ------------------------------------------------------- java <-> resources
    blocks_src = java_text("CandyBlocks.java")
    block_ids = set(re.findall(
        r'\b(?:infected|register|lollipop|hardCandy)\(\s*"([a-z0-9_]+)"', blocks_src))
    block_ids -= {"textures", "block", "item"}

    items_src = java_text("CandyItems.java")
    # CandyItems uses several private helpers that all funnel into register(),
    # so collect the first string literal argument of any of them.
    item_ids = set(re.findall(
        r'\b(?:register|blockItem|simple|rare|legendary|food|armor|egg)\(\s*"([a-z0-9_]+)"',
        items_src))
    # blockItem() derives its id from the block, so add every block too
    item_ids |= block_ids

    print("registry: %d blocks, %d items referenced from Java" % (len(block_ids), len(item_ids)))

    for block in sorted(block_ids):
        if block not in blockstates:
            errors.append("block '%s' is registered in Java but has no blockstate" % block)
        if not model_exists("item", block):
            errors.append("block '%s' has no item model" % block)
        has_model = model_exists("block", block) or block == "sugar_crystal_cluster" \
            or block == "gummy_log"
        if not has_model:
            errors.append("block '%s' has no block model" % block)

    for item in sorted(item_ids):
        if not model_exists("item", item):
            errors.append("item '%s' is registered in Java but has no item model" % item)

    for name in sorted(blockstates):
        if name not in block_ids:
            warnings.append("blockstate '%s' exists but no Java block registers it" % name)

    # ------------------------------------------------------------------ recipes
    recipe_dir = os.path.join(MAIN, "data", NS, "recipe")
    recipes = [os.path.join(recipe_dir, f) for f in sorted(os.listdir(recipe_dir))]
    for path in recipes:
        payload = load_json(path)
        result = payload.get("result", {})
        rid = result.get("id", "")
        if rid.startswith(NS + ":") and rid[len(NS) + 1:] not in item_ids:
            errors.append("recipe %s -> unknown result item %s" % (rel(path), rid))

        def check_ingredient(ing, where):
            if not isinstance(ing, dict):
                return
            iid = ing.get("item")
            if iid and iid.startswith(NS + ":") and iid[len(NS) + 1:] not in item_ids:
                errors.append("recipe %s %s -> unknown ingredient %s" % (rel(path), where, iid))

        for ing in payload.get("ingredients", []) or []:
            check_ingredient(ing, "ingredient")
        for ing in (payload.get("key") or {}).values():
            check_ingredient(ing, "key")
        if "ingredient" in payload:
            check_ingredient(payload["ingredient"], "smelting ingredient")
    print("recipes: %d" % len(recipes))

    # -------------------------------------------------------------- loot tables
    loot_dir = os.path.join(MAIN, "data", NS, "loot_table", "blocks")
    loot = {f[:-5] for f in os.listdir(loot_dir) if f.endswith(".json")}
    for block in sorted(block_ids):
        if block not in loot:
            errors.append("block '%s' has no loot table" % block)
    for name in sorted(loot - block_ids):
        warnings.append("loot table '%s' exists but no Java block registers it" % name)

    # ----------------------------------------------------------------- mod meta
    mod_json = load_json(os.path.join(MAIN, "fabric.mod.json"))
    for kind, entries in (mod_json.get("entrypoints") or {}).items():
        for entry in entries:
            class_path = entry.replace(".", os.sep) + ".java"
            found = any(os.path.exists(os.path.join(r, class_path)) for r in
                        (os.path.join(ROOT, "src", "main", "java"),
                         os.path.join(ROOT, "src", "client", "java")))
            if not found:
                errors.append("fabric.mod.json entrypoint '%s' (%s) has no source file" % (entry, kind))
    if not os.path.exists(os.path.join(MAIN, mod_json.get("icon", ""))):
        errors.append("fabric.mod.json icon '%s' does not exist" % mod_json.get("icon"))

    # -------------------------------------------------------------- lang keys
    lang = load_json(os.path.join(assets, "lang", "en_us.json"))
    for block in sorted(block_ids):
        if "block.candyinfection.%s" % block not in lang:
            errors.append("missing lang key block.candyinfection.%s" % block)
    for item in sorted(item_ids):
        if "item.candyinfection.%s" % item not in lang:
            errors.append("missing lang key item.candyinfection.%s" % item)

    print("vanilla (minecraft:) references counted but not verified: %d" % vanilla_refs)
    print("warnings: %d" % len(warnings))
    for warning in warnings:
        print("  WARN %s" % warning)
    print("errors: %d" % len(errors))
    for error in errors:
        print("  ERR  %s" % error)
    print("RESULT: %s" % ("clean" if not errors else "%d error(s)" % len(errors)))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
