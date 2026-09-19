#!/usr/bin/env python3
"""Turn Gradle/javac output into one-line GitHub annotations plus a job summary.

GitHub's automatic javac parsing keeps only the first line of each diagnostic,
so ``cannot find symbol`` arrives without the symbol or location that makes it
actionable, and the raw log blob host is not always reachable. This script
re-joins each javac diagnostic into a single line and emits it both as an
``::error::`` annotation (readable through the check-run annotations API) and as
a markdown job summary.

Usage:  python3 tools/report_build_errors.py <gradle-output-file>
"""

import os
import re
import sys

START = re.compile(r"^(?P<path>[^\s].*?\.java):(?P<line>\d+):\s*(?:error|warning):\s*(?P<msg>.*)$")
CONTINUATION = re.compile(r"^\s+(symbol|location|required|found|reason|actual)\s*:")


def collect(lines):
    """Group javac diagnostics: header line + the indented detail lines."""
    blocks = []
    current = None
    for raw in lines:
        line = raw.rstrip("\n")
        match = START.match(line)
        if match:
            current = {
                "path": match.group("path"),
                "line": match.group("line"),
                "msg": match.group("msg").strip(),
                "detail": [],
            }
            blocks.append(current)
            continue
        if current is None:
            continue
        detail = CONTINUATION.match(line)
        if detail:
            current["detail"].append(line.strip())
        elif line.strip().startswith("^") or not line.strip():
            continue
        elif line.startswith(" ") and len(current["detail"]) < 4:
            # the offending source line, kept short for context
            current["detail"].append("code: " + line.strip()[:160])
        else:
            current = None
    return blocks


def relative(path):
    marker = os.sep + "src" + os.sep
    index = path.find(marker)
    return path[index + 1:] if index >= 0 else path


def main():
    if len(sys.argv) < 2:
        print("usage: report_build_errors.py <gradle-output-file>", file=sys.stderr)
        return 2
    with open(sys.argv[1], encoding="utf-8", errors="replace") as handle:
        lines = handle.readlines()

    blocks = collect(lines)
    errors = [b for b in blocks if True]

    # Also surface Gradle-level failures that are not javac diagnostics.
    gradle_failures = [l.strip() for l in lines
                       if re.match(r"^\s*(FAILURE:|What went wrong:|> Task .* FAILED)", l)]

    print("parsed %d javac diagnostics" % len(errors))

    summary = ["## Build errors", ""]
    if not errors and not gradle_failures:
        summary += ["No compile errors were parsed from the build output.", ""]

    for block in errors:
        detail = " | ".join(dict.fromkeys(block["detail"]))
        message = block["msg"] + ((" -- " + detail) if detail else "")
        print("::error file=%s,line=%s::%s" % (relative(block["path"]), block["line"], message))
        summary.append("- `%s:%s` %s" % (relative(block["path"]), block["line"], message))

    if gradle_failures:
        summary += ["", "### Gradle output", "```"]
        summary += gradle_failures[:20]
        summary.append("```")

    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as handle:
            handle.write("\n".join(summary) + "\n")
        print("wrote job summary to %s" % step_summary)

    return 0


if __name__ == "__main__":
    sys.exit(main())
