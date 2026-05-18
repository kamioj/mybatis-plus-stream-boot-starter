# Changelog

本项目所有发版记录。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## 版本号约定

`{MyBatis-Plus 版本}.{补丁号}`，例如 `3.5.16.0` 表示对齐 MyBatis-Plus 3.5.16 的第 0 个补丁。
升级 MyBatis-Plus 主线版本即新开 minor 段，本项目自身修复在 patch 段递增。

---

## [Unreleased]

（暂无）

## [4.0.3.0] - 2026-05-19

### Added

**DM 三种批量写入完整实现**（通过 `MERGE INTO` + `@InsertProvider`）：

| API | DM 8 生成的 SQL（Oracle 兼容模式）|
|---|---|
| `saveDuplicate` | `MERGE INTO t USING (SELECT ... FROM DUAL UNION ALL ...) src ON (t.pk = src.pk) WHEN MATCHED THEN UPDATE SET col = ... WHEN NOT MATCHED THEN INSERT ...` |
| `saveIgnore` | `MERGE INTO ... WHEN NOT MATCHED THEN INSERT ...`（无 WHEN MATCHED → 已存在不动）|
| `saveReplace` | `MERGE INTO ... WHEN MATCHED THEN UPDATE SET 全列 = src.全列 WHEN NOT MATCHED THEN INSERT ...` |

DM `saveDuplicate / saveReplace` 同 PG，要求实体声明 `@TableId`（MERGE INTO ON 子句必需）。

### Fixed

**`columns` 列名硬编码反引号 bug**（4.0.2 隐藏问题）：

`MybatisExecutableStream` 之前用 `` `column` `` 硬拼列名（MySQL 风格）。改为走 `DialectRegistry.current().quoteIdentifier(name)` 后：

- MySQL: `` `column` ``（不变）
- PostgreSQL: `"column"` （正确）
- DM: `"column"` （正确）

修复 8 处 `StringPool.BACKTICK` 调用。这个 bug 在 4.0.2 PG `saveDuplicate / saveIgnore / saveReplace` 真实跑时可能触发；4.0.3 之前未 surface 是因为没集成测试。

### Added (SPI)

- `SqlDialect.useMergeInto(WriteMode)` 默认 false；DM 在 DUPLICATE/IGNORE/REPLACE 返回 true
- `SqlDialect.buildMergeIntoScript(columns, wrapper)` 默认 throw；DM 实现完整 `<script>...</script>` 字符串
- `MergeIntoSqlProvider` 类：`@InsertProvider` 入口，把请求委托给当前 dialect
- `StreamBaseMapper.mergeInto(columns, values, wrapper)` 新增 mapper 方法
- `MybatisExecutableStream.dispatchBatchWrite(...)` 私有 helper：根据 dialect 选 INSERT vs MERGE INTO 路径

### 三方言完整能力（4.0.3 起）

| API | MySQL | PG | DM 8 |
|---|---|---|---|
| `saveBatchWithoutId` | ✅ | ✅ | ✅ |
| `saveDuplicate` | ✅ | ✅ | ✅ |
| `saveIgnore` | ✅ | ✅ | ✅ |
| `saveReplace` | ✅ | ✅ | ✅ |
| `forUpdate` 系列 | ✅ | ✅ NOWAIT/SKIP | ✅ NOWAIT/WAIT n |
| 所有查询路径 | ✅ | ✅ | ✅ |

## [4.0.2.0] - 2026-05-19

### Added

**批量写入路径完整接管 dialect**（MySQL + PostgreSQL）：

- `WriteMode` 枚举（INSERT / DUPLICATE / IGNORE / REPLACE）
- `SqlDialect.insertPrefix(WriteMode)` + `SqlDialect.conflictClause(WriteMode, setters, pk, allColumns)`
- `ExecutableQueryWrapper` 加 `writeMode` 字段 + `getSqlInsertPrefix()` / `getSqlConflictClause()` 方言敏感 getter
- `StreamBaseMapper` 三个 INSERT 方法统一使用 `${ew.sqlInsertPrefix}` + `${ew.sqlConflictClause}` 占位

**PostgreSQL 三种写入完整可用**：

| API | PG 生成的 SQL |
|---|---|
| `saveDuplicate` | `INSERT INTO ... ON CONFLICT (pk) DO UPDATE SET col = ...` |
| `saveIgnore` | `INSERT INTO ... ON CONFLICT DO NOTHING` |
| `saveReplace` | `INSERT INTO ... ON CONFLICT (pk) DO UPDATE SET 全列=EXCLUDED.全列` |

`saveDuplicate` / `saveReplace` 在 PG 上**要求实体声明 `@TableId` 主键**（ON CONFLICT 必需），未声明 fail-fast 抛 `IllegalStateException`。

### Changed

- 删除 `SqlDialect.supportsUpsert/supportsInsertIgnore/supportsInsertReplace` 三个 hint 方法 —— 改由"调用时通过 dialect 渲染"决定能力
- `ExecutableQueryWrapper.getSqlDuplicateSet()` 标记 `@Deprecated(forRemoval = true, since = "4.0.2")`；内部委托 `getSqlConflictClause()`，4.1 删除

### Known limitations

- **达梦 DM 三种写入路径暂未实现**：`DamengDialect.insertPrefix(DUPLICATE/IGNORE/REPLACE)` 与 `conflictClause(DUPLICATE/IGNORE/REPLACE)` 抛 `UnsupportedOperationException` 并给出清晰错误信息引导用户。规划 4.0.3 通过 `MERGE INTO` + `@InsertProvider` 完整实现（DM 语句结构与 INSERT 完全不同，不能套统一模板）
- DM 用户 4.0.2 仅可用查询路径 + `saveBatchWithoutId`（纯 INSERT）

## [4.0.1.0] - 2026-05-18

### Added

- **`PostgreSqlDialect`**（PG 17 LTS 验证）：分页 `LIMIT n OFFSET m` / 字符串 `||` 拼接 / `STRING_AGG` 聚合 / 双引号标识符 / `FOR UPDATE NOWAIT|SKIP LOCKED` / 类型映射（`SIGNED→BIGINT` / `DATETIME→TIMESTAMP` / `BINARY→BYTEA`）
- **`DamengDialect`**（DM 8 验证，Oracle 兼容模式优先）：分页 `LIMIT n OFFSET m` / 字符串 `||` 拼接 / `STRING_AGG` 聚合 / 双引号标识符 / `FOR UPDATE NOWAIT|WAIT n` / 类型映射（`CHAR→VARCHAR` / `BINARY→BLOB`）
- 在 `META-INF/services/com.baomidou.mybatisplus.extension.dialect.SqlDialect` 注册三个内置方言，启动时通过 ServiceLoader 自动加载。用户切换方式：
  ```java
  DialectRegistry.use(DbType.POSTGRESQL);  // 或 DbType.DAMENG
  ```

### Notes

- PG 与 DM 都**不支持** `REPLACE INTO`（`supportsInsertReplace() == false`）；调用 `saveReplace` 会 fail-fast
- DM **不支持** `INSERT IGNORE`（`supportsInsertIgnore() == false`）；调用 `saveIgnore` 会 fail-fast
- DM 是唯一支持 `FOR UPDATE WAIT n` 的内置方言（来自 Oracle 兼容性）
- 三方言均不影响 MySQL 默认行为，4.0.0.0 → 4.0.1.0 是 **0 breaking 增量升级**

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

[Unreleased]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.3.0...HEAD
[4.0.3.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.2.0...v4.0.3.0
[4.0.2.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.1.0...v4.0.2.0
[4.0.1.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.0.0...v4.0.1.0
[4.0.0.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v3.5.16.0...v4.0.0.0
[3.5.16.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v3.5.9.0...v3.5.16.0
[3.5.9.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/releases/tag/v3.5.9.0
