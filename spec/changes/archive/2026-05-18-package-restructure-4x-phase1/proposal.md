# Proposal：包结构重组 4.0.0.0 + arity 痛点缓解

## Why

**问题 1（包结构）**：`com.baomidou.mybatisplus.extension/` 顶层平铺 54 个 java 文件，包括 29 个 wrapper 家族、7 个 Stream、6 个 SingleValue、12 个杂项（元数据 / 工具）。同维度类对齐良好（命名网格化），但物理上没分组，新人首次阅读门槛高、IDE 无法靠目录折叠。

**问题 2（arity 痛点）**：用户痛点不是 `Function3..15`（极少改），而是 `MybatisQueryableStream1..5 + Many`——每个 ~200 行的高复杂度类，新增"列投影类"方法时要梭 6 处。

**决策容忍度**：用户已确认接受 breaking change（发 `4.0.0.0`），主线 MP 当前最新就是 3.5.16，没有上游版本压力。

## What Changes

### 必做 P0：包重组（一层细分，~54 → 6 子包）

```
com.baomidou.mybatisplus.extension/
├── core/         (4) ExQueryWrapper, ExecutableQueryWrapper, LambdaQueryWrapper [抽象], Converter
├── wrapper/      (28) 所有 *LambdaQueryWrapper（保留扁平 + 命名网格化）
│   ├── (Abstract|Normal|Group|Duplicate)WhereLambdaQueryWrapper
│   ├── (Abstract|Normal|Group|Duplicate)FunctionLambdaQueryWrapper
│   ├── (Abstract|Normal|Duplicate)SetLambdaQueryWrapper
│   ├── (Abstract|Normal|Group|Duplicate)CaseLambdaQueryWrapper
│   ├── AbstractSelectLambdaQueryWrapper / SelectLambdaQueryWrapper / SubSelectLambdaQueryWrapper
│   ├── AbstractSubLambdaQueryWrapper / AbstractSubSqlLambdaQueryWrapper / SubSqlLambdaQueryWrapper / SingleValueSubSqlLambdaQueryWrapper / NonValueSubSqlLambdaQueryWrapper
│   ├── JoinLambdaQueryWrapper / GroupLambdaQueryWrapper / OrderLambdaQueryWrapper
├── stream/       (7) MybatisStream, MybatisQueryableStream{,1..5,Many}, MybatisExecutableStream
├── value/        (7) Single{,String,Long,Integer,Boolean,Date}Value, NonValue
├── metadata/     (6) ColumnInfo, TableInfo, MysqlDataType, ProcedureParam(Def), Struct
├── support/      (2) StringUtils, LambdaOrderItem
├── bo/           现有 28 个，内部小整理 ↓
│   ├── functional/  (21) Function3..15, Consumer3..10
│   ├── key/         (4) MapKey3..5, BiMapKey
│   └── (root)       (3) PageVo, SortVo, BiList
├── mapper/       (1) MysqlBaseMapper（不动）
└── service/      (2) IMysqlServiceBase + impl/（不动）
```

**关键设计点**：
- **不在 wrapper/ 下再细分 where/select/set/function/case** —— 命名前缀已足够分组，再细分 IDE 跳包成本反而增加
- **case 不做子包名**（保留字）；保留扁平
- **bo/ 仅在内部分子包**：bo 是用户偶尔 import 的 lambda 类型，内部小重组 OK；顶部留 PageVo/SortVo/BiList（普通容器）

### 可选 P1：MybatisQueryableStream 代码生成

**动机**：6 个 Stream 类（1..5 + Many）总计 ~1200 行 90% 重复结构，差异只在投影列数（R, R1+R2, R1+R2+R3 ...）。手工同步是真痛点。

**方案**：`gmaven-plus-plugin` + Groovy 脚本读 `templates/MybatisQueryableStreamN.java.template` 在 `generate-sources` 阶段生成 5 个 stream 类到 `target/generated-sources/`。Many 类差异大保持手写。

**收益**：新增 stream 方法 1 处 vs 6 处；可扩展到 arity 6/7/8。  
**成本**：~4-6 小时初始化（模板抽取 + Maven 配置）+ 1 次 IDE 集成验证；后续零维护。

### 必做 P0.5（追加）：泛型反射热路径优化

**动机**：4.0 是改 API 表面 / 内部反射策略的最后窗口；4.x 系列锁后再改就又是 breaking。借包重组的 PR 顺带把"绕"的代码做局部清理，主要瞄准 SFunction 解析的高频热路径。

**4 项优化**：

| # | 改动 | 文件 | 价值 | API 影响 |
|---|---|---|---|---|
| 1 | `ReflectUtils.getLambda()` + 正则提取 → 加 `ClassValue<LambdaInfo>` 缓存层；`LambdaInfo` 含 className（字符串切片）、implMethodName、propertyName | `toolkit/ReflectUtils.java`、`core/LambdaQueryWrapper.java` | **高**：每次 SFunction 解析走缓存，命中后 0 反射；首次解析也省掉正则编译 | 无（纯内部）|
| 2 | `getSubSqlSegment(predicate, Class<S>)` 保留作底层 API；在 `AbstractWhereLambdaQueryWrapper` 等子类加便捷重载 `getSubSqlSegment(predicate)` 内部走 `this.fClazz` | `wrapper/AbstractWhereLambdaQueryWrapper.java` 等 | 中（代码可读性）| 仅新增 protected 方法，不删旧的 |
| 3 | `clazz.newInstance()` 反射构造缓存 | `core/LambdaQueryWrapper.java` | **延后**：需 benchmark 验证收益，留 4.1 | — |
| 4 | MethodHandle 替代 `writeReplace` 反射 | `toolkit/ReflectUtils.java` | 已被 #1 的 ClassValue 缓存吸收（缓存命中后 writeReplace 都跳过）| — |

**实际只做 #1 + #2**，#3 / #4 已经被 #1 覆盖或推迟。

**P0.5 工作量**：~2h（含写一个最小 benchmark 验证 ClassValue 缓存命中率）

### 不做 P2：Function/Consumer/MapKey 代码生成

`Function3..15` 等历史以来从未改过，痛点不真实。**保持手写**，加一份 `dev-docs/ARITY-TEMPLATE.md` 说明新增 arity 时的复制粘贴模板和 IDE Live Template 示例即可。

---

## 迁移路径

### 1. CHANGELOG.md 4.0.0.0 章节

```markdown
## [4.0.0.0] - 2026-MM-DD

### ⚠️ BREAKING CHANGES
- 所有类都从 `com.baomidou.mybatisplus.extension` 顶层移到了对应子包
- 用户需要在 IDE 中执行 "Optimize Imports"（IntelliJ: Ctrl+Alt+O）自动重新解析所有 import
- 详细迁移指南：[MIGRATION-4.0.md](MIGRATION-4.0.md)

### Changed
- 包结构重组：54 个文件按职责拆 6 子包
- MybatisQueryableStream1..5 改为编译期生成（用户无感）
```

### 2. MIGRATION-4.0.md（用户迁移指南）

提供一张完整的旧路径 → 新路径表，让用户能 sed 批量替换 import：

```
旧路径                                                  → 新路径
com.baomidou.mybatisplus.extension.LambdaQueryWrapper  → com.baomidou.mybatisplus.extension.core.LambdaQueryWrapper
com.baomidou.mybatisplus.extension.NormalWhereLambdaQueryWrapper → com.baomidou.mybatisplus.extension.wrapper.NormalWhereLambdaQueryWrapper
...
```

提供一个 `sed` 脚本（或 PowerShell 等价物）让用户一键替换：

```bash
# migrate-to-4.0.sh
sed -i 's|extension\.LambdaQueryWrapper|extension.core.LambdaQueryWrapper|g' $(grep -rl "extension\.LambdaQueryWrapper" src/)
# ... 50+ rules
```

### 3. package-info.java（每个子包写一句话）

例：`wrapper/package-info.java`

```java
/**
 * Wrapper 家族：按 (上下文 × 角色) 两维度命名网格化。
 * - 上下文前缀：Abstract / Normal / Group / Duplicate / Sub
 * - 角色后缀：Where / Select / Set / Function / Case
 * 例如 GroupWhereLambdaQueryWrapper = 分组场景的 WHERE 条件，附带 HAVING 能力。
 */
package com.baomidou.mybatisplus.extension.wrapper;
```

### 4. README + 使用文档.md + docs 站

- README 项目结构图更新（指向新包路径）
- `使用文档.md` 内有 import 示例的更新
- `mybatis-plus-stream-docs/docs/pages/` 全文搜替 import 路径

---

## Impact / Risks

| 风险 | 影响 | 缓解 |
|---|---|---|
| **用户 import 全失效** | 所有依赖 3.5.x 的下游升 4.x 必须改 import | 提供 MIGRATION-4.0.md + sed 脚本一键替换；IDE Optimize Imports 自动解决 |
| **反射 / SpringBean 名硬编码失效** | 极少（本项目无 @Component 类） | 已确认 wrapper 全是 lambda 参数 / 显式 new，无反射查找类名 |
| **codegen 引入构建依赖** | 增加 `gmaven-plus-plugin` 依赖；新人 clone 后首次构建需要联网拉插件 | P1 是可选项，可推迟到 4.1；首版 4.0 只做包重组 |
| **maven-source-plugin 与生成源码的交互** | 生成的 java 不应进 sources.jar？还是应该进？ | 默认让 generated-sources 进 sources.jar（与 Lombok 行为一致） |
| **回滚成本** | 4.0 发出后撤回需要发 4.0.1 重导回 3.x 路径 | 4.0 发版前在 main 之外的 release 分支充分验证；Sonatype Central 24h 内可撤稿 |

### 回滚预案

1. **发版前**：在 `release/4.0.0.0` 分支充分测试（写一个最小 demo project 验证 import 路径 + IDE auto-complete）
2. **发版后 24h 内出问题**：登录 https://central.sonatype.com 撤稿（unpublish）；同时回滚 main 上 4.0.0.0 commit；GitHub Release 标记 deprecated
3. **超过 24h**：发 4.0.1 把 import 路径**全部重导回** `extension/` 平铺（package-info.java 仍保留作 JavaDoc 导航）；CHANGELOG 注明 4.0.0 已被撤稿

---

## 工作量估算

| 阶段 | 工作量 | 说明 |
|---|---|---|
| **P0-a 文件迁移** | 1.5h | 机械操作：`git mv` 54 个文件到对应子包 |
| **P0-b 包声明 + import 更新** | 1.5h | 每个 java 文件改 `package` 行 + 同一项目内所有 import；用 IDE Refactor > Move 一键搞定，外加跑 mvn compile 验证 |
| **P0-c package-info.java** | 0.5h | 6 个子包各写一句话导航 |
| **P0-d CHANGELOG + MIGRATION-4.0.md** | 1h | 包括 sed 脚本生成 |
| **P0-e README / 使用文档.md / docs 站** | 1h | 更新项目结构图和示例 |
| **P0 小计** | **5.5h** | 加上等 CI + 发版流程约 7h |
| **P0.5 反射热路径优化** | 2h | ClassValue 缓存 + getSubSqlSegment 便捷重载 |
| **P1 codegen（可选）** | 4-6h | 抽 stream 模板 + 配 gmaven-plus + 验证 IDE 集成；如不做可推迟 4.1 |
| **P2 ARITY-TEMPLATE.md** | 0.5h | 不做 codegen 的替代方案 |

**推荐节奏**：
1. **4.0.0.0**：只做 P0 + P2（包重组 + arity 模板文档）
2. **4.1.0.0**：加 P1（stream codegen），如果 P0 后真的觉得 Stream 痛

这样 4.0 风险面最小（纯机械迁移，无新依赖）；4.1 是非破坏性增强。

---

## 验收标准（实施时回头对照）

- [ ] `mvn clean install` 通过，无编译错误
- [ ] `git mv` 历史保留（用 `git log --follow` 能查到原始 commit）
- [ ] 每个子包有 package-info.java
- [ ] MIGRATION-4.0.md 提供完整旧→新路径表
- [ ] README 项目结构图与实际目录一致
- [ ] 在 throwaway 测试项目里跑 `mvn dependency:get -Dartifact=io.github.kamioj:mybatis-plus-stream-boot-starter:4.0.0.0` 后写一个最小 Stream 用例，IDE auto-import 命中新路径
- [ ] CHANGELOG 4.0.0.0 节包含 BREAKING CHANGES 警告 + 迁移指南链接
