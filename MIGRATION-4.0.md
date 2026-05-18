# 4.0.0.0 迁移指南

3.5.16.x → 4.0.0.0 的 **唯一 breaking change**：所有类按职责拆到了 6 个子包，平铺的 `com.baomidou.mybatisplus.extension.*` 顶层不再有任何业务类。

API 行为、方法签名、SQL 渲染、版本号约定（`{MP-ver}.{patch}`）**完全不变**。

---

## 推荐迁移方式：IDE 自动重导入

**IntelliJ IDEA**：升级依赖到 `4.0.0.0` 后，对项目执行 `Ctrl+Alt+O`（Optimize Imports），所有 import 自动解析到新路径。一分钟搞定。

**Eclipse**：`Source → Organize Imports`（`Ctrl+Shift+O`）效果相同。

**VS Code + Java 插件**：右键文件 → "Organize Imports"。

如果你的项目用到的本库类不多（通常 < 10 个），IDE 重导就足够，无需查表。

---

## 备选：CLI 批量替换

如果你的项目几百处 import 散落、不想手点 IDE，跑下方 `migrate-to-4.0.sh`（PowerShell 版见底部）。脚本会扫你项目下所有 `.java` 文件，把旧 import 路径机械替换为新路径。

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
