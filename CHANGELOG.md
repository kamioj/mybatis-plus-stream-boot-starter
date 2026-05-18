# Changelog

本项目所有发版记录。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## 版本号约定

`{MyBatis-Plus 版本}.{补丁号}`，例如 `3.5.16.0` 表示对齐 MyBatis-Plus 3.5.16 的第 0 个补丁。
升级 MyBatis-Plus 主线版本即新开 minor 段，本项目自身修复在 patch 段递增。

---

## [Unreleased]

（暂无）

## [4.0.0.0] - 2026-05-18

### ⚠️ BREAKING CHANGES

本版本是一次大重构，三大类 breaking change。**首次升级建议读 [MIGRATION-4.0.md](MIGRATION-4.0.md) 一键迁移**。

#### 1. 包结构重组：54 个类按职责拆到 6 个子包

```
extension/
├── core/        ExQueryWrapper, ExecutableQueryWrapper, LambdaQueryWrapper, Converter
├── wrapper/     所有 *LambdaQueryWrapper（按"上下文 × 角色"网格命名）
├── stream/      MybatisStream / MybatisQueryableStream{,1..5,Many} / MybatisExecutableStream
├── value/       Single*Value 家族 + NonValue
├── metadata/    ColumnInfo, TableInfo, SqlDataType, ProcedureParam(Def), Struct
├── support/     StringUtils, LambdaOrderItem
├── dialect/     SqlDialect SPI + DbType + DialectRegistry + MySqlDialect
├── bo/
│   ├── functional/  Function3..15, Consumer3..10
│   ├── key/         MapKey3..5, BiMapKey
│   └── (root)       PageVo, SortVo, BiList（不动）
├── mapper/      StreamBaseMapper（原 MysqlBaseMapper）
└── service/     IStreamService + impl/StreamServiceImpl
```

#### 2. 类名去 `Mysql` 前缀（产品定位"多方言流式构建器"）

| 旧名 | 新名 |
|---|---|
| `MysqlBaseMapper` | `StreamBaseMapper` |
| `IMysqlServiceBase` | `IStreamService` |
| `MysqlServiceBaseImpl` | `StreamServiceImpl` |
| `MysqlDataType` | `SqlDataType` |

前 3 个保留 `@Deprecated(forRemoval = true, since = "4.0")` 别名空壳到 4.1，方便渐进式升级；`MysqlDataType` 是枚举无法 extends，需要一次性 sed 替换。

#### 3. `LambdaQueryWrapper.getSubSqlSegment` 抛 `ReflectiveOperationException`

原来抛 `InstantiationException | IllegalAccessException` 的子查询拼装方法，4.0 升级到 `getDeclaredConstructor().newInstance()` 后改抛更广义的 `ReflectiveOperationException`。继承本库 wrapper 自定义子类的用户可能需要更新 catch 块。

### Added

#### 方言 SPI（`extension/dialect/`）
- `SqlDialect` 接口：分页 / 字符串拼接 / 聚合字符串 / 类型转换 / 行锁 / 标识符引用 / UPSERT 能力声明
- `DbType` 枚举（MYSQL / POSTGRESQL / DAMENG / CUSTOM）
- `DialectRegistry` 注册表 + `ServiceLoader` 自动加载用户自定义方言
- `MySqlDialect` 默认实现（行为与 3.x 100% 一致）
- 用户在 `META-INF/services/com.baomidou.mybatisplus.extension.dialect.SqlDialect` 声明实现即可热插拔，无需修改本库源码
- **本库自身不引入任何 JDBC 驱动**，方言只负责 SQL 字符串拼装
- PostgreSQL / DM 方言：规划 4.0.1 内置（社区可自行实现并 PR）

#### SQL 表达力补齐
- **EXISTS / NOT EXISTS**：`AbstractWhereLambdaQueryWrapper.exists(subSql)` / `.notExists(subSql)` 在 WHERE 上下文显式使用 EXISTS 谓词
- **行锁细粒度**：`MybatisQueryableStream.forUpdateNoWait()` / `.forUpdateSkipLocked()` / `.forUpdateWait(int seconds)` 对应 SQL `FOR UPDATE NOWAIT | SKIP LOCKED | WAIT n`

#### **SQL-aware Terminal Operations**（基于 `ceremonyproapp` 50+ 真实绕过案例驱动）

在 `MybatisQueryableStream` 基类新增 9 个 terminal 方法，让用户不再需要"`service.list().stream().collect(JDK Collector)`"三段式全量加载：

| 方法 | SQL 行为 |
|---|---|
| `toSet(col)` | `SELECT col FROM ...` |
| `toMap(keyCol, valCol)` | `SELECT keyCol, valCol FROM ...`（key 冲突时后者覆盖前者）|
| `toMap(k, v, merger)` | 同上 + 自定义合并策略 |
| `groupingBy(keyCol)` | `SELECT * FROM ...` + 应用层按 keyCol 分组保留完整实体 |
| `toMapCount(keyCol)` | `SELECT keyCol, COUNT(*) FROM ... GROUP BY keyCol`（**SQL 真下推**）|
| `toMapSum(keyCol, sumCol)` | `SELECT keyCol, SUM(sumCol) GROUP BY keyCol` |
| `toMapAvg(keyCol, avgCol)` | `SELECT keyCol, AVG(avgCol) GROUP BY keyCol`（返回 `Map<K, Double>`）|
| `toMapMax(keyCol, maxCol)` | `SELECT keyCol, MAX(maxCol) GROUP BY keyCol` |
| `toMapMin(keyCol, minCol)` | `SELECT keyCol, MIN(minCol) GROUP BY keyCol` |

设计原则：
- 命名与 JDK `Collectors.toMap/toSet/groupingBy` 接近，开发者已会
- 接 `SFunction<T, ?>` 而非 lambda，列名解析后下推 SQL
- "SQL 一目了然" 兑现：方法名 → SQL 关键字直接映射
- **零 breaking**：纯增量；旧 `.list().stream().collect(...)` 路径继续工作

依赖新增的 `MybatisUtil.propertyOf(SFunction)` / `columnOf(SFunction)` / `readProperty(entity, name)` 三个 public helpers（列名带方言引号，由 `DialectRegistry.current().quoteIdentifier(...)` 提供）。

#### 文档
- 每个子包都有 `package-info.java` 描述职责
- `MIGRATION-4.0.md`：完整的旧→新路径表 + Bash/PowerShell 一键迁移脚本
- `dev-docs/ARITY-TEMPLATE.md`：Function/Consumer/MapKey 新增 arity 的模板说明

### Removed

- **`Function3..15` + `Consumer3..10` 共 21 个类全部移除**。基于生产项目 `ceremonyproapp` 实测，这两族类业务代码 0 调用——是预防性 over-engineering。`BiMapKey` / `MapKey3..5` **保留**（生产 70+ 引用，真实热点）。  
  外部用户如有 import 这两个族，4.0 会编译错误；请重构（业务上几乎不可能用得到这些类，因为 starter 的高阶 API 已用 `Object[]` 投影避开了 FunctionN）。

### Performance

- `ReflectUtils.getLambda` 加 `ClassValue<Method>` 缓存：同一 lambda 表达式产生同一 JVM 内部类，缓存命中后无需重复反射 lookup（生产负载下命中率 &gt; 99%）
- `LambdaQueryWrapper.getTable` 用字符串切片代替正则提取类名（`indexOf("(L")+2` / `indexOf(";)")`），省正则编译开销
- `StringUtils.regexMatcher` 用 `ConcurrentHashMap<String, Pattern>` 缓存 Pattern，避免热路径重复编译
- `MybatisUtil.getTableInfo` 从 `synchronized HashMap` 升级到 `ClassValue<TableInfo>`，**无锁**线程局部缓存

### Changed (Internal)

- `clazz.newInstance()`（JDK 9+ 已废弃）3 处全部升级到 `getDeclaredConstructor().newInstance()`
- `ExQueryWrapper.last(LIMIT ...)` 改走 `DialectRegistry.current().paginate(...)`，为多方言铺路
- 包重组完全由 `scripts/restructure.py` + `scripts/rename_4_0.py` 机械完成（保留 git mv 历史，`git log --follow` 可追溯）

### Migration

迁移路径见 [MIGRATION-4.0.md](MIGRATION-4.0.md)。最简方式：
- **IntelliJ**：`Ctrl+Alt+O`（Optimize Imports）一键搞定
- **CLI**：运行 `migrate-to-4.0.sh` / `.ps1`（脚本内置完整路径表）

### 后续路线
- **4.0.1**：PostgreSQL / DM 方言内置实现 + 集成测试
- **4.1**：WINDOW 函数 / CTE / UNION / JSON 操作符；MybatisQueryableStream codegen


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

[Unreleased]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.0.0...HEAD
[4.0.0.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v3.5.16.0...v4.0.0.0
[3.5.16.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v3.5.9.0...v3.5.16.0
[3.5.9.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/releases/tag/v3.5.9.0
