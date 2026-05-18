# Arity-N 类维护模板

本项目有几组按"参数个数"模板化的类，每次扩 arity 都得复制一份。这份文档说明：

1. **哪些类是 arity 模板族**
2. **新增一档时复制谁、改哪几处**
3. **IDE 加速复制的 Live Template 配置**
4. **为什么暂时不做编译期代码生成**

---

## 1. Arity 模板族清单

| 族 | 包 | 现有档位 | 单类大小 | 总行数 |
|---|---|---|---|---|
| `Function3..15` | `extension.bo.functional` | 3 → 15（13 档） | ~30 行 | ~390 |
| `Consumer3..10` | `extension.bo.functional` | 3 → 10（8 档） | ~25 行 | ~200 |
| `MapKey3..5` | `extension.bo.key` | 3 → 5（3 档） | ~60 行 | ~180 |
| `MybatisQueryableStream1..5` | `extension.stream` | 1 → 5 + Many（6 档）| ~200 行 | ~1200 |

**痛点等级**：

- `Function/Consumer` —— 每个 30 行的小类，新增极少（项目历史里几乎没改过）。**几乎不痛**。
- `MapKey3..5` —— 同上，3 档不会扩太多。**不痛**。
- `MybatisQueryableStream1..5` —— **真正的痛点**。每个 200 行高复杂度类，新增 stream 方法需同步 6 处。

---

## 2. 新增一档的复制步骤

### 2.1 新增 Function（如 Function16）

```bash
# 1. 复制最近一个 arity 作为模板
cp src/main/java/com/baomidou/mybatisplus/extension/bo/functional/Function15.java \
   src/main/java/com/baomidou/mybatisplus/extension/bo/functional/Function16.java

# 2. 改三处
#   - 类名 Function15 → Function16
#   - 泛型参数 <T1...T15, R> → <T1...T16, R>
#   - apply 方法签名加一个参数 t16
```

### 2.2 新增 Consumer（如 Consumer11）

同上，模板换成 `Consumer10.java`，方法名是 `accept`，无返回值。

### 2.3 新增 MapKey（如 MapKey6）

模板换成 `MapKey5.java`。注意：还要在所有引用 `MapKey5` 的地方决定要不要扩展（如 `MybatisQueryableStream6` 也要新增）。

### 2.4 新增 MybatisQueryableStream（如 MybatisQueryableStream6）

**这是最复杂的一档**。流程：

1. 复制 `MybatisQueryableStream5.java` → `MybatisQueryableStream6.java`
2. 改类名、泛型参数（增加 `R6`）、构造函数
3. 把所有 `MapKey5<R1, R2, R3, R4, R5>` 换成 `MapKey6<R1, R2, R3, R4, R5, R6>`（前提：MapKey6 已存在）
4. `.map(...)` 方法签名加 `Function7`（接受 `T, R1..R6` 共 7 个参数）—— 前提：Function7 已存在
5. 在 `IMysqlServiceBase` 和 `MybatisStream` 顶层抽象增加返回 `MybatisQueryableStream6` 的链式方法
6. 跑 `mvn compile` 验证

---

## 3. IntelliJ IDEA Live Template（加速复制）

`File → Settings → Editor → Live Templates → Java → Add Template`：

### 模板 1：多元 Function

```
abbreviation: funcN
description: Generate Function<N> functional interface

template text:
package com.baomidou.mybatisplus.extension.bo.functional;

import java.util.Objects;

/**
 * $N$-元函数。
 */
@FunctionalInterface
public interface Function$N$<$GENERICS$, R> {
    R apply($PARAMS$);
}
```

变量定义：
- `$N$` → 输入 arity 数字
- `$GENERICS$` → `groovyScript("def n = _1.toInteger(); (1..n).collect{'T'+it}.join(', ')", N)`
- `$PARAMS$` → `groovyScript("def n = _1.toInteger(); (1..n).collect{'T'+it+' t'+it}.join(', ')", N)`

用法：在新文件里输入 `funcN`，按 Tab，IDE 会问 arity，输入 `16` 回车，自动展开。

### 模板 2：多元 Consumer

类似，模板 text 用 `accept` 方法（无返回值）。

---

## 4. 为什么暂不做编译期代码生成

业界 arity-N 处理主流方案：

| 方案 | 项目案例 | 评价 |
|---|---|---|
| Scala 脚本 + Maven `generate-sources` | Vavr | 强，但需 Scala/Groovy 模板引擎依赖 |
| Annotation Processor | 个别小项目 | AST 改写复杂、IDE 友好度低，无主流库采纳 |
| 手写 + IDE Live Template | jOOL、Spring Tuple | **本项目当前方案**，简单可靠 |
| Manifold AST 插件 | 0 库采纳 | 不推荐 |

**本项目决策**：

- `Function/Consumer/MapKey`：手写成本极低（30-60 行/档，每年新增 0 次），不引入构建复杂度
- `MybatisQueryableStream`：是真正痛点（200 行/档），**未来可单独引入 Groovy + Freemarker 模板**，但 4.0.0.0 不做（先稳定包重组）。规划在 4.1.0.0 评估

---

## 5. Checklist：新增 arity 前确认

- [ ] 新档真的需要吗？（业务上有 N 列投影的需求？还是 N 元 lambda？）
- [ ] 现有最大档位是多少？复制谁？
- [ ] 跨族联动：新档涉及多个族吗（MapKeyN + FunctionN+1 + MybatisQueryableStreamN）？
- [ ] `IMysqlServiceBase` / `MybatisStream` 是否需要新方法暴露新档？
- [ ] CHANGELOG 加一条 `### Added`
