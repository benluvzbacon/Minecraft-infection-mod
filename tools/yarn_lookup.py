#!/usr/bin/env python3
"""Developer helper: look up Yarn 1.21.1 mappings for a Minecraft class.

Usage:
    YARN_DIR=~/yarn-mappings python3 tools/yarn_lookup.py <Class> [memberFilter]

`<Class>` may be a simple name (`EntityType`), a nested name (`EntityType$Builder`)
or a package fragment (`block.A`).  Nested classes live inside their parent
mapping file, so those are searched too.

The mappings tree is expected at $YARN_DIR (default ~/yarn-mappings); clone the
1.21.1 branch of https://github.com/FabricMC/yarn and copy its `mappings`
directory there.  This tool exists so that API usage in this mod is checked
against the real 1.21.1 Yarn mappings instead of being guessed.
"""
import os
import sys

YARN = os.environ.get("YARN_DIR", os.path.expanduser("~/yarn-mappings"))
MAP = os.path.join(YARN, "mappings")


def files():
    for root, _dirs, names in os.walk(MAP):
        for n in names:
            if n.endswith(".mapping"):
                yield os.path.join(root, n)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    query = sys.argv[1]
    member = sys.argv[2].lower() if len(sys.argv) > 2 else None
    simple = query.split(".")[-1].split("$")[-1].lower()
    found = 0
    for path in files():
        with open(path) as fh:
            text = fh.read()
        lines = text.split("\n")
        # locate the CLASS block(s) whose named part matches
        idxs = []
        for i, line in enumerate(lines):
            if line.startswith("CLASS") or line.startswith("\tCLASS"):
                parts = line.split()
                named = parts[2] if len(parts) > 2 else parts[1]
                if named.split("/")[-1].lower() == simple:
                    idxs.append(i)
        if not idxs:
            continue
        if "." in query:
            want = query.split("$")[0].replace(".", "/")
            if not os.path.relpath(path, MAP)[:-len(".mapping")].endswith(want):
                continue
        rel = os.path.relpath(path, MAP)
        for start in idxs:
            indent = 1 if lines[start].startswith("\t") else 0
            end = len(lines)
            prefix = "\t" * (indent + 1)
            for j in range(start + 1, len(lines)):
                l = lines[j]
                if not l.strip():
                    continue
                depth = len(l) - len(l.lstrip("\t"))
                if depth <= indent and (l.startswith("CLASS") or l.startswith("\tCLASS")):
                    end = j
                    break
            print("=" * 72)
            print(rel.replace(".mapping", "").replace("/", ".") + " :: " + lines[start].strip())
            print("=" * 72)
            for l in lines[start + 1:end]:
                st = l.strip()
                if st.startswith("ARG ") or st.startswith("COMMENT") or l.startswith(prefix + "CLASS"):
                    continue
                if member and member not in l.lower():
                    continue
                print(" ".join(l.split())[:150])
            found += 1
    if not found:
        print("no mapping match for", query)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
