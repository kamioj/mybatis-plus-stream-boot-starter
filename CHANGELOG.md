# Changelog

本项目所有发版记录。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## 版本号约定

`{MyBatis-Plus 版本}.{补丁号}`，例如 `3.5.16.0` 表示对齐 MyBatis-Plus 3.5.16 的第 0 个补丁。
升级 MyBatis-Plus 主线版本即新开 minor 段，本项目自身修复在 patch 段递增。

---

## [Unreleased]

### ⚠️ BREAKING CHANGES — 包结构重组（4.0.0.0）

所有类按职责拆到 6 个子包，平铺的 `com.baomidou.mybatisplus.extension.*` 顶层不再有任何业务类：

```
extension/
├── core/         (4) ExQueryWrapper, ExecutableQueryWrapper, LambdaQueryWrapper, Converter
├── wrapper/      (26) 所有 *LambdaQueryWrapper（按"上下文 × 角色"网格命名）
├── stream/      (9) MybatisStream / MybatisQueryableStream{,1..5,Many} / MybatisExecutableStream
├── value/       (7) Single*Value 家族 + NonValue
├── metadata/    (6) ColumnInfo, TableInfo, MysqlDataType, ProcedureParam(Def), Struct
├── support/     (2) StringUtils, LambdaOrderItem
├── bo/
│   ├── functional/ (21) Function3..15, Consumer3..10
│   ├── key/        (4) MapKey3..5, BiMapKey
│   └── (root)      PageVo, SortVo, BiList（不动）
├── mapper/      MysqlBaseMapper（不动）
└── service/     IMysqlServiceBase + impl/（不动）
```

API 行为、方法签名、SQL 渲染、版本号约定 **完全不变**。只有 import 路径变化。

**迁移**：
- IDE 一键：`Ctrl+Alt+O`（IntelliJ）/ `Ctrl+Shift+O`（Eclipse）即可
- CLI 批量：见 [MIGRATION-4.0.md](MIGRATION-4.0.md) 的 `migrate-to-4.0.sh` / `.ps1`

### Added
- 每个子包都有 `package-info.java` 描述其职责和命名约定，方便 JavaDoc 与 IDE 导航
- `MIGRATION-4.0.md` —— 完整的旧→新路径表 + Bash/PowerShell 一键迁移脚本
- `dev-docs/ARITY-TEMPLATE.md` —— Function/Consumer/MapKey 新增 arity 的模板说明

### Internal
- 重组完全由 `scripts/restructure.py` 机械完成（保留 git mv 历史，方便 `git log --follow` 追溯）


## [3.5.16.0] - 2026-05-17

### Changed
- 升级 MyBatis-Plus 至 **3.5.16**（[#2](https://github.com/kamioj/mybatis-plus-stream-boot-starter/pull/2)）
- 同步更新 README 与 pom 中的版本声明

### Compatibility
- JDK 17+
- Spring Boot 3.x
- MyBatis-Plus 3.5.16（由本 starter 传递依赖）

## [3.5.9.0] - 2026-05-17

首个发布到 **Maven Central** 的版本（`io.github.kamioj:mybatis-plus-stream-boot-starter`）。

### Added
- **流式查询 API**：`stream().filter().sorted().limit().collect()` 风格的链式调用
- **连表查询**：`JoinLambdaQueryWrapper` 支持 LEFT / RIGHT / INNER / CROSS JOIN，Lambda 类型安全
- **聚合函数**：100+ SQL 函数封装（COUNT / SUM / AVG、字符串、日期、数学等）
- **分组查询**：`listGroup` / `pageGroupJoin` 一行完成 GROUP BY + 聚合
- **分页查询**：单表 / 连表 / 分组分页统一 API
- **逻辑删除开关**：`withDeleted()` 一键切换
- **批量写入扩展**：`saveBatchWithoutId` / `saveDuplicate` / `saveIgnore` / `saveReplace`
- **联合更新**：`updateJoin` 支持多表 JOIN UPDATE
- **零侵入接入**：`MysqlBaseMapper` 替换 `BaseMapper`、`IMysqlServiceBase` 继承 `IService` 即用

### Compatibility
- JDK 17+
- Spring Boot 3.x
- MyBatis-Plus 3.5.9

---

[Unreleased]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v3.5.16.0...HEAD
[3.5.16.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v3.5.9.0...v3.5.16.0
[3.5.9.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/releases/tag/v3.5.9.0
