#!/usr/bin/env python3
"""Generate every Candy Infection JSON resource.

Blockstates, block models, item models, particle definitions, the language file,
recipes, block loot tables and tags are all derived from the same tables that the
Java registry uses, so the two can never drift apart.

Run from the repository root:

    python3 tools/generate_resources.py
"""

import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN = os.path.join(ROOT, "src", "main", "resources")
CLIENT = os.path.join(ROOT, "src", "client", "resources")
ASSETS = os.path.join(MAIN, "assets", "candyinfection")
DATA = os.path.join(MAIN, "data", "candyinfection")
NS = "candyinfection"

# ------------------------------------------------------------------ block table
# name -> model style
CUBE_BLOCKS = [
    "infected_grass_block", "infected_dirt", "candy_stone", "candy_bricks",
    "sugar_crystal_ore", "sugar_crystal_block", "candy_sand", "candy_gravel",
    "sticky_syrup", "caramel_growth", "gummy_planks", "gummy_leaves",
    "chocolate_blob", "lollipop_pink", "lollipop_blue", "lollipop_red",
    "lollipop_yellow", "hard_candy_pink", "hard_candy_red", "hard_candy_purple",
    "hard_candy_cyan", "infection_core", "purifier",
]
CROSS_BLOCKS = ["gummy_growth", "chocolate_growth", "candy_vines"]
PILLAR_BLOCKS = ["gummy_log"]
# sugar_crystal_cluster uses blockstate property "triple_size" (0..2)

ALL_BLOCKS = CUBE_BLOCKS + CROSS_BLOCKS + PILLAR_BLOCKS + ["sugar_crystal_cluster"]

# ------------------------------------------------------------------- item table
BLOCK_ITEMS = list(ALL_BLOCKS)

PLAIN_ITEMS = [
    "sugar_shard", "hardened_sugar", "gummy_resin", "caramel_chunk", "chocolate_core",
    "jawbreaker_fragment", "infection_crystal", "purification_crystal", "candy_essence",
    "holy_sugar",
    "gummy_worm", "candy_apple", "chocolate_bar", "giant_lollipop", "caramel_apple",
    "sugar_cookie", "jawbreaker_candy",
    "purification_potion", "anti_candy_syringe",
    "candy_pickaxe", "candy_axe", "candy_shovel", "candy_hoe", "candy_hammer",
    "sugar_sword", "jawbreaker_mace", "candy_bow", "gummy_spear", "chocolate_hammer",
    "caramel_blade",
    "candy_helmet", "candy_chestplate", "candy_leggings", "candy_boots",
    "hardened_sugar_helmet", "hardened_sugar_chestplate", "hardened_sugar_leggings",
    "hardened_sugar_boots",
    "confectioner_helmet", "confectioner_chestplate", "confectioner_leggings",
    "confectioner_boots",
    "confectioner_trophy", "candy_item",
]

# The boss entity id is the_confectioner, but the spawn egg ITEM is registered
# as confectioner_spawn_egg in CandyItems.java, so this list holds egg item ids.
SPAWN_EGGS = [
    "candy_crawler_spawn_egg", "gummy_spawn_spawn_egg", "gummy_brute_spawn_egg",
    "sugar_leech_spawn_egg", "candy_mimic_spawn_egg", "caramel_beast_spawn_egg",
    "jawbreaker_spawn_egg", "chocolate_creeper_spawn_egg", "lollipop_stalker_spawn_egg",
    "confectioner_spawn_egg",
]

ENTITIES = {
    "candy_crawler": "Candy Crawler",
    "gummy_spawn": "Gummy Spawn",
    "gummy_brute": "Gummy Brute",
    "sugar_leech": "Sugar Leech",
    "candy_mimic": "Candy Mimic",
    "caramel_beast": "Caramel Beast",
    "jawbreaker": "Jawbreaker",
    "chocolate_creeper": "Chocolate Creeper",
    "lollipop_stalker": "Lollipop Stalker",
    "the_confectioner": "The Confectioner",
    "candy_projectile": "Candy Projectile",
}

EFFECTS = {
    "sugar_craving": "Sugar Craving",
    "sticky": "Sticky",
    "caramel_coated": "Caramel Coated",
    "purified": "Purified",
    "sugar_rush": "Sugar Rush",
}

PARTICLES = {
    "infection_spark": ["infection_spark"],
    "purification_spark": ["purification_spark"],
    "candy_dust": ["candy_dust"],
    "gummy_droplet": ["gummy_droplet"],
    "chocolate_fragment": ["chocolate_fragment"],
    "sugar_sparkle": ["sugar_sparkle"],
    "caramel_bubble": ["caramel_bubble"],
}

PRETTY = {
    "infected_grass_block": "Infected Grass Block",
    "infected_dirt": "Infected Dirt",
    "candy_stone": "Candy Stone",
    "candy_bricks": "Candy Bricks",
    "sugar_crystal_ore": "Sugar Crystal Ore",
    "sugar_crystal_block": "Block of Sugar Crystal",
    "sugar_crystal_cluster": "Sugar Crystal Cluster",
    "candy_sand": "Candy Sand",
    "candy_gravel": "Candy Gravel",
    "sticky_syrup": "Sticky Syrup",
    "caramel_growth": "Caramel Growth",
    "gummy_log": "Gummy Log",
    "gummy_planks": "Gummy Planks",
    "gummy_leaves": "Gummy Leaves",
    "gummy_growth": "Gummy Growth",
    "chocolate_growth": "Chocolate Growth",
    "chocolate_blob": "Chocolate Blob",
    "candy_vines": "Candy Vines",
    "lollipop_pink": "Pink Lollipop",
    "lollipop_blue": "Blue Lollipop",
    "lollipop_red": "Red Lollipop",
    "lollipop_yellow": "Yellow Lollipop",
    "hard_candy_pink": "Pink Hard Candy",
    "hard_candy_red": "Red Hard Candy",
    "hard_candy_purple": "Purple Hard Candy",
    "hard_candy_cyan": "Cyan Hard Candy",
    "infection_core": "Candy Infection Core",
    "purifier": "Purifier",
    "sugar_shard": "Sugar Shard",
    "hardened_sugar": "Hardened Sugar",
    "gummy_resin": "Gummy Resin",
    "caramel_chunk": "Caramel Chunk",
    "chocolate_core": "Chocolate Core",
    "jawbreaker_fragment": "Jawbreaker Fragment",
    "infection_crystal": "Infection Crystal",
    "purification_crystal": "Purification Crystal",
    "candy_essence": "Candy Essence",
    "holy_sugar": "Holy Sugar",
    "gummy_worm": "Gummy Worm",
    "candy_apple": "Candy Apple",
    "chocolate_bar": "Chocolate Bar",
    "giant_lollipop": "Giant Lollipop",
    "caramel_apple": "Caramel Apple",
    "sugar_cookie": "Sugar Cookie",
    "jawbreaker_candy": "Jawbreaker Candy",
    "purification_potion": "Purification Potion",
    "anti_candy_syringe": "Anti-Candy Syringe",
    "candy_pickaxe": "Candy Pickaxe",
    "candy_axe": "Candy Axe",
    "candy_shovel": "Candy Shovel",
    "candy_hoe": "Candy Hoe",
    "candy_hammer": "Candy Hammer",
    "sugar_sword": "Sugar Sword",
    "jawbreaker_mace": "Jawbreaker Mace",
    "candy_bow": "Candy Bow",
    "gummy_spear": "Gummy Spear",
    "chocolate_hammer": "Chocolate Hammer",
    "caramel_blade": "Caramel Blade",
    "candy_helmet": "Candy Helmet",
    "candy_chestplate": "Candy Chestplate",
    "candy_leggings": "Candy Leggings",
    "candy_boots": "Candy Boots",
    "hardened_sugar_helmet": "Hardened Sugar Helmet",
    "hardened_sugar_chestplate": "Hardened Sugar Chestplate",
    "hardened_sugar_leggings": "Hardened Sugar Leggings",
    "hardened_sugar_boots": "Hardened Sugar Boots",
    "confectioner_helmet": "Confectioner Helmet",
    "confectioner_chestplate": "Confectioner Chestplate",
    "confectioner_leggings": "Confectioner Leggings",
    "confectioner_boots": "Confectioner Boots",
    "confectioner_trophy": "Confectioner Trophy",
    "candy_item": "Candy",
}

EGG_NAMES = {
    "candy_crawler_spawn_egg": "Candy Crawler Spawn Egg",
    "gummy_spawn_spawn_egg": "Gummy Spawn Spawn Egg",
    "gummy_brute_spawn_egg": "Gummy Brute Spawn Egg",
    "sugar_leech_spawn_egg": "Sugar Leech Spawn Egg",
    "candy_mimic_spawn_egg": "Candy Mimic Spawn Egg",
    "caramel_beast_spawn_egg": "Caramel Beast Spawn Egg",
    "jawbreaker_spawn_egg": "Jawbreaker Spawn Egg",
    "chocolate_creeper_spawn_egg": "Chocolate Creeper Spawn Egg",
    "lollipop_stalker_spawn_egg": "Lollipop Stalker Spawn Egg",
    "confectioner_spawn_egg": "The Confectioner Spawn Egg",
}
PRETTY.update(EGG_NAMES)


def pretty(name):
    return PRETTY.get(name, name.replace("_", " ").title())


# ------------------------------------------------------------------ file helper

def write(path, payload):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def blockstate(name, payload):
    write(os.path.join(ASSETS, "blockstates", name + ".json"), payload)


def block_model(name, payload):
    write(os.path.join(ASSETS, "models", "block", name + ".json"), payload)


def item_model(name, payload):
    write(os.path.join(ASSETS, "models", "item", name + ".json"), payload)


# ------------------------------------------------------------------- blockstates

def gen_blockstates():
    for name in CUBE_BLOCKS:
        blockstate(name, {"variants": {"": {"model": "%s:block/%s" % (NS, name)}}})

    for name in CROSS_BLOCKS:
        blockstate(name, {"variants": {"": {"model": "%s:block/%s" % (NS, name)}}})

    for name in PILLAR_BLOCKS:
        blockstate(name, {"variants": {
            "axis=x": {"model": "%s:block/%s_horizontal" % (NS, name), "x": 90, "y": 90},
            "axis=y": {"model": "%s:block/%s" % (NS, name)},
            "axis=z": {"model": "%s:block/%s_horizontal" % (NS, name), "x": 90},
        }})

    blockstate("sugar_crystal_cluster", {"variants": {
        "triple_size=0": {"model": "%s:block/sugar_crystal_cluster_0" % NS},
        "triple_size=1": {"model": "%s:block/sugar_crystal_cluster_1" % NS},
        "triple_size=2": {"model": "%s:block/sugar_crystal_cluster_2" % NS},
    }})


def gen_block_models():
    for name in CUBE_BLOCKS:
        block_model(name, {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": "%s:block/%s" % (NS, name)},
        })

    for name in CROSS_BLOCKS:
        block_model(name, {
            "parent": "minecraft:block/cross",
            "textures": {"cross": "%s:block/%s" % (NS, name)},
        })

    block_model("gummy_log", {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": "%s:block/gummy_planks" % NS, "side": "%s:block/gummy_log" % NS},
    })
    block_model("gummy_log_horizontal", {
        "parent": "minecraft:block/cube_column_horizontal",
        "textures": {"end": "%s:block/gummy_planks" % NS, "side": "%s:block/gummy_log" % NS},
    })

    for size in (0, 1, 2):
        block_model("sugar_crystal_cluster_%d" % size, {
            "parent": "minecraft:block/cross",
            "textures": {"cross": "%s:block/sugar_crystal_cluster" % NS},
        })


def gen_item_models():
    for name in BLOCK_ITEMS:
        parent = "%s:block/%s" % (NS, name)
        if name == "sugar_crystal_cluster":
            parent = "%s:block/sugar_crystal_cluster_2" % NS
        if name == "gummy_log":
            parent = "%s:block/gummy_log" % NS
        item_model(name, {"parent": parent})

    for name in PLAIN_ITEMS:
        item_model(name, {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "%s:item/%s" % (NS, name)},
        })

    for egg in SPAWN_EGGS:
        item_model(egg, {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "%s:item/%s" % (NS, egg)},
        })


def gen_particles():
    for name, textures in PARTICLES.items():
        write(os.path.join(ASSETS, "particles", name + ".json"),
              {"textures": ["%s:%s" % (NS, t) for t in textures]})


# ------------------------------------------------------------------------ lang

STAGE_KEYS = {
    "seeding": "Seeding",
    "spreading": "Spreading",
    "colonising": "Colonising",
    "blooming": "Blooming",
    "infesting": "Infesting",
    "overgrowth": "Overgrowth",
    "terminal": "Terminal",
}


def gen_lang():
    lang = {}
    lang["itemGroup.candyinfection.main"] = "Candy Infection"

    for name in ALL_BLOCKS:
        lang["block.candyinfection.%s" % name] = pretty(name)

    for name in BLOCK_ITEMS + PLAIN_ITEMS + SPAWN_EGGS:
        lang["item.candyinfection.%s" % name] = pretty(name)

    for key, value in ENTITIES.items():
        lang["entity.candyinfection.%s" % key] = value

    for key, value in EFFECTS.items():
        lang["effect.candyinfection.%s" % key] = value

    for key in PARTICLES:
        lang["particle.candyinfection.%s" % key] = key.replace("_", " ").title()

    lang["message.candyinfection.core_status"] = "Infection Core: stage %s, shield %s"
    lang["message.candyinfection.core_stage"] = "The Infection Core shudders - stage %s!"
    lang["message.candyinfection.core_shield"] = "Core shield reduced to %s"
    lang["message.candyinfection.core_destroyed"] = "An Infection Core has been destroyed! The land around it is purging."
    lang["message.candyinfection.purifier_status"] = "Purifier: %s fuel, clearing %s blocks"
    lang["message.candyinfection.purifier_refuelled"] = "Purifier refuelled (+%s)"
    lang["message.candyinfection.stumble"] = "Your legs give out as the sugar takes hold."
    lang["message.candyinfection.fully_infected"] = "You are completely infected. Find holy sugar or a purifier, fast."
    lang["message.candyinfection.tier"] = "Infection level: %s"
    lang["message.candyinfection.boss_spawned"] = "The Confectioner awakens. The colony has a defender."
    lang["message.candyinfection.boss_defeated"] = "The Confectioner collapses. The colony begins to purge."
    for key, value in STAGE_KEYS.items():
        lang["message.candyinfection.stage.%s" % key] = "The infection reaches stage %s." % value.upper()

    lang["event.candyinfection.candy_bloom"] = "Candy Bloom! The colony bursts into growth."
    lang["event.candyinfection.sugar_storm"] = "Sugar Storm! Crystals are falling from the sky."
    lang["event.candyinfection.infection_surge"] = "Infection Surge! The infection is spreading twice as fast."
    lang["event.candyinfection.gummy_migration"] = "Gummy Migration! Hordes are moving through the colony."
    lang["event.candyinfection.candyfall"] = "Candyfall! Candy is raining down."

    lang["death.attack.candyinfection.infection"] = "%s was consumed by the infection"
    lang["death.attack.candyinfection.infection.player"] = "%s was consumed by the infection whilst fighting %s"
    lang["death.attack.candyinfection.sugar"] = "%s's heart gave out from too much sugar"

    lang["subtitles.candyinfection.ambient"] = "Candy hums"
    lang["subtitles.candyinfection.core"] = "Infection Core pulses"

    write(os.path.join(ASSETS, "lang", "en_us.json"), lang)


# -------------------------------------------------------------------- recipes

def shaped(result, count, pattern, key):
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": key,
        "result": {"id": "%s:%s" % (NS, result), "count": count},
    }


def shapeless(result, count, ingredients):
    return {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": i} if ":" in i else {"item": "%s:%s" % (NS, i)} for i in ingredients],
        "result": {"id": "%s:%s" % (NS, result), "count": count},
    }


def smelting(result, ingredient, xp, time=200):
    return {
        "type": "minecraft:smelting",
        "ingredient": {"item": "%s:%s" % (NS, ingredient)},
        "result": {"id": "%s:%s" % (NS, result)},
        "experience": xp,
        "cookingtime": time,
    }


def gen_recipes():
    recipes = {}
    i = lambda name: {"item": "%s:%s" % (NS, name)}
    v = lambda name: {"item": "minecraft:%s" % name}

    # ---- materials -------------------------------------------------------
    recipes["hardened_sugar"] = smelting("hardened_sugar", "sugar_shard", 0.2)
    recipes["hardened_sugar_from_block"] = shaped("hardened_sugar", 9, ["B"], {"B": i("sugar_crystal_block")})
    recipes["sugar_crystal_block"] = shaped("sugar_crystal_block", 1, ["SSS", "SSS", "SSS"], {"S": i("sugar_shard")})
    recipes["sugar_shard_from_block"] = shapeless("sugar_shard", 9, ["sugar_crystal_block"])
    recipes["candy_bricks"] = shaped("candy_bricks", 4, ["SS", "SS"], {"S": i("candy_stone")})
    recipes["gummy_planks"] = shapeless("gummy_planks", 4, ["gummy_log"])
    recipes["chocolate_core"] = smelting("chocolate_core", "chocolate_blob", 0.4)
    recipes["caramel_chunk"] = smelting("caramel_chunk", "caramel_growth", 0.2)

    # ---- cures -----------------------------------------------------------
    recipes["purification_crystal"] = shaped("purification_crystal", 1,
                                             [" S ", "SCS", " S "],
                                             {"S": i("sugar_shard"), "C": v("amethyst_shard")})
    recipes["purification_potion"] = shaped("purification_potion", 2,
                                            ["PCP"],
                                            {"P": i("purification_crystal"), "C": v("glass_bottle")})
    recipes["anti_candy_syringe"] = shaped("anti_candy_syringe", 1,
                                           [" H ", "PIP", " N "],
                                           {"H": i("holy_sugar"), "P": i("purification_crystal"),
                                            "I": v("iron_ingot"), "N": v("gold_nugget")})
    recipes["holy_sugar"] = shaped("holy_sugar", 1,
                                   ["ECE", "CHC", "ECE"],
                                   {"E": i("candy_essence"), "C": i("purification_crystal"),
                                    "H": i("hardened_sugar")})
    recipes["candy_essence"] = shapeless("candy_essence", 1,
                                         ["infection_crystal", "sugar_shard", "purification_crystal"])

    # ---- purifier --------------------------------------------------------
    recipes["purifier"] = shaped("purifier", 1,
                                 ["PCP", "CIC", "PCP"],
                                 {"P": i("purification_crystal"), "C": i("candy_bricks"),
                                  "I": v("iron_block")})

    # ---- tools -----------------------------------------------------------
    recipes["candy_pickaxe"] = shaped("candy_pickaxe", 1,
                                      ["SSS", " T ", " T "],
                                      {"S": i("sugar_shard"), "T": v("stick")})
    recipes["candy_axe"] = shaped("candy_axe", 1,
                                  ["SS", "ST", " T"],
                                  {"S": i("sugar_shard"), "T": v("stick")})
    recipes["candy_shovel"] = shaped("candy_shovel", 1,
                                     ["S", "T", "T"],
                                     {"S": i("sugar_shard"), "T": v("stick")})
    recipes["candy_hoe"] = shaped("candy_hoe", 1,
                                  ["SS", " T", " T"],
                                  {"S": i("sugar_shard"), "T": v("stick")})
    recipes["candy_hammer"] = shaped("candy_hammer", 1,
                                     ["HHH", "HTH", " T "],
                                     {"H": i("hardened_sugar"), "T": v("stick")})

    # ---- weapons ---------------------------------------------------------
    recipes["sugar_sword"] = shaped("sugar_sword", 1,
                                    ["S", "S", "T"],
                                    {"S": i("sugar_shard"), "T": v("stick")})
    recipes["jawbreaker_mace"] = shaped("jawbreaker_mace", 1,
                                        ["JJJ", "JHJ", " T "],
                                        {"J": i("jawbreaker_fragment"), "H": i("hardened_sugar"),
                                         "T": v("stick")})
    recipes["candy_bow"] = shaped("candy_bow", 1,
                                  [" SR", "S R", " SR"],
                                  {"S": i("sugar_shard"), "R": i("gummy_resin")})
    recipes["gummy_spear"] = shaped("gummy_spear", 1,
                                    ["  H", " G ", "T  "],
                                    {"H": i("hardened_sugar"), "G": i("gummy_resin"), "T": v("stick")})
    recipes["chocolate_hammer"] = shaped("chocolate_hammer", 1,
                                         ["CCC", "CTC", " T "],
                                         {"C": i("chocolate_core"), "T": v("blaze_rod")})
    recipes["caramel_blade"] = shaped("caramel_blade", 1,
                                      [" C ", " C ", " R "],
                                      {"C": i("caramel_chunk"), "R": i("gummy_resin")})

    # ---- armour ----------------------------------------------------------
    armour_patterns = {
        "helmet": (["SSS", "S S"], 5),
        "chestplate": (["S S", "SSS", "SSS"], 8),
        "leggings": (["SSS", "S S", "S S"], 7),
        "boots": (["S S", "S S"], 4),
    }
    for tier, material in (("candy", "sugar_shard"), ("hardened_sugar", "hardened_sugar"),
                           ("confectioner", "chocolate_core")):
        for piece, (pattern, _count) in armour_patterns.items():
            recipes["%s_%s" % (tier, piece)] = shaped("%s_%s" % (tier, piece), 1,
                                                      pattern, {"S": i(material)})

    # ---- food ------------------------------------------------------------
    recipes["candy_apple"] = shaped("candy_apple", 1,
                                    [" S ", "SAS", " T "],
                                    {"S": i("sugar_shard"), "A": v("apple"), "T": v("stick")})
    recipes["caramel_apple"] = shaped("caramel_apple", 1,
                                      [" C ", "CAC", " T "],
                                      {"C": i("caramel_chunk"), "A": v("apple"), "T": v("stick")})
    recipes["chocolate_bar"] = shaped("chocolate_bar", 2,
                                      ["CC", "CC"],
                                      {"C": i("chocolate_core")})
    recipes["gummy_worm"] = shaped("gummy_worm", 4,
                                   ["RRR"],
                                   {"R": i("gummy_resin")})
    recipes["giant_lollipop"] = shaped("giant_lollipop", 1,
                                       ["HHH", "HHH", " T "],
                                       {"H": i("hardened_sugar"), "T": v("stick")})
    recipes["sugar_cookie"] = shaped("sugar_cookie", 8,
                                     ["WSW"],
                                     {"W": v("wheat"), "S": i("sugar_shard")})
    recipes["jawbreaker_candy"] = shaped("jawbreaker_candy", 2,
                                         ["JHJ"],
                                         {"J": i("jawbreaker_fragment"), "H": i("hardened_sugar")})

    for name, payload in recipes.items():
        write(os.path.join(DATA, "recipe", name + ".json"), payload)
    return len(recipes)


# ----------------------------------------------------------------- loot tables

def block_drop(name, extra=None):
    entries = [{
        "type": "minecraft:item",
        "name": "%s:%s" % (NS, name),
    }]
    if extra:
        entries.append(extra)
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": "%s:%s" % (NS, name)}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def gen_loot_tables():
    count = 0
    for name in ALL_BLOCKS:
        payload = block_drop(name)
        if name == "sugar_crystal_ore":
            payload = {
                "type": "minecraft:block",
                "pools": [{
                    "rolls": 1,
                    "bonus_rolls": 0,
                    "entries": [{
                        "type": "minecraft:alternatives",
                        "children": [
                            {
                                "type": "minecraft:item",
                                "name": "%s:sugar_crystal_ore" % NS,
                                "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                                    "predicates": {"minecraft:enchantments": [
                                        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}],
                            },
                            {
                                "type": "minecraft:item",
                                "name": "%s:sugar_shard" % NS,
                                "functions": [
                                    {"function": "minecraft:set_count",
                                     "count": {"type": "minecraft:uniform", "min": 2, "max": 4}},
                                    {"function": "minecraft:apply_bonus",
                                     "enchantment": "minecraft:fortune",
                                     "formula": "minecraft:ore_drops"},
                                    {"function": "minecraft:explosion_decay"},
                                ],
                            },
                        ],
                    }],
                }],
            }
        elif name == "gummy_leaves":
            payload = {
                "type": "minecraft:block",
                "pools": [{
                    "rolls": 1,
                    "bonus_rolls": 0,
                    "entries": [{
                        "type": "minecraft:alternatives",
                        "children": [
                            {"type": "minecraft:item", "name": "%s:gummy_leaves" % NS},
                            {
                                "type": "minecraft:item",
                                "name": "%s:gummy_resin" % NS,
                                "conditions": [{"condition": "minecraft:random_chance", "chance": 0.12}],
                            },
                        ],
                    }],
                }],
            }
        elif name == "infection_core":
            payload = {
                "type": "minecraft:block",
                "pools": [{
                    "rolls": 1,
                    "bonus_rolls": 0,
                    "entries": [{
                        "type": "minecraft:item",
                        "name": "%s:infection_crystal" % NS,
                        "functions": [{"function": "minecraft:set_count",
                                        "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}],
                    }],
                }],
            }
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), payload)
        count += 1
    return count


# ------------------------------------------------------------------------ tags

def gen_tags():
    def block_tag(name, values, replace=False):
        payload = {"values": values}
        if replace:
            payload["replace"] = True
        write(os.path.join(DATA, "tags", "block", name + ".json"), payload)

    def item_tag(name, values, replace=False):
        payload = {"values": values}
        if replace:
            payload["replace"] = True
        write(os.path.join(DATA, "tags", "item", name + ".json"), payload)

    # what the infection is allowed to overwrite (datapacks can extend this)
    block_tag("infectable", [
        "#minecraft:dirt", "#minecraft:sand", "#minecraft:base_stone_overworld",
        "#minecraft:stone_ore_replaceables", "#minecraft:deepslate_ore_replaceables",
        "#minecraft:logs", "#minecraft:leaves", "#minecraft:crops", "minecraft:gravel",
    ])

    # blocks that count as infection
    infected = ["%s:%s" % (NS, b) for b in ALL_BLOCKS if b not in ("purifier",)]
    block_tag("infected", infected)

    # never overwritten
    block_tag("infection_immune", [
        "minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:bedrock",
        "minecraft:barrier", "minecraft:bedrock", "minecraft:end_portal_frame",
        "minecraft:respawn_anchor", "minecraft:beacon", "minecraft:enchanting_table",
        "%s:purifier" % NS,
    ])

    # what charging monsters smash through
    block_tag("charge_breakable", [
        "#minecraft:leaves", "minecraft:tall_grass", "minecraft:short_grass",
        "minecraft:fern", "minecraft:large_fern", "#minecraft:flowers",
        "minecraft:snow", "minecraft:vine", "minecraft:glass", "minecraft:glass_pane",
        "%s:candy_vines" % NS, "%s:gummy_growth" % NS, "%s:chocolate_growth" % NS,
    ])

    # what the purifier accepts as fuel
    item_tag("purifier_fuel", [
        "%s:sugar_shard" % NS, "%s:hardened_sugar" % NS, "%s:gummy_resin" % NS,
        "%s:caramel_chunk" % NS, "%s:chocolate_core" % NS, "%s:candy_essence" % NS,
        "%s:infection_crystal" % NS,
    ])

    # entity immunity
    write(os.path.join(DATA, "tags", "entity_type", "infection_immune.json"),
          {"values": ["minecraft:villager", "minecraft:wandering_trader", "minecraft:iron_golem",
                      "minecraft:snow_golem", "minecraft:warden", "minecraft:ender_dragon",
                      "minecraft:wither"]})

    # vanilla mineable + tool tier tags so candy blocks break at the right speed
    pickaxe = [b for b in ALL_BLOCKS if b in (
        "candy_stone", "candy_bricks", "sugar_crystal_ore", "sugar_crystal_block",
        "sugar_crystal_cluster", "hard_candy_pink", "hard_candy_red", "hard_candy_purple",
        "hard_candy_cyan", "infection_core", "purifier")]
    axe = ["gummy_log", "gummy_planks"]
    shovel = ["infected_grass_block", "infected_dirt", "candy_sand", "candy_gravel"]
    hoe = ["gummy_leaves", "gummy_growth", "chocolate_growth", "candy_vines",
           "sticky_syrup", "caramel_growth", "chocolate_blob"]

    for tag_name, blocks in (("pickaxe", pickaxe), ("axe", axe), ("shovel", shovel), ("hoe", hoe)):
        payload = {
            "replace": False,
            "values": ["%s:%s" % (NS, b) for b in blocks],
        }
        path = os.path.join(MAIN, "data", "minecraft", "tags", "block", "mineable", tag_name + ".json")
        write(path, payload)

    write(os.path.join(MAIN, "data", "minecraft", "tags", "block", "needs_iron_tool.json"),
          {"replace": False,
           "values": ["%s:infection_core" % NS, "%s:purifier" % NS, "%s:sugar_crystal_ore" % NS]})
    write(os.path.join(MAIN, "data", "minecraft", "tags", "block", "needs_stone_tool.json"),
          {"replace": False,
           "values": ["%s:candy_stone" % NS, "%s:candy_bricks" % NS, "%s:sugar_crystal_block" % NS]})
    write(os.path.join(MAIN, "data", "minecraft", "tags", "block", "needs_diamond_tool.json"),
          {"replace": False, "values": []})

    # item tags used by vanilla recipes
    write(os.path.join(MAIN, "data", "minecraft", "tags", "item", "planks.json"),
          {"replace": False, "values": ["%s:gummy_planks" % NS]})
    write(os.path.join(MAIN, "data", "minecraft", "tags", "item", "logs.json"),
          {"replace": False, "values": ["%s:gummy_log" % NS]})
    write(os.path.join(MAIN, "data", "minecraft", "tags", "item", "leaves.json"),
          {"replace": False, "values": ["%s:gummy_leaves" % NS]})

    # creative inventory population
    write(os.path.join(DATA, "tags", "item", "creative_tab_contents.json"), {"values": []})


def main():
    gen_blockstates()
    gen_block_models()
    gen_item_models()
    gen_particles()
    gen_lang()
    recipes = gen_recipes()
    loot = gen_loot_tables()
    gen_tags()

    total = 0
    for base in (MAIN, CLIENT):
        for _, _, files in os.walk(base):
            for name in files:
                if name.endswith(".json"):
                    total += 1
    print("resources written: %d blocks, %d items, %d recipes, %d loot tables, %d json files total"
          % (len(ALL_BLOCKS), len(BLOCK_ITEMS) + len(PLAIN_ITEMS) + len(SPAWN_EGGS), recipes, loot, total))


if __name__ == "__main__":
    main()
