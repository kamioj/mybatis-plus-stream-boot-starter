"""
4.0 命名重构：去 Mysql 前缀（一次性脚本）。

替换映射：
    MysqlBaseMapper      -> StreamBaseMapper
    IMysqlServiceBase    -> IStreamService
    MysqlServiceBaseImpl -> StreamServiceImpl
    MysqlDataType        -> SqlDataType

只替换源代码里的"裸类名"（含 import 路径和类型引用）。Markdown 文档另行处理。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src" / "main" / "java"

RENAMES = {
    "MysqlBaseMapper":      "StreamBaseMapper",
    "IMysqlServiceBase":    "IStreamService",
    "MysqlServiceBaseImpl": "StreamServiceImpl",
    "MysqlDataType":        "SqlDataType",
}


def main() -> int:
    java_files = list(SRC.rglob("*.java"))
    print(f"Scanning {len(java_files)} java files...")

    touched = 0
    for p in java_files:
        text = p.read_text(encoding="utf-8")
        new_text = text
        for old_cls, new_cls in RENAMES.items():
            new_text = re.sub(rf"\b{old_cls}\b", new_cls, new_text)
        if new_text != text:
            p.write_text(new_text, encoding="utf-8")
            touched += 1
            print(f"  {p.relative_to(ROOT)}")

    print(f"\nTotal files touched: {touched}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
