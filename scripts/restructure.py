"""
One-shot restructure script for 4.0.0.0 package reorganization.

What it does:
1. Updates `package ...;` declaration of every moved file to match its new subdir.
2. For every .java file under src/main/java, scans for class-name references
   that are now in a different subpackage and inserts the corresponding
   `import` lines (deduped, sorted).
3. Removes obsolete imports that point to the old flat path.

Idempotent: re-running on already-fixed sources is safe (it only adds missing
imports; never strips legitimate ones).
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src" / "main" / "java"
EXT_DIR = SRC / "com" / "baomidou" / "mybatisplus" / "extension"

# Subpackage layout. None of these subdirs share class names.
SUBPACKAGES = {
    "core": "com.baomidou.mybatisplus.extension.core",
    "wrapper": "com.baomidou.mybatisplus.extension.wrapper",
    "stream": "com.baomidou.mybatisplus.extension.stream",
    "value": "com.baomidou.mybatisplus.extension.value",
    "metadata": "com.baomidou.mybatisplus.extension.metadata",
    "support": "com.baomidou.mybatisplus.extension.support",
    "bo/functional": "com.baomidou.mybatisplus.extension.bo.functional",
    "bo/key": "com.baomidou.mybatisplus.extension.bo.key",
}

# Build class → new fqn map by scanning the filesystem
CLASS_TO_FQN: dict[str, str] = {}
for rel, pkg in SUBPACKAGES.items():
    for p in (EXT_DIR / rel).glob("*.java"):
        CLASS_TO_FQN[p.stem] = f"{pkg}.{p.stem}"

# Files still at extension/bo top level keep their old fqn — record them too so
# we can detect cross-package refs from those classes.
for p in (EXT_DIR / "bo").glob("*.java"):
    CLASS_TO_FQN[p.stem] = f"com.baomidou.mybatisplus.extension.bo.{p.stem}"

# Reverse: old fqn → new fqn (for every class that moved subpackage).
# Each class has an inferable "old fqn":
#   - classes now under extension/<sub>/ used to live at extension.<cls>
#   - classes now under extension/bo/<sub>/ used to live at extension.bo.<cls>
#   - classes still at extension/bo/ (top-level) did not move
OLD_FLAT_FQN_TO_NEW: dict[str, str] = {}
for cls, fqn in CLASS_TO_FQN.items():
    pkg = fqn.rsplit(".", 1)[0]
    if pkg == "com.baomidou.mybatisplus.extension.bo":
        continue  # bo top-level: never moved
    if pkg.startswith("com.baomidou.mybatisplus.extension.bo."):
        old_fqn = f"com.baomidou.mybatisplus.extension.bo.{cls}"
    else:
        old_fqn = f"com.baomidou.mybatisplus.extension.{cls}"
    if old_fqn != fqn:
        OLD_FLAT_FQN_TO_NEW[old_fqn] = fqn

PACKAGE_RE = re.compile(r"^package\s+([\w.]+)\s*;", re.M)
IMPORT_RE = re.compile(r"^import\s+(static\s+)?([\w.]+)\s*;", re.M)

# Strip strings, char literals, comments before scanning for class names so we
# don't pick up matches inside text content.
STRING_RE = re.compile(r'"(?:\\.|[^"\\])*"')
CHAR_RE = re.compile(r"'(?:\\.|[^'\\])'")
LINE_COMMENT_RE = re.compile(r"//[^\n]*")
BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.S)


def strip_noise(src: str) -> str:
    src = BLOCK_COMMENT_RE.sub("", src)
    src = LINE_COMMENT_RE.sub("", src)
    src = STRING_RE.sub('""', src)
    src = CHAR_RE.sub("''", src)
    return src


def derive_package_from_path(p: Path) -> str:
    rel = p.relative_to(SRC).with_suffix("")
    return ".".join(rel.parts[:-1])


def fix_package_declaration(p: Path) -> bool:
    """Ensure `package ...;` matches the file's actual directory."""
    expected = derive_package_from_path(p)
    text = p.read_text(encoding="utf-8")
    m = PACKAGE_RE.search(text)
    if not m:
        return False
    if m.group(1) == expected:
        return False
    new_text = text[: m.start()] + f"package {expected};" + text[m.end() :]
    p.write_text(new_text, encoding="utf-8")
    return True


def fix_imports(p: Path) -> tuple[int, int]:
    """Add missing imports for class refs that moved to other subpackages.

    Returns (added, removed) counts.
    """
    text = p.read_text(encoding="utf-8")
    cur_pkg = derive_package_from_path(p)
    body_no_noise = strip_noise(text)

    existing_imports: set[str] = set()
    for m in IMPORT_RE.finditer(text):
        existing_imports.add(m.group(2))
    # Simple-name set: if a class with this short name is already imported from
    # *some* package, leave it alone — adding a competing import would cause a
    # single-type-import collision (e.g. core.toolkit.StringUtils vs our own).
    imported_simple_names: set[str] = {
        fqn.rsplit(".", 1)[-1] for fqn in existing_imports
    }

    # Identify class refs (bare names in source code) that need imports
    needed_imports: set[str] = set()
    for cls, new_fqn in CLASS_TO_FQN.items():
        new_pkg = new_fqn.rsplit(".", 1)[0]
        if new_pkg == cur_pkg:
            continue  # same package — no import needed
        if cls in imported_simple_names:
            continue  # another fqn with same short name is already imported
        # bare-class-name reference (word boundary)
        if re.search(rf"\b{re.escape(cls)}\b", body_no_noise):
            if new_fqn not in existing_imports:
                needed_imports.add(new_fqn)

    # Replace any obsolete flat-path imports with the new fqn
    new_text = text
    removed = 0
    added_from_replace = 0
    for old_fqn, new_fqn in OLD_FLAT_FQN_TO_NEW.items():
        if old_fqn == new_fqn:
            continue
        pattern = rf"^import\s+{re.escape(old_fqn)}\s*;\s*\n"
        if re.search(pattern, new_text, re.M):
            new_text = re.sub(pattern, "", new_text, flags=re.M)
            removed += 1
            if new_fqn not in existing_imports:
                needed_imports.add(new_fqn)
                added_from_replace += 1

    # Refresh existing_imports after removals (they're no longer there)
    # Insert needed imports right after the last existing import block, or
    # right after the package declaration if no imports yet.
    if needed_imports:
        pkg_match = PACKAGE_RE.search(new_text)
        if not pkg_match:
            return (0, removed)
        # Find insertion point: after last import, else after package line
        last_import_end = pkg_match.end()
        for m in IMPORT_RE.finditer(new_text):
            last_import_end = max(last_import_end, m.end())
        # Skip trailing newline(s) at insertion point
        insert_at = last_import_end
        # Build import block (sorted)
        block = "\n" + "\n".join(f"import {fqn};" for fqn in sorted(needed_imports))
        new_text = new_text[:insert_at] + block + new_text[insert_at:]

    if new_text != text:
        p.write_text(new_text, encoding="utf-8")
    return (len(needed_imports), removed)


def main() -> int:
    java_files = list(SRC.rglob("*.java"))
    print(f"Scanning {len(java_files)} java files...")

    pkg_fixed = 0
    total_added = 0
    total_removed = 0
    for p in java_files:
        if fix_package_declaration(p):
            pkg_fixed += 1
        added, removed = fix_imports(p)
        total_added += added
        total_removed += removed

    print(f"  package declarations fixed: {pkg_fixed}")
    print(f"  imports added:              {total_added}")
    print(f"  obsolete imports replaced:  {total_removed}")
    print("\nDictionary used:")
    for cls in sorted(CLASS_TO_FQN):
        print(f"  {cls:50s} -> {CLASS_TO_FQN[cls]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
