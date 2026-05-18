# Proposal：4.0 全面审查 + 多方言扩展

## 用户三点强约束（贯穿全文）

1. **干净**：本库自身不引入第三方服务依赖，方言/扩展点全部以 SPI 形式让用户实现
2. **SQL 标准结构可见**：API 不抹除 SQL，让开发者一眼对应到底层 SQL
3. **支持达梦 (DM 8) + PostgreSQL (17 LTS)**

---

## Why

P0 包重组（PR #3）已经把 4.0 当成 breaking-version 改包结构；这是改架构的**唯一窗口**。基于 5 路调研结果，4.0 还应一并解决：

- **方言耦合**：10 处 MySQL 硬编码（`ON DUPLICATE KEY UPDATE` / `LIMIT a,b` / `GROUP_CONCAT` / `DATE_FORMAT` / `CONVERT(... CHARSET)` 等）写死在 wrapper / mapper / service 中。要让 DM、PG 能用，必须抽象方言层
- **命名误导**：`MysqlBaseMapper` / `IMysqlServiceBase` / `MysqlServiceBaseImpl` / `MysqlDataType` 全带 `Mysql` 前缀，违反"通用 DB 工具"产品定位。这是 4.0 一并修掉的最后机会
- **反射热路径**：`ReflectUtils.getLambda()` 每次 SFunction 解析都反射 `writeReplace`；`clazz.newInstance()` 在 `getSubSqlSegment` 每次新建；`StringUtils.regexMatcher` 每次 `Pattern.compile`——这些都是高频热点
- **JDK 9+ 已废弃 API**：`Class#newInstance()` / `Constructor#newInstance()` 多处仍在用
- **SQL 表达完整性**：EXISTS / NOT EXISTS / WINDOW / CTE / UNION / JSON 操作符 / 行锁细粒度（NOWAIT / SKIP LOCKED）等 SQL 标准结构暂缺；其中 EXISTS 与行锁细粒度最实用

---

## 整体节奏：4.0 → 4.0.1 → 4.1

**避免一锅炖**——把改动按"是否 breaking + 用户感知度"分三档：

| 阶段 | 内容 | breaking? | 工作量 |
|---|---|---|---|
| **4.0.0.0** ← 主版本 | 包重组（已完成）+ 类名去 Mysql 前缀 + 方言 SPI 接口 + MySQL 实现 + 反射热点修复 + 废弃 API 清理 + EXISTS/行锁细粒度补齐 | **是**（一次性把 breaking 改完）| 35-45h |
| **4.0.1** 起 patch | DM 方言实现 + PG 方言实现 + 各自集成测试 | 否（仅新增）| 12-16h |
| **4.1.0.0** | WINDOW / CTE / UNION / JSON 操作符（SQL 表达力增强）+ MybatisQueryableStream codegen | 否（仅新增 API）| 20-30h |

**理由**：4.0 已经是 breaking，能塞进去的 breaking 都塞进去；非 breaking 的功能放后续 minor。这样 4.0 之后 4.x 系列**对用户都是非破坏性升级**。

---

## What Changes

### P1 命名去 Mysql 前缀（4.0 breaking）

| 旧名 | 新名 | 影响 |
|---|---|---|
| `mapper.MysqlBaseMapper` | `mapper.StreamBaseMapper` | 用户继承点 |
| `service.IMysqlServiceBase` | `service.IStreamService` | 用户继承点 |
| `service.impl.MysqlServiceBaseImpl` | `service.impl.StreamServiceImpl` | 用户继承点 |
| `metadata.MysqlDataType` | `metadata.SqlDataType`（枚举改为通用类型）| 内部 |

**理由**：包名已经叫 `mybatis-plus-stream`，类名再带 `Mysql` 是矛盾的。`Stream` 前缀也呼应包/项目名。

> 提供 deprecated 别名类（继承新名）保留 4.0 一个 patch 周期，4.1 删除。

### P2 方言 SPI（4.0 接口 + MySQL 实现；4.0.1 DM/PG）

#### 设计

```
extension/dialect/
├── SqlDialect.java          (interface) ← SPI 入口
├── DbType.java              (enum)      ← MYSQL / POSTGRESQL / DAMENG / CUSTOM
├── DialectRegistry.java     (静态注册表 + ServiceLoader 自动加载)
└── impl/
    ├── MySqlDialect.java
    ├── PostgreSqlDialect.java   ← 4.0.1
    └── DamengDialect.java        ← 4.0.1
```

#### `SqlDialect` 接口（最小集）

```java
public interface SqlDialect {
    DbType getDbType();

    /** 分页：LIMIT 语法因方言而异 */
    String paginate(String sql, long offset, long limit);

    /** UPSERT：MySQL = ON DUPLICATE KEY UPDATE / PG = ON CONFLICT / DM = MERGE INTO */
    String upsertInsert(String table, String[] cols, Object[][] values, String onDuplicateSetClause);

    /** INSERT IGNORE / REPLACE：各方言写法不同 */
    String insertIgnore(String table, String[] cols, Object[][] values);
    String insertReplace(String table, String[] cols, Object[][] values);

    /** 字符串拼接：MySQL CONCAT / PG/DM 用 || */
    String concat(String... parts);

    /** 聚合字符串：MySQL GROUP_CONCAT / PG/DM STRING_AGG */
    String groupConcat(String column, String separator);

    /** CAST 函数 + 数据类型名映射 */
    String cast(String expr, SqlDataType type);
    String dataTypeName(SqlDataType type);  // SIGNED → SIGNED/INTEGER/...

    /** 行锁细粒度：FOR UPDATE [NOWAIT | SKIP LOCKED | WAIT n] */
    String forUpdate(LockMode mode);

    /** 标识符引用：MySQL ` / PG/DM " */
    String quoteIdentifier(String name);
}
```

#### 注册与切换

- 默认 `MySqlDialect`（向后兼容 3.5.x 用户行为）
- 用户在 `application.yml` 配 `mybatis-plus-stream.dialect: postgresql` 切换
- 用户实现自定义方言 → `META-INF/services/com.baomidou.mybatisplus.extension.dialect.SqlDialect` 注册 → 启动自动加载（ServiceLoader）

#### 集成位置

| 当前硬编码 | 替换为 |
|---|---|
| `MybatisQueryableStream.last("LIMIT ...")` | `dialect.paginate(...)` |
| `ExecutableQueryWrapper.sqlDuplicateSet` 生成 `ON DUPLICATE KEY UPDATE` | `dialect.upsertInsert(...)` |
| `StreamBaseMapper.insertIgnore` 注解 SQL | 改为运行时 `${ew.sqlInsertIgnore}` 由 dialect 生成 |
| `AbstractFunctionLambdaQueryWrapper` 中的 `CONCAT` / `GROUP_CONCAT` / `CONVERT` | `dialect.concat(...)` / `dialect.groupConcat(...)` / `dialect.cast(...)` |

### P3 反射热点优化（4.0 内部，无 API 变化）

| 改 | 怎么改 | 收益 |
|---|---|---|
| `ReflectUtils.getLambda` | `ClassValue<LambdaMeta>` 缓存：lambda class → (className, implMethodName, propertyName)。首次解析后 0 反射 | 高频热路径，预计 5-10x 加速 |
| 正则 `\\(L(.*);\\)` 提取类名 | 字符串切片（`indexOf("(L")+2` / `indexOf(";)")`）| 省正则编译；与上面 ClassValue 配合一次性缓存结果 |
| `clazz.newInstance()` × 3 处 | `getDeclaredConstructor().newInstance()`（去 deprecated）；构造器用 `ClassValue<Constructor<?>>` 缓存 | 去废弃 + 加速 |
| `StringUtils.regexMatcher` | `Pattern.compile` 提到 static final | 复用 Pattern |
| `getSubSqlSegment(predicate, fClazz)` | 子类内加便捷重载 `getSubSqlSegment(predicate)` 自动用 `this.fClazz` | 调用方更干净 |

### P4 实现质量清扫（4.0 内部）

- 7 处空 catch 块 → 改为 `log.debug` 或显式 throw
- 98 处 `@SuppressWarnings("unchecked")` 按需保留（不强行清理）
- `JDK 9+ deprecated` Class 反射 API 全部升级到 `getDeclaredConstructor()`
- 用 JDK 17 `pattern matching for instanceof` 简化 `ReflectUtils` 里 `if (type instanceof X) { X x = (X) type; ... }`
- `MybatisUtil.getTableInfo` 加 `ClassValue<TableInfo>` 缓存

### P6（追加于 2026-05-18）：SQL-aware Terminal Operations

**动机**：基于 `ceremonyproapp` 50+ 处真实代码证据，用户**主动绕过** `.listJoin/.listGroup/.pageJoin` 高阶 API，改用 `service.list(...).stream().collect(Collectors.toMap/groupingBy/...)` 三段式。问题：全量加载到内存才能进入 JDK Stream，浪费数据传输和 JVM 堆。

**真痛点**：用户已习惯 JDK Collector API 心智，但希望 starter 在收集前**下推 SQL**。

**新增 5 个 terminal 方法**（添加到 `MybatisQueryableStream` 基类，所有 Stream1..5 自动继承）：

```java
/** SQL: SELECT col FROM ... 应用层收 Set */
<K> Set<K> toSet(SFunction<T, K> col);

/** SQL: SELECT keyCol, valCol FROM ... 应用层收 Map */
<K, V> Map<K, V> toMap(SFunction<T, K> keyCol, SFunction<T, V> valCol);

/** Key 冲突时用 merger 合并 */
<K, V> Map<K, V> toMap(SFunction<T, K> keyCol, SFunction<T, V> valCol, BinaryOperator<V> merger);

/** SQL: SELECT * 应用层按 keyCol 分组（保留所有列）；如只要 key+count 用 toMapCount */
<K> Map<K, List<T>> groupingBy(SFunction<T, K> keyCol);

/** SQL: SELECT keyCol, COUNT(*) GROUP BY keyCol —— GROUP BY 真下推到数据库 */
<K> Map<K, Long> toMapCount(SFunction<T, K> keyCol);
```

**设计要点**：

- **命名与 JDK 接近**（`toMap/toSet/groupingBy`），用户已会
- **接 `SFunction<T, ?>` 而非 lambda** —— starter 解析为列名下推 SQL
- **"SQL 一目了然" 兑现**：方法名 → SQL 关键字直接映射
- **零 breaking**：纯增量；旧路径继续工作
- 多元 key 变体（`toMap` with 复合 key、`groupingBy` 多列）留 4.1，按真实需求迭代

**工作量估算**：6-8 h 实施 + 2h 文档 = **~10h**

---

### P5 SQL 表达完整性补齐（4.0 必做的 2 项；其他留 4.1）

| 必做（4.0）| 推迟（4.1）|
|---|---|
| **EXISTS / NOT EXISTS**：`.exists(subquery)` / `.notExists(subquery)` 出现在 WHERE 上下文 | WINDOW 函数 |
| **行锁细粒度**：`.forUpdateNoWait()` / `.forUpdateSkipLocked()` / `.forUpdateWait(n)` | CTE / WITH RECURSIVE |
| | UNION / UNION ALL |
| | JSON 操作符 |

理由：EXISTS 和行锁细粒度是日常业务高频；WINDOW/CTE/UNION 是高级特性，加在 4.1 minor 增量发布不会破坏 4.0 用户。

---

## 类与文件清单（实施时核对）

### 4.0 新增
- `extension/dialect/SqlDialect.java`
- `extension/dialect/DbType.java`（含 MYSQL/POSTGRESQL/DAMENG/CUSTOM）
- `extension/dialect/DialectRegistry.java`
- `extension/dialect/LockMode.java`（行锁枚举）
- `extension/dialect/impl/MySqlDialect.java`
- `extension/value/LambdaMeta.java`（缓存 record）
- `extension/mapper/StreamBaseMapper.java`（替代 MysqlBaseMapper）
- `extension/service/IStreamService.java`（替代 IMysqlServiceBase）
- `extension/service/impl/StreamServiceImpl.java`（替代 MysqlServiceBaseImpl）
- `metadata/SqlDataType.java`（替代 MysqlDataType）

### 4.0 改造（含 deprecated 别名）
- `mapper/MysqlBaseMapper` → 改为 `extends StreamBaseMapper` 的空壳 + `@Deprecated`
- `service/IMysqlServiceBase` → 同上
- `service/impl/MysqlServiceBaseImpl` → 同上
- `toolkit/ReflectUtils`、`toolkit/MybatisUtil` → 加 ClassValue 缓存
- 所有 wrapper 中 SQL 拼装 → 调 `DialectRegistry.current()`

### 4.0.1 新增
- `extension/dialect/impl/PostgreSqlDialect.java`
- `extension/dialect/impl/DamengDialect.java`
- 集成测试模块（PG 用 testcontainers；DM 用社区 docker 镜像 + 可选 CI）

### 4.1 新增（非破坏增量）
- `wrapper/WindowLambdaQueryWrapper.java`（窗口函数）
- `wrapper/CteLambdaQueryWrapper.java`（CTE / WITH RECURSIVE）
- `wrapper/UnionLambdaQueryWrapper.java`
- `wrapper/JsonLambdaQueryWrapper.java`
- `stream/` 内 `MybatisQueryableStream1..5` 用 gmaven-plus 模板生成（如 P1 调研所示）

---

## Impact / Risks

| 风险 | 缓解 |
|---|---|
| **类名前缀变更 breaking 影响所有用户** | 提供 deprecated 别名类保留一个 patch 周期；`MIGRATION-4.0.md` 写明 sed 一键替换；IDE Optimize Imports + Rename Refactor 也能搞定 |
| **方言抽象引入"用户跑通 MySQL 自然 OK，PG/DM 还要等 4.0.1"** | 4.0 默认 MySQL 行为完全不变；4.0.1 才补 PG/DM 实现 |
| **DM 测试基础设施薄弱**（testcontainers 无官方模块）| 4.0.1 选 toyangdon/docker-dm 社区镜像；CI 用 GitHub Actions services + 跳过模式（DM 测试默认 skip，本地或 nightly 跑）|
| **ClassValue 缓存 + ServiceLoader 引入复杂度** | 单元测试覆盖；先小流量验证缓存命中率 |
| **`Mysql*` 别名空壳维护负担** | 4.0 加 `@Deprecated(forRemoval = true, since = "4.0")`；4.1 删除（patch 周期 ≥3 个月）|
| **SQL 表达力补齐是否必要** | EXISTS + 行锁细粒度是高频；WINDOW/CTE 放 4.1。不做 codegen，保持手写灵活 |

### 回滚预案

- 4.0 发版后 24h 内出问题：Maven Central 撤稿 + 回滚 main + GitHub Release 标 deprecated
- 24h 后：发 4.0.1 修复关键 bug；如方言架构本身有错则发 4.1 重设计（不撤回 4.0，4.0 用户继续用 MySQL 默认实现）

---

## 工作量估算

| 阶段 | 任务 | 工作量 |
|---|---|---|
| **4.0** | 类名去前缀 + deprecated 别名 | 2h |
| | 方言 SPI 接口 + MySqlDialect 实现 | 8h |
| | 替换 10 处硬编码 → 调 dialect | 6h |
| | 反射热点（ClassValue 缓存 + 去 deprecated API）| 4h |
| | EXISTS + 行锁细粒度 | 3h |
| | 实现质量清扫（空 catch、JDK 17 特性）| 3h |
| | CHANGELOG + MIGRATION-4.0.md 更新 + 写 dialect 章节 | 2h |
| | mvn compile + 测试 + PR 准备 | 4h |
| **4.0 小计** | | **~32h** |
| **4.0.1** | PostgreSqlDialect 实现 + testcontainers 集成测试 | 6h |
| | DamengDialect 实现 + docker 测试方案 | 8h |
| **4.0.1 小计** | | **~14h** |
| **4.1**（可选）| WINDOW / CTE / UNION / JSON | 12h |
| | MybatisQueryableStream codegen | 6h |
| | 其他 SQL 完整性补齐 | 4h |
| **4.1 小计** | | **~22h** |

---

## 验收标准（实施时回头对照）

### 4.0
- [ ] 所有 `Mysql*` 类已重命名，旧名作为 `@Deprecated` 空壳保留
- [ ] `SqlDialect` SPI 跑通：默认 `MySqlDialect`；通过 `application.yml` 切换；通过 ServiceLoader 加载用户自定义
- [ ] 10 处硬编码 SQL 全部走 dialect
- [ ] `ReflectUtils.getLambda` ClassValue 缓存命中率 > 99%（写一个 benchmark）
- [ ] 0 处 `clazz.newInstance()`（grep 验证）
- [ ] EXISTS / NOT EXISTS / 行锁细粒度 API 工作
- [ ] CI 绿（包括新增 dialect 单元测试）
- [ ] `mvn install` 通过，`使用文档.md` + README + docs 站全部同步

### 4.0.1
- [ ] PG / DM dialect 通过 testcontainers / docker 集成测试
- [ ] 至少一个 DM CI job（即使 skip-by-default）
- [ ] `MIGRATION-4.0.md` 补 dialect 切换示例
