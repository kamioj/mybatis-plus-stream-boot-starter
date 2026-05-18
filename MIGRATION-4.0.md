# 4.0.0.0 迁移指南

3.5.16.x → 4.0.0.0 的 **三类 breaking change**：

1. **包结构重组**：所有类按职责拆到 6 个子包
2. **类名去 `Mysql` 前缀**：`MysqlBaseMapper` → `StreamBaseMapper` 等
3. **`getSubSqlSegment` throws 加宽**：继承 wrapper 写自定义子类的用户 catch 块需要改

API 行为、方法签名、SQL 渲染、版本号约定（`{MP-ver}.{patch}`）**完全不变**。
**`MysqlBaseMapper` / `IMysqlServiceBase` / `MysqlServiceBaseImpl` 旧名保留 `@Deprecated` 空壳到 4.1，4.0 可平滑过渡**。

## ⚠️ 删除（21 个 dead 类，4.0 一次性清理）

下列类被认定为预防性 over-engineering（生产项目 0 引用），4.0 移除：

- `bo.functional.Function3` ... `Function15`（13 个）
- `bo.functional.Consumer3` ... `Consumer10`（8 个）
- 整个 `bo.functional` 子包

`BiMapKey` / `MapKey3..5` 保留（生产真实使用）。如你的代码 import 了上述被删类，编译会失败——99% 概率你没用到（这些类从未真正接通到 starter API）。

## 新增能力概览（升级即用）

- **方言 SPI**：`com.baomidou.mybatisplus.extension.dialect.SqlDialect` —— 可注册自定义方言；本库自身不引入 JDBC 驱动。详见 [DIALECT.md](#dialect-extension) 章节
- **EXISTS / NOT EXISTS**：WHERE 上下文新增 `.exists(subSql)` / `.notExists(subSql)`
- **行锁细粒度**：`.forUpdateNoWait()` / `.forUpdateSkipLocked()` / `.forUpdateWait(int seconds)`
- **反射热路径优化**：SFunction 解析 ClassValue 缓存（&gt; 99% 命中率），`MybatisUtil.getTableInfo` 无锁化
- **SQL-aware Terminal Operations**：9 个新方法替代 "全量加载 + JDK Collector" 模式，见下方 [Terminal Operations 章节](#terminal-operations)

---

## 推荐迁移方式：IDE 自动重导入

**IntelliJ IDEA**：升级依赖到 `4.0.0.0` 后，对项目执行 `Ctrl+Alt+O`（Optimize Imports），所有 import 自动解析到新路径。一分钟搞定。

**Eclipse**：`Source → Organize Imports`（`Ctrl+Shift+O`）效果相同。

**VS Code + Java 插件**：右键文件 → "Organize Imports"。

如果你的项目用到的本库类不多（通常 < 10 个），IDE 重导就足够，无需查表。

---

## 备选：CLI 批量替换

如果你的项目几百处 import 散落、不想手点 IDE，跑下方 `migrate-to-4.0.sh`（PowerShell 版见底部）。脚本会扫你项目下所有 `.java` 文件，把旧 import 路径机械替换为新路径。

### 类名重命名总表（4.0 唯一新增项）

| 旧名 | 新名 | 兼容策略 |
|---|---|---|
| `mapper.MysqlBaseMapper` | `mapper.StreamBaseMapper` | `@Deprecated` 别名保留到 4.1 |
| `service.IMysqlServiceBase` | `service.IStreamService` | `@Deprecated` 别名保留到 4.1 |
| `service.impl.MysqlServiceBaseImpl` | `service.impl.StreamServiceImpl` | `@Deprecated` 别名保留到 4.1 |
| `metadata.MysqlDataType` | `metadata.SqlDataType` | **无别名**（enum 无法 extends），4.0 直接断；用 sed/IDE Rename 替换 |

### 路径映射总表

| 类名 | 旧 fqn | 新 fqn |
|---|---|---|
| **wrapper/** (26 个) | | |
| AbstractCaseLambdaQueryWrapper | `...extension.AbstractCaseLambdaQueryWrapper` | `...extension.wrapper.AbstractCaseLambdaQueryWrapper` |
| AbstractFunctionLambdaQueryWrapper | `...extension.AbstractFunctionLambdaQueryWrapper` | `...extension.wrapper.AbstractFunctionLambdaQueryWrapper` |
| AbstractSelectLambdaQueryWrapper | `...extension.AbstractSelectLambdaQueryWrapper` | `...extension.wrapper.AbstractSelectLambdaQueryWrapper` |
| AbstractSetLambdaQueryWrapper | `...extension.AbstractSetLambdaQueryWrapper` | `...extension.wrapper.AbstractSetLambdaQueryWrapper` |
| AbstractSubLambdaQueryWrapper | `...extension.AbstractSubLambdaQueryWrapper` | `...extension.wrapper.AbstractSubLambdaQueryWrapper` |
| AbstractSubSqlLambdaQueryWrapper | `...extension.AbstractSubSqlLambdaQueryWrapper` | `...extension.wrapper.AbstractSubSqlLambdaQueryWrapper` |
| AbstractWhereLambdaQueryWrapper | `...extension.AbstractWhereLambdaQueryWrapper` | `...extension.wrapper.AbstractWhereLambdaQueryWrapper` |
| DuplicateCaseLambdaQueryWrapper | `...extension.DuplicateCaseLambdaQueryWrapper` | `...extension.wrapper.DuplicateCaseLambdaQueryWrapper` |
| DuplicateFunctionLambdaQueryWrapper | `...extension.DuplicateFunctionLambdaQueryWrapper` | `...extension.wrapper.DuplicateFunctionLambdaQueryWrapper` |
| DuplicateSetLambdaQueryWrapper | `...extension.DuplicateSetLambdaQueryWrapper` | `...extension.wrapper.DuplicateSetLambdaQueryWrapper` |
| DuplicateWhereLambdaQueryWrapper | `...extension.DuplicateWhereLambdaQueryWrapper` | `...extension.wrapper.DuplicateWhereLambdaQueryWrapper` |
| GroupCaseLambdaQueryWrapper | `...extension.GroupCaseLambdaQueryWrapper` | `...extension.wrapper.GroupCaseLambdaQueryWrapper` |
| GroupFunctionLambdaQueryWrapper | `...extension.GroupFunctionLambdaQueryWrapper` | `...extension.wrapper.GroupFunctionLambdaQueryWrapper` |
| GroupLambdaQueryWrapper | `...extension.GroupLambdaQueryWrapper` | `...extension.wrapper.GroupLambdaQueryWrapper` |
| GroupWhereLambdaQueryWrapper | `...extension.GroupWhereLambdaQueryWrapper` | `...extension.wrapper.GroupWhereLambdaQueryWrapper` |
| JoinLambdaQueryWrapper | `...extension.JoinLambdaQueryWrapper` | `...extension.wrapper.JoinLambdaQueryWrapper` |
| NonValueSubSqlLambdaQueryWrapper | `...extension.NonValueSubSqlLambdaQueryWrapper` | `...extension.wrapper.NonValueSubSqlLambdaQueryWrapper` |
| NormalCaseLambdaQueryWrapper | `...extension.NormalCaseLambdaQueryWrapper` | `...extension.wrapper.NormalCaseLambdaQueryWrapper` |
| NormalFunctionLambdaQueryWrapper | `...extension.NormalFunctionLambdaQueryWrapper` | `...extension.wrapper.NormalFunctionLambdaQueryWrapper` |
| NormalSetLambdaQueryWrapper | `...extension.NormalSetLambdaQueryWrapper` | `...extension.wrapper.NormalSetLambdaQueryWrapper` |
| NormalWhereLambdaQueryWrapper | `...extension.NormalWhereLambdaQueryWrapper` | `...extension.wrapper.NormalWhereLambdaQueryWrapper` |
| OrderLambdaQueryWrapper | `...extension.OrderLambdaQueryWrapper` | `...extension.wrapper.OrderLambdaQueryWrapper` |
| SelectLambdaQueryWrapper | `...extension.SelectLambdaQueryWrapper` | `...extension.wrapper.SelectLambdaQueryWrapper` |
| SingleValueSubSqlLambdaQueryWrapper | `...extension.SingleValueSubSqlLambdaQueryWrapper` | `...extension.wrapper.SingleValueSubSqlLambdaQueryWrapper` |
| SubSelectLambdaQueryWrapper | `...extension.SubSelectLambdaQueryWrapper` | `...extension.wrapper.SubSelectLambdaQueryWrapper` |
| SubSqlLambdaQueryWrapper | `...extension.SubSqlLambdaQueryWrapper` | `...extension.wrapper.SubSqlLambdaQueryWrapper` |
| **core/** (4 个) | | |
| ExQueryWrapper | `...extension.ExQueryWrapper` | `...extension.core.ExQueryWrapper` |
| ExecutableQueryWrapper | `...extension.ExecutableQueryWrapper` | `...extension.core.ExecutableQueryWrapper` |
| LambdaQueryWrapper | `...extension.LambdaQueryWrapper` | `...extension.core.LambdaQueryWrapper` |
| Converter | `...extension.Converter` | `...extension.core.Converter` |
| **stream/** (9 个) | | |
| MybatisStream | `...extension.MybatisStream` | `...extension.stream.MybatisStream` |
| MybatisQueryableStream | `...extension.MybatisQueryableStream` | `...extension.stream.MybatisQueryableStream` |
| MybatisQueryableStream1 | `...extension.MybatisQueryableStream1` | `...extension.stream.MybatisQueryableStream1` |
| MybatisQueryableStream2 | `...extension.MybatisQueryableStream2` | `...extension.stream.MybatisQueryableStream2` |
| MybatisQueryableStream3 | `...extension.MybatisQueryableStream3` | `...extension.stream.MybatisQueryableStream3` |
| MybatisQueryableStream4 | `...extension.MybatisQueryableStream4` | `...extension.stream.MybatisQueryableStream4` |
| MybatisQueryableStream5 | `...extension.MybatisQueryableStream5` | `...extension.stream.MybatisQueryableStream5` |
| MybatisQueryableStreamMany | `...extension.MybatisQueryableStreamMany` | `...extension.stream.MybatisQueryableStreamMany` |
| MybatisExecutableStream | `...extension.MybatisExecutableStream` | `...extension.stream.MybatisExecutableStream` |
| **value/** (7 个) | | |
| SingleValue | `...extension.SingleValue` | `...extension.value.SingleValue` |
| SingleStringValue | `...extension.SingleStringValue` | `...extension.value.SingleStringValue` |
| SingleLongValue | `...extension.SingleLongValue` | `...extension.value.SingleLongValue` |
| SingleIntegerValue | `...extension.SingleIntegerValue` | `...extension.value.SingleIntegerValue` |
| SingleBooleanValue | `...extension.SingleBooleanValue` | `...extension.value.SingleBooleanValue` |
| SingleDateValue | `...extension.SingleDateValue` | `...extension.value.SingleDateValue` |
| NonValue | `...extension.NonValue` | `...extension.value.NonValue` |
| **metadata/** (6 个) | | |
| ColumnInfo | `...extension.ColumnInfo` | `...extension.metadata.ColumnInfo` |
| TableInfo | `...extension.TableInfo` | `...extension.metadata.TableInfo` |
| MysqlDataType | `...extension.MysqlDataType` | `...extension.metadata.MysqlDataType` |
| ProcedureParam | `...extension.ProcedureParam` | `...extension.metadata.ProcedureParam` |
| ProcedureParamDef | `...extension.ProcedureParamDef` | `...extension.metadata.ProcedureParamDef` |
| Struct | `...extension.Struct` | `...extension.metadata.Struct` |
| **support/** (2 个) | | |
| StringUtils | `...extension.StringUtils` | `...extension.support.StringUtils` |
| LambdaOrderItem | `...extension.LambdaOrderItem` | `...extension.support.LambdaOrderItem` |
| **bo/key/** (4 个) | | |
| BiMapKey | `...extension.bo.BiMapKey` | `...extension.bo.key.BiMapKey` |
| MapKey3 | `...extension.bo.MapKey3` | `...extension.bo.key.MapKey3` |
| MapKey4 | `...extension.bo.MapKey4` | `...extension.bo.key.MapKey4` |
| MapKey5 | `...extension.bo.MapKey5` | `...extension.bo.key.MapKey5` |
| **bo/functional/** (21 个) | | |
| Function3..15 | `...extension.bo.FunctionN` | `...extension.bo.functional.FunctionN` |
| Consumer3..10 | `...extension.bo.ConsumerN` | `...extension.bo.functional.ConsumerN` |
| **不变（保持原位）** | | |
| MysqlBaseMapper | `...extension.mapper.MysqlBaseMapper` | 不变 |
| IMysqlServiceBase | `...extension.service.IMysqlServiceBase` | 不变 |
| MysqlServiceBaseImpl | `...extension.service.impl.MysqlServiceBaseImpl` | 不变 |
| PageVo / SortVo / BiList | `...extension.bo.{Cls}` | 不变 |
| MybatisUtil / ReflectUtils | `...toolkit.{Cls}` | 不变 |

`...` = `com.baomidou.mybatisplus`

---

## `migrate-to-4.0.sh`（Bash / Git Bash / WSL）

```bash
#!/usr/bin/env bash
# Run from your project root. Modifies every .java file under src/.
set -euo pipefail

PREFIX="com.baomidou.mybatisplus.extension"

declare -A MAP=(
  # wrapper/
  [AbstractCaseLambdaQueryWrapper]=wrapper
  [AbstractFunctionLambdaQueryWrapper]=wrapper
  [AbstractSelectLambdaQueryWrapper]=wrapper
  [AbstractSetLambdaQueryWrapper]=wrapper
  [AbstractSubLambdaQueryWrapper]=wrapper
  [AbstractSubSqlLambdaQueryWrapper]=wrapper
  [AbstractWhereLambdaQueryWrapper]=wrapper
  [DuplicateCaseLambdaQueryWrapper]=wrapper
  [DuplicateFunctionLambdaQueryWrapper]=wrapper
  [DuplicateSetLambdaQueryWrapper]=wrapper
  [DuplicateWhereLambdaQueryWrapper]=wrapper
  [GroupCaseLambdaQueryWrapper]=wrapper
  [GroupFunctionLambdaQueryWrapper]=wrapper
  [GroupLambdaQueryWrapper]=wrapper
  [GroupWhereLambdaQueryWrapper]=wrapper
  [JoinLambdaQueryWrapper]=wrapper
  [NonValueSubSqlLambdaQueryWrapper]=wrapper
  [NormalCaseLambdaQueryWrapper]=wrapper
  [NormalFunctionLambdaQueryWrapper]=wrapper
  [NormalSetLambdaQueryWrapper]=wrapper
  [NormalWhereLambdaQueryWrapper]=wrapper
  [OrderLambdaQueryWrapper]=wrapper
  [SelectLambdaQueryWrapper]=wrapper
  [SingleValueSubSqlLambdaQueryWrapper]=wrapper
  [SubSelectLambdaQueryWrapper]=wrapper
  [SubSqlLambdaQueryWrapper]=wrapper
  # core/
  [ExQueryWrapper]=core
  [ExecutableQueryWrapper]=core
  [LambdaQueryWrapper]=core
  [Converter]=core
  # stream/
  [MybatisStream]=stream
  [MybatisQueryableStream]=stream
  [MybatisQueryableStream1]=stream
  [MybatisQueryableStream2]=stream
  [MybatisQueryableStream3]=stream
  [MybatisQueryableStream4]=stream
  [MybatisQueryableStream5]=stream
  [MybatisQueryableStreamMany]=stream
  [MybatisExecutableStream]=stream
  # value/
  [SingleValue]=value
  [SingleStringValue]=value
  [SingleLongValue]=value
  [SingleIntegerValue]=value
  [SingleBooleanValue]=value
  [SingleDateValue]=value
  [NonValue]=value
  # metadata/
  [ColumnInfo]=metadata
  [TableInfo]=metadata
  [MysqlDataType]=metadata
  [ProcedureParam]=metadata
  [ProcedureParamDef]=metadata
  [Struct]=metadata
  # support/
  [StringUtils]=support
  [LambdaOrderItem]=support
)

for cls in "${!MAP[@]}"; do
  sub="${MAP[$cls]}"
  find src -name '*.java' -type f -exec sed -i \
    "s|import ${PREFIX}\.${cls};|import ${PREFIX}.${sub}.${cls};|g" {} +
done

# bo/key/
for cls in BiMapKey MapKey3 MapKey4 MapKey5; do
  find src -name '*.java' -type f -exec sed -i \
    "s|import ${PREFIX}\.bo\.${cls};|import ${PREFIX}.bo.key.${cls};|g" {} +
done

# bo/functional/  Function3..15 + Consumer3..10
for n in 3 4 5 6 7 8 9 10 11 12 13 14 15; do
  find src -name '*.java' -type f -exec sed -i \
    "s|import ${PREFIX}\.bo\.Function${n};|import ${PREFIX}.bo.functional.Function${n};|g" {} +
done
for n in 3 4 5 6 7 8 9 10; do
  find src -name '*.java' -type f -exec sed -i \
    "s|import ${PREFIX}\.bo\.Consumer${n};|import ${PREFIX}.bo.functional.Consumer${n};|g" {} +
done

# 类名重命名（4.0 新增；可推迟到 4.1 旧名空壳删除前再做）
# 注：前 3 个旧名在 4.0 仍可用（@Deprecated 别名空壳），4.1 才删除；
#     MysqlDataType 无别名，必须现在替换。
find src -name '*.java' -type f -exec sed -i \
    -e 's|\bMysqlBaseMapper\b|StreamBaseMapper|g' \
    -e 's|\bIMysqlServiceBase\b|IStreamService|g' \
    -e 's|\bMysqlServiceBaseImpl\b|StreamServiceImpl|g' \
    -e 's|\bMysqlDataType\b|SqlDataType|g' \
    {} +

echo "Done. Review with: git diff --stat"
```

## `migrate-to-4.0.ps1`（PowerShell / Windows）

```powershell
# Run from your project root.
$prefix = 'com.baomidou.mybatisplus.extension'

$map = @{
    # wrapper/
    'AbstractCaseLambdaQueryWrapper' = 'wrapper'
    'AbstractFunctionLambdaQueryWrapper' = 'wrapper'
    'AbstractSelectLambdaQueryWrapper' = 'wrapper'
    'AbstractSetLambdaQueryWrapper' = 'wrapper'
    'AbstractSubLambdaQueryWrapper' = 'wrapper'
    'AbstractSubSqlLambdaQueryWrapper' = 'wrapper'
    'AbstractWhereLambdaQueryWrapper' = 'wrapper'
    'DuplicateCaseLambdaQueryWrapper' = 'wrapper'
    'DuplicateFunctionLambdaQueryWrapper' = 'wrapper'
    'DuplicateSetLambdaQueryWrapper' = 'wrapper'
    'DuplicateWhereLambdaQueryWrapper' = 'wrapper'
    'GroupCaseLambdaQueryWrapper' = 'wrapper'
    'GroupFunctionLambdaQueryWrapper' = 'wrapper'
    'GroupLambdaQueryWrapper' = 'wrapper'
    'GroupWhereLambdaQueryWrapper' = 'wrapper'
    'JoinLambdaQueryWrapper' = 'wrapper'
    'NonValueSubSqlLambdaQueryWrapper' = 'wrapper'
    'NormalCaseLambdaQueryWrapper' = 'wrapper'
    'NormalFunctionLambdaQueryWrapper' = 'wrapper'
    'NormalSetLambdaQueryWrapper' = 'wrapper'
    'NormalWhereLambdaQueryWrapper' = 'wrapper'
    'OrderLambdaQueryWrapper' = 'wrapper'
    'SelectLambdaQueryWrapper' = 'wrapper'
    'SingleValueSubSqlLambdaQueryWrapper' = 'wrapper'
    'SubSelectLambdaQueryWrapper' = 'wrapper'
    'SubSqlLambdaQueryWrapper' = 'wrapper'
    # core/
    'ExQueryWrapper' = 'core'
    'ExecutableQueryWrapper' = 'core'
    'LambdaQueryWrapper' = 'core'
    'Converter' = 'core'
    # stream/
    'MybatisStream' = 'stream'
    'MybatisQueryableStream' = 'stream'
    'MybatisQueryableStream1' = 'stream'
    'MybatisQueryableStream2' = 'stream'
    'MybatisQueryableStream3' = 'stream'
    'MybatisQueryableStream4' = 'stream'
    'MybatisQueryableStream5' = 'stream'
    'MybatisQueryableStreamMany' = 'stream'
    'MybatisExecutableStream' = 'stream'
    # value/
    'SingleValue' = 'value'
    'SingleStringValue' = 'value'
    'SingleLongValue' = 'value'
    'SingleIntegerValue' = 'value'
    'SingleBooleanValue' = 'value'
    'SingleDateValue' = 'value'
    'NonValue' = 'value'
    # metadata/
    'ColumnInfo' = 'metadata'
    'TableInfo' = 'metadata'
    'MysqlDataType' = 'metadata'
    'ProcedureParam' = 'metadata'
    'ProcedureParamDef' = 'metadata'
    'Struct' = 'metadata'
    # support/
    'StringUtils' = 'support'
    'LambdaOrderItem' = 'support'
}

Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object {
    $file = $_.FullName
    $content = Get-Content -Raw $file
    foreach ($cls in $map.Keys) {
        $sub = $map[$cls]
        $content = $content -replace "import\s+$([regex]::Escape($prefix))\.$cls;", "import $prefix.$sub.$cls;"
    }
    # bo/key/
    foreach ($cls in 'BiMapKey','MapKey3','MapKey4','MapKey5') {
        $content = $content -replace "import\s+$([regex]::Escape($prefix))\.bo\.$cls;", "import $prefix.bo.key.$cls;"
    }
    # bo/functional/ Function3..15 + Consumer3..10
    foreach ($n in 3..15) {
        $content = $content -replace "import\s+$([regex]::Escape($prefix))\.bo\.Function$n;", "import $prefix.bo.functional.Function$n;"
    }
    foreach ($n in 3..10) {
        $content = $content -replace "import\s+$([regex]::Escape($prefix))\.bo\.Consumer$n;", "import $prefix.bo.functional.Consumer$n;"
    }
    # 类名重命名（4.0 新增）
    $content = $content -replace '\bMysqlBaseMapper\b', 'StreamBaseMapper'
    $content = $content -replace '\bIMysqlServiceBase\b', 'IStreamService'
    $content = $content -replace '\bMysqlServiceBaseImpl\b', 'StreamServiceImpl'
    $content = $content -replace '\bMysqlDataType\b', 'SqlDataType'
    Set-Content -Path $file -Value $content -NoNewline
}

Write-Host "Done. Review with: git diff --stat"
```

---

## 验证

迁完后跑你项目的编译：

```powershell
mvn clean compile
# 或
./gradlew compileJava
```

如有 "cannot find symbol" 错误，看看是否对应 `MIGRATION-4.0.md` 漏掉了哪一项；可能也是你自己用了 fully-qualified（`com.baomidou.mybatisplus.extension.NormalWhereLambdaQueryWrapper.class` 这种）而非 import——这种情况 IDE 重导不会改，需要手动改。

---

<a id="dialect-extension"></a>

## 方言扩展（4.0 新增 SPI）

4.0 把分页 / 行锁 / 字符串拼接 / 类型转换等 SQL 片段抽到 `SqlDialect` SPI。默认 `MySqlDialect`，**3.x 用户升级 4.0 无感**。

### 切换内置方言

```java
// 应用启动时（如 Spring Boot @PostConstruct）
DialectRegistry.use(DbType.POSTGRESQL);   // 规划 4.0.1 内置
DialectRegistry.use(DbType.DAMENG);       // 规划 4.0.1 内置
```

### 注册自定义方言（零侵入）

1. 实现 `SqlDialect`（建议继承 `MySqlDialect` 只覆写差异方法）：

```java
public class MyOracleDialect extends MySqlDialect {
    @Override public DbType dbType() { return DbType.CUSTOM; }
    @Override public String paginate(String sql, long off, long lim) {
        return sql + " OFFSET " + off + " ROWS FETCH NEXT " + lim + " ROWS ONLY";
    }
    @Override public String quoteIdentifier(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
    // ... 其他差异方法
}
```

2. 在你的 jar 里建文件 `META-INF/services/com.baomidou.mybatisplus.extension.dialect.SqlDialect`：

```
com.example.MyOracleDialect
```

3. 启动时切换：

```java
DialectRegistry.use(new MyOracleDialect());
```

ServiceLoader 会自动扫描注册，启动后即可用 `DialectRegistry.current()` 获取。

### 行锁细粒度示例

```java
// SELECT ... FOR UPDATE NOWAIT
userService.stream()
    .filter(w -> w.eq(User::getId, 1))
    .forUpdateNoWait()
    .findFirst();

// SELECT ... FOR UPDATE SKIP LOCKED（消费抢锁场景）
userService.stream()
    .filter(w -> w.eq(User::getStatus, "pending"))
    .forUpdateSkipLocked()
    .limit(10)
    .collect(Collectors.toList());

// SELECT ... FOR UPDATE WAIT 5（DM 支持，MySQL 抛 UnsupportedOperationException）
userService.stream()
    .filter(w -> w.eq(User::getId, 1))
    .forUpdateWait(5)
    .findFirst();
```

<a id="terminal-operations"></a>

### SQL-aware Terminal Operations 示例（4.0 重头戏）

之前用户大量写"全量加载 + 内存 Collector"：

```java
// 旧（全量加载到内存，再 stream + JDK Collector）
Map<Long, String> userMap = userService.list(w -> w.eq(User::getStatus, 1))
    .stream()
    .collect(Collectors.toMap(User::getId, User::getName));
```

**4.0 起**：

```java
// SELECT id, name FROM user WHERE status = 1 —— 只取需要的两列
Map<Long, String> userMap = userService.stream()
    .filter(w -> w.eq(User::getStatus, 1))
    .toMap(User::getId, User::getName);
```

完整 9 个方法（详见 CHANGELOG）：

```java
// 单列取集合
Set<String> emails = userService.stream().toSet(User::getEmail);

// 键值 Map（key 冲突时后者覆盖前者）
Map<Long, String> map = userService.stream().toMap(User::getId, User::getName);

// 键值 Map（自定义冲突合并）
Map<Long, String> merged = userService.stream()
    .toMap(User::getDept, User::getName, (a, b) -> a + "," + b);

// 应用层分组（保留完整实体）
Map<Long, List<User>> byDept = userService.stream().groupingBy(User::getDeptId);

// SQL 下推 COUNT/SUM/AVG/MAX/MIN（数据库完成聚合，1 条结果回 JVM）
Map<Integer, Long> statusCount = userService.stream().toMapCount(User::getStatus);
Map<Long, BigDecimal> deptSalarySum = userService.stream()
    .filter(w -> w.eq(User::getActive, true))
    .toMapSum(User::getDeptId, User::getSalary);
Map<Long, Double> deptAvg = userService.stream().toMapAvg(User::getDeptId, User::getSalary);
Map<Long, LocalDate> deptLatestJoin = userService.stream().toMapMax(User::getDeptId, User::getJoinDate);
Map<Long, BigDecimal> deptMinSalary = userService.stream().toMapMin(User::getDeptId, User::getSalary);
```

### EXISTS / NOT EXISTS 示例

```java
// 查询有订单的用户
userService.list(w -> w.exists(
    "SELECT 1 FROM orders o WHERE o.user_id = " + StreamBaseMapper.fromAlias(User.class) + ".id"
));
```
