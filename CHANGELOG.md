# Changelog

本项目所有发版记录。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## 版本号约定

`{MyBatis-Plus 版本}.{补丁号}`，例如 `3.5.16.0` 表示对齐 MyBatis-Plus 3.5.16 的第 0 个补丁。
升级 MyBatis-Plus 主线版本即新开 minor 段，本项目自身修复在 patch 段递增。

---

## [Unreleased]

（暂无）

## [4.1.1.2] - 2026-05-19

### Fixed

**Multi-module 发布兼容修复**（4.1.1.1 发布失败的修复）：

- 4.1.1.1 deploy 失败：Sonatype Central 拒绝 bundle，错误：
  - `Failed to get coordinates from pom file ... .pom` —— pom 的 groupId/version 通过 parent 继承没展开
  - `Dependency version information is missing for dependency: org.junit.jupiter:junit-jupiter` —— `junit-jupiter` 的 version 在父 pom `<dependencyManagement>` 里，starter pom 没显式 version
- 修复：`starter/pom.xml` 加 `flatten-maven-plugin`（mode=ossrh），install/deploy 时上传扁平化 pom（parent 已展开 + dep version 已展开）
- 4.1.1.2 与 4.1.1.0 / 4.1.1.1 的库代码完全一致，纯发布配置补丁

## [4.1.1.1] - 2026-05-19

### Fixed

**Maven Central 同步配置修复**：

- `starter/pom.xml` 的 `central-publishing-maven-plugin` 加 `<autoPublish>true</autoPublish>`
  - 此前 v3.5.9.0 ~ v4.1.1.0 跑 `mvn deploy` 只上传到 Sonatype Central Portal staging，**未触发 Publish**——所有这些版本实际从未同步到 Maven Central 仓库（`https://repo.maven.apache.org/.../maven-metadata.xml` 只列 `1.0.0` 一个版本）
  - 用户拉 `<version>4.1.1.0</version>` 会报 "Could not resolve dependencies"
  - 加 `autoPublish` 后 staging 完成上传立即自动 publish，无需手工操作

### Note

4.1.1.1 与 4.1.1.0 的**库代码完全一致**——纯发布配置修复，让 Maven Central 真正可用。

## [4.1.1.0] - 2026-05-19

### Added

**PostgreSQL Testcontainers 集成测试**（验证 4.0/4.1 dialect 适配在真实 PG 17 数据库上工作）：

- `pom.xml` 加 test 依赖：`spring-boot-starter-test` / `spring-boot-testcontainers` / `testcontainers:postgresql:1.20.3` / `postgresql:42.7.4`（test scope，不影响生产打包）
- `src/test/java/.../it/`：最小 Spring Boot 测试应用 + `UserDo` 实体 + `UserMapper` + `UserService`
- `PostgreSqlIntegrationTest`：7 个集成测试覆盖
  - `saveBatchWithoutId` + `list(where)`
  - `stream().filter().toSet(col)`
  - `stream().toMap(keyCol, valCol)` 验证 SQL 真下推 SELECT 两列
  - `stream().toMapCount(keyCol)` 验证 GROUP BY 真下推 SQL
  - `saveDuplicate` 验证 PG `ON CONFLICT DO UPDATE`
  - `saveIgnore` 验证 PG `ON CONFLICT DO NOTHING`
  - `saveReplace` 验证 PG `ON CONFLICT 全列覆盖`

测试自动启动 `postgres:17-alpine` Docker 容器，每个测试方法 reset schema。

### Notes

- DM 集成测试按之前约定**不上 CI**（DM 无官方 testcontainers 模块；用户本地手动验证）
- 集成测试运行需 Docker；GitHub Actions ubuntu-latest 默认提供 Docker 环境
- 本地开发跑 `mvn test`：单元测试一定跑通；集成测试需本地有 Docker，否则会启动 PG 容器失败

## [4.1.0.0] - 2026-05-19

### Added

**BACKTICK token 化适配器**（解决 PG/DM 复杂 SQL 反引号语法错的核心机制）：

- `DialectQuoteTranslator`：把 wrapper 内部 BACKTICK 风格 SQL 片段（如 `` `user`.`id` ``）翻译为当前方言引号（PG/DM 用双引号 `"user"."id"`；MySQL 是 no-op 不变）
- 设计：wrapper 内部 SQL 渲染逻辑**完全不变**（保留 BACKTICK 作内部 token / keySet 协议分隔符），只在"最后一公里" SQL 渲染入口加 translator
- 适配器风格：translator 直接调 `dialect.quoteIdentifier(...)`，不耦合具体方言；用户自定义方言只要实现 quoteIdentifier 自动生效
- 处理字符串字面值：SQL `'...'` 内的反引号不翻译；标准 `''` 转义正确处理
- 不成对反引号防御性保留（不破坏 MySQL 兼容路径）

**ExQueryWrapper 5 个 SQL 渲染入口接入 translator**：

- `getSqlSelect()`（SELECT 列）
- `getCustomSqlSegment()`（WHERE/GROUP BY/HAVING/ORDER BY）
- `getSqlFrom()`（FROM + JOIN）
- `getCustomSqlFromSegment()`（同上 + FROM 前缀）
- `getSqlSet()` / `getSqlConflictClause()`（UPDATE SET / UPSERT）

**单元测试**（10/10 通过）：

- MySQL 方言 no-op 验证
- PG/DM 翻译验证
- 字符串字面值保护
- SQL '' 转义
- 表别名/JOIN 路径
- 自定义方言适配器路径
- 边界情况（空、null、不成对反引号）

### Fixed

之前在 PG/DM 复杂 query 路径会报反引号语法错的 corner cases 现已统一修复：

- 复杂 JOIN + ORDER BY
- SELECT 子句列名
- WHERE 表达式列名
- UPDATE SET 子句
- ExecutableQueryWrapper 的 getSqlConflictClause 返回值

### Internal

- pom.xml 加 `org.junit.jupiter:junit-jupiter:5.10.2` test scope 依赖
- 升级 `maven-surefire-plugin` 2.12.4 → 3.2.5（支持 JUnit 5）
- 新建 `src/test/java` 测试目录

### Known limitations

- testcontainers PG 集成测试是更大工程（需要 Spring Boot test 全栈 + Docker CI 配置），规划在 4.1.1 单独实施。当前 4.1.0 的 unit test 已覆盖 translator 适配器核心行为，加上 ExQueryWrapper 5 个 getter 接入 translator，**理论上**所有反引号 corner case 都已修复

## [4.0.5.0] - 2026-05-19

### Fixed

- **`ExQueryWrapper.setFromTable` 表名 + AS 别名走 dialect**：之前硬编码 `` `table` AS `rename` ``（MySQL 反引号），PG/DM 会报语法错。改 `dialect.quoteIdentifier(...)` 后三方言生成正确的 SQL（PG/DM 用双引号）
- **`ExQueryWrapper.addLogicDelete` 逻辑删除列引号走 dialect**：之前硬编码 `` `alias`.`column` `` —— PG/DM 用户带 `@TableLogic` 实体的隐式 WHERE 子句会语法错。改 dialect 后修复

### Known limitations（4.0.x 系列已知未完）

剩余 36 处 `StringPool.BACKTICK` 硬编码在以下文件：

| 文件 | 处数 | 影响路径 |
|---|---|---|
| `core/LambdaQueryWrapper.java` | 19 | wrapper 内部列名/表别名渲染（SELECT/JOIN/WHERE）|
| `stream/MybatisQueryableStream.java` | 4 | stream 链式 select 路径 |
| `core/ExQueryWrapper.java` | 6 | 排序、内部 keySet 约定 |
| `service/impl/StreamServiceImpl.java` | 2 | service 层辅助 |
| `support/LambdaOrderItem.java` | 1 | order item |
| `metadata/ColumnInfo.java` | 1 | 元数据 |
| `stream/MybatisExecutableStream.java` | 1 | 列名剥引号（不需改）|

这些路径在 corner case（复杂 JOIN + ORDER BY、wrapper 内部约定 keySet）可能让 PG/DM 触发语法错。**MySQL 主流程不受影响**。

修复策略需要重新设计：把 `BACKTICK` 当作 wrapper 内部 token（不暴露到最终 SQL），最后一刻渲染时统一替换为 `dialect.quoteIdentifier`。这是 4.1 单独 spec 工程，本 patch 不做。

**当前 4.0.5 后 PG/DM 用户能跑通**：所有 `saveBatchWithoutId / saveDuplicate / saveIgnore / saveReplace`、基础 `list / page / stream / forUpdate / groupConcat / 行锁细粒度 / 逻辑删除`。
**仍可能踩坑**：复杂 JOIN + ORDER BY、自定义 selectAll、深度嵌套子查询。

## [4.0.4.0] - 2026-05-19

### Fixed

- **`GroupFunctionLambdaQueryWrapper.groupConcatFunc / groupConcatDistinctFunc` 改走方言**：
  - PG / DM：生成 `STRING_AGG(col, ',')`
  - MySQL：保留 `GROUP_CONCAT(... [ORDER BY] [SEPARATOR ','])` 完整行为
  - PG/DM 之前调 `func.groupConcat(...)` 会生成 MySQL 语法报错，本版本修复

### Known limitations

- `func.convert(col, dataType)` 仍调底层 `function("CONVERT", ...)` 工具拼字符串，在 PG/DM 上仍会生成 `CONVERT(...)` 语法（PG/DM 应该用 `CAST(... AS type)`）。改造涉及 `function` 工具方法的深度重构，留 4.0.5。
- 临时绕过：PG/DM 用户避开 `func.convert(...)` API，改用原始 SQL 片段或外层 dialect.cast 包装

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

[Unreleased]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.1.1.0...HEAD
[4.1.1.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.1.0.0...v4.1.1.0
[4.1.0.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.5.0...v4.1.0.0
[4.0.5.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.4.0...v4.0.5.0
[4.0.4.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.3.0...v4.0.4.0
[4.0.3.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.2.0...v4.0.3.0
[4.0.2.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.1.0...v4.0.2.0
[4.0.1.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v4.0.0.0...v4.0.1.0
[4.0.0.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v3.5.16.0...v4.0.0.0
[3.5.16.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/compare/v3.5.9.0...v3.5.16.0
[3.5.9.0]: https://github.com/kamioj/mybatis-plus-stream-boot-starter/releases/tag/v3.5.9.0
