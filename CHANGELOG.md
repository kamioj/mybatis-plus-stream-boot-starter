# Changelog

本项目所有发版记录。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## 版本号约定

`{MyBatis-Plus 版本}.{补丁号}`，例如 `3.5.16.0` 表示对齐 MyBatis-Plus 3.5.16 的第 0 个补丁。
升级 MyBatis-Plus 主线版本即新开 minor 段，本项目自身修复在 patch 段递增。

---

## [Unreleased]

（暂无）

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
