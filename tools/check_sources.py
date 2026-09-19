#!/usr/bin/env python3
"""Structural lint for the Candy Infection Java sources.

A real compile is not possible in this sandbox: there is no ``javac`` (the local
runtime is a JRE) and every Maven/Mojang/Gradle host is unreachable, so no
Minecraft or Fabric jars can be fetched. This script runs the checks that do not
need a classpath:

* a Java-aware tokenizer that strips comments, string/char/text-block literals,
  then verifies brace/paren/bracket balance and reports the exact line of any
  imbalance or unterminated literal;
* package declaration matches the directory, and the primary type name matches
  the file name;
* every ``dev.candyinfection`` import resolves to a real source file;
* every ``X.member`` static reference into another mod class resolves to a
  member that class actually declares.

Usage:  python3 tools/check_sources.py
"""

import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE_ROOTS = [os.path.join(ROOT, "src", "main", "java"),
                os.path.join(ROOT, "src", "client", "java")]


def java_files():
    for base in SOURCE_ROOTS:
        for dirpath, _dirs, files in os.walk(base):
            for name in sorted(files):
                if name.endswith(".java"):
                    yield os.path.join(dirpath, name)


# ------------------------------------------------------------------- tokenizer

def tokenize(path):
    """Return (masked_text, errors).

    masked_text has every comment and literal replaced by spaces, so structural
    scanning cannot be fooled by braces inside strings or comments. Line numbers
    are preserved.
    """
    with open(path, encoding="utf-8") as handle:
        text = handle.read()
    out = list(text)
    errors = []
    i = 0
    n = len(text)

    def line_of(index):
        return text.count("\n", 0, index) + 1

    def blank(start, end):
        for k in range(start, min(end, n)):
            if text[k] != "\n":
                out[k] = " "

    while i < n:
        ch = text[i]
        if ch == "/" and i + 1 < n and text[i + 1] == "/":
            end = text.find("\n", i)
            end = n if end == -1 else end
            blank(i, end)
            i = end
        elif ch == "/" and i + 1 < n and text[i + 1] == "*":
            end = text.find("*/", i + 2)
            if end == -1:
                errors.append("unterminated block comment starting line %d" % line_of(i))
                blank(i, n)
                break
            blank(i, end + 2)
            i = end + 2
        elif text.startswith('"""', i):
            end = text.find('"""', i + 3)
            if end == -1:
                errors.append("unterminated text block starting line %d" % line_of(i))
                blank(i, n)
                break
            blank(i, end + 3)
            i = end + 3
        elif ch == '"':
            j = i + 1
            while j < n and text[j] != '"':
                if text[j] == "\\":
                    j += 1
                elif text[j] == "\n":
                    break
                j += 1
            if j >= n or text[j] != '"':
                errors.append("unterminated string literal on line %d" % line_of(i))
                blank(i, j)
                i = j
            else:
                blank(i, j + 1)
                i = j + 1
        elif ch == "'":
            j = i + 1
            while j < n and text[j] != "'":
                if text[j] == "\\":
                    j += 1
                elif text[j] == "\n":
                    break
                j += 1
            if j >= n or text[j] != "'":
                errors.append("unterminated char literal on line %d" % line_of(i))
                blank(i, j)
                i = j
            else:
                blank(i, j + 1)
                i = j + 1
        else:
            i += 1
    return "".join(out), errors


def check_balance(path, masked):
    errors = []
    stack = []
    pairs = {"}": "{", ")": "(", "]": "["}
    line_starts = [0]
    for k, ch in enumerate(masked):
        if ch == "\n":
            line_starts.append(k + 1)

    def line_of(index):
        lo, hi = 0, len(line_starts) - 1
        while lo < hi:
            mid = (lo + hi + 1) // 2
            if line_starts[mid] <= index:
                lo = mid
            else:
                hi = mid - 1
        return lo + 1

    for index, ch in enumerate(masked):
        if ch in "({[":
            stack.append((ch, index))
        elif ch in ")}]":
            if not stack:
                errors.append("unexpected '%s' at line %d" % (ch, line_of(index)))
            elif stack[-1][0] != pairs[ch]:
                errors.append("'%s' at line %d does not match '%s' opened at line %d"
                              % (ch, line_of(index), stack[-1][0], line_of(stack[-1][1])))
                stack.pop()
            else:
                stack.pop()
    for ch, index in stack:
        errors.append("unclosed '%s' opened at line %d" % (ch, line_of(index)))
    return errors


# --------------------------------------------------------------- structure checks

def fqn_of(path):
    rel = os.path.relpath(path, ROOT)
    for prefix in (os.path.join("src", "main", "java") + os.sep,
                   os.path.join("src", "client", "java") + os.sep):
        if rel.startswith(prefix):
            rel = rel[len(prefix):]
    return rel[:-5].replace(os.sep, ".")


DECL_RE = re.compile(
    r"^\s*(?:public|protected|private|static|final|abstract|sealed|non-sealed|strictfp|\s)*"
    r"(?:class|interface|enum|record|@interface)\s+(\w+)", re.MULTILINE)


def check_structure(path, masked):
    errors = []
    with open(path, encoding="utf-8") as handle:
        original = handle.read()

    package = re.search(r"^package\s+([\w.]+)\s*;", original, re.MULTILINE)
    expected_pkg = fqn_of(path).rsplit(".", 1)[0]
    if package is None:
        errors.append("missing package declaration (expected '%s')" % expected_pkg)
    elif package.group(1) != expected_pkg:
        errors.append("package '%s' does not match directory '%s'"
                      % (package.group(1), expected_pkg))

    names = DECL_RE.findall(masked)
    expected_type = os.path.basename(path)[:-5]
    if not names:
        errors.append("no top-level type declaration found")
    elif expected_type not in names:
        errors.append("file declares %s but is named %s.java" % (names, expected_type))
    return errors


# ---------------------------------------------------------------- cross references

def declared_members(masked):
    """Every identifier that appears as a method or field name in this file."""
    members = set(re.findall(r"\b([a-z_]\w*)\s*\(", masked))
    members |= set(re.findall(r"\b(?:[A-Za-z_][\w<>\[\], .?]*?)\s+([a-z_]\w*)\s*[=;]", masked))
    members |= set(re.findall(r"\b([A-Z][A-Z0-9_]*)\b", masked))
    return members


def check_imports(paths, fqns):
    problems = []
    for path in paths:
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        for match in re.finditer(r"^import\s+(?:static\s+)?(dev\.candyinfection\.[\w.]+)\s*;",
                                 text, re.MULTILINE):
            target = match.group(1)
            if target in fqns:
                continue
            parts = target.split(".")
            for cut in range(len(parts) - 1, 2, -1):
                if ".".join(parts[:cut]) in fqns:
                    break
            else:
                problems.append((path, target))
    return problems


SKIP_MEMBERS = {"class", "super", "this", "values", "valueOf", "length", "ordinal", "name"}


def check_static_refs(paths, member_map):
    problems = []
    pattern = re.compile(r"\b([A-Z]\w{2,})\.([a-z_]\w*)\b")
    for path in paths:
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        masked, _ = tokenize(path)
        package = re.search(r"^package\s+([\w.]+)\s*;", text, re.MULTILINE)
        package = package.group(1) if package else ""
        imported = {}
        for match in re.finditer(r"^import\s+(dev\.candyinfection\.[\w.]+)\s*;", text, re.MULTILINE):
            fqn = match.group(1)
            imported[fqn.split(".")[-1]] = fqn
        for cls, member in pattern.findall(masked):
            if member in SKIP_MEMBERS:
                continue
            fqn = imported.get(cls)
            if fqn is None and package + "." + cls in member_map:
                fqn = package + "." + cls
            if fqn is None or fqn not in member_map:
                continue
            if member not in member_map[fqn]:
                problems.append((path, "%s.%s" % (cls, member)))
    return problems


# --------------------------------------------------------------------------- main

def main():
    paths = list(java_files())
    print("checking %d java files" % len(paths))

    member_map = {}
    syntax_errors = []
    for path in paths:
        masked, lex_errors = tokenize(path)
        syntax_errors.extend((path, e) for e in lex_errors)
        syntax_errors.extend((path, e) for e in check_balance(path, masked))
        syntax_errors.extend((path, e) for e in check_structure(path, masked))
        member_map[fqn_of(path)] = declared_members(masked)

    print("structural errors: %d" % len(syntax_errors))
    for path, error in syntax_errors:
        print("  %s: %s" % (os.path.relpath(path, ROOT), error))

    fqns = set(member_map)
    import_problems = check_imports(paths, fqns)
    print("unresolved dev.candyinfection imports: %d" % len(import_problems))
    for path, target in import_problems:
        print("  %s -> %s" % (os.path.relpath(path, ROOT), target))

    ref_problems = sorted(set(check_static_refs(paths, member_map)))
    print("unresolved intra-mod static references: %d" % len(ref_problems))
    for path, ref in ref_problems:
        print("  %s -> %s" % (os.path.relpath(path, ROOT), ref))

    total = len(syntax_errors) + len(import_problems) + len(ref_problems)
    print("RESULT: %s" % ("clean" if total == 0 else "%d problem(s)" % total))
    return 0


if __name__ == "__main__":
    sys.exit(main())
