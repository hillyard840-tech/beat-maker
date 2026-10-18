#!/usr/bin/env python3
"""Check each skill folder against ClawHub's documented skill-format rules.

Usage: python3 tools/validate_skills.py [skills_root]
Exits non-zero if any skill has an error.
"""
import os
import re
import sys

NAME_RE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
SEMVER_RE = re.compile(r"^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$")
MAX_BUNDLE = 50 * 1024 * 1024
SKIP_DIRS = {"tools", "research", ".git", ".github"}


def frontmatter(text):
    if not text.startswith("---\n"):
        return None, text
    end = text.find("\n---\n", 4)
    if end == -1:
        return None, text
    return text[4:end], text[end + 5:]


def field(fm, key):
    m = re.search(rf"^{re.escape(key)}:\s*(.+)$", fm, re.M)
    return m.group(1).strip().strip('"').strip("'") if m else None


def check(skill_dir):
    errors, warnings = [], []
    folder = os.path.basename(skill_dir.rstrip("/"))
    path = os.path.join(skill_dir, "SKILL.md")
    if not os.path.isfile(path):
        return [f"missing SKILL.md"], warnings
    text = open(path, encoding="utf-8").read()
    fm, body = frontmatter(text)
    if fm is None:
        return ["SKILL.md has no YAML frontmatter between --- lines"], warnings

    name = field(fm, "name")
    desc = field(fm, "description")
    version = field(fm, "version")
    if not name:
        errors.append("frontmatter is missing name")
    elif not NAME_RE.match(name) or len(name) > 64:
        errors.append(f"name '{name}' must be 1-64 lowercase letters, digits, hyphens")
    elif name != folder:
        errors.append(f"name '{name}' does not match folder '{folder}'")
    if not desc:
        errors.append("frontmatter is missing description")
    elif len(desc) > 160:
        errors.append(f"description is {len(desc)} chars; keep it under 160")
    if version and not SEMVER_RE.match(version):
        errors.append(f"version '{version}' is not semver")
    if re.search(r"^(license|price|pricing)\s*:", fm, re.M | re.I):
        errors.append("license/pricing fields are not supported (ClawHub skills are MIT-0 and free)")

    for ref in re.findall(r"\{baseDir\}/([^\s`)]+)", body):
        if not os.path.exists(os.path.join(skill_dir, ref)):
            errors.append(f"referenced file not found: {ref}")

    total, files = 0, 0
    for root, dirs, names in os.walk(skill_dir):
        dirs[:] = [d for d in dirs if not d.startswith(".")]
        for n in names:
            if n.startswith(".") and n not in (".clawhubignore", ".gitignore"):
                warnings.append(f"hidden file will be skipped on publish: {n}")
                continue
            p = os.path.join(root, n)
            if os.path.islink(p):
                warnings.append(f"symlink will be skipped on publish: {n}")
                continue
            total += os.path.getsize(p)
            files += 1
    if total > MAX_BUNDLE:
        errors.append(f"bundle is {total} bytes; limit is 50MB")
    if files > 40:
        warnings.append(f"{files} files; only about 40 text files are indexed for search")

    secrets = re.findall(r"\$\{?([A-Z][A-Z0-9_]{3,})\}?", text)
    if secrets and "envVars" not in fm and "requires" not in fm:
        warnings.append(f"env vars used but not declared in metadata.openclaw: {sorted(set(secrets))}")
    return errors, warnings


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    failed = False
    for entry in sorted(os.listdir(root)):
        d = os.path.join(root, entry)
        if not os.path.isdir(d) or entry in SKIP_DIRS or entry.startswith("."):
            continue
        errors, warnings = check(d)
        status = "FAIL" if errors else "OK"
        print(f"[{status}] {entry}")
        for e in errors:
            print(f"   error: {e}")
        for w in warnings:
            print(f"   warning: {w}")
        failed = failed or bool(errors)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
