# Arity-N 类维护模板

本项目仅有两组"按参数个数模板化"的类，新增 arity 时需照模板复制。

> **4.0 起删除 Function3..15 + Consumer3..10**（21 个类）—— 经生产项目 `ceremonyproapp` 实测：业务代码 0 调用，是预防性 over-engineering。如未来真有解包式 lambda 需求，按当时具体场景设计 API，不再预挖坑。

---

## 1. Arity 模板族清单（4.0 修订后）

| 族 | 包 | 现有档位 | 单类大小 | 总行数 |
|---|---|---|---|---|
| `MapKey3..5` + `BiMapKey` | `extension.bo.key` | 2..5（4 档） | ~60 行 | ~240 |
| `MybatisQueryableStream1..5` | `extension.stream` | 1..5 + Many（6 档） | ~200 行 | ~1200 |

### 真实使用证据（ceremonyproapp）

- **`BiMapKey`**：70+ 处真实引用，主用于"主详表关联保存"和"双键分组去重"
- **`MapKey3..5`**：10-20 处引用，多用作 `stream().collect(Collectors.toSet())` 的复合键
- **`MybatisQueryableStream1..5`**：通过 `service.stream()` 间接使用

---

## 2. 新增一档的复制步骤

### 2.1 新增 MapKey（如 MapKey6）

```bash
cp src/main/java/com/baomidou/mybatisplus/extension/bo/key/MapKey5.java \
   src/main/java/com/baomidou/mybatisplus/extension/bo/key/MapKey6.java
```

改三处：
- 类名 `MapKey5` → `MapKey6`
- 泛型参数 `<T1..T5>` → `<T1..T6>`
- 构造器、`key6` 字段、`getKey6()`、`equals/hashCode` 各加一项

同步要做：如果新增需要支持 6 列流投影，还要加 `MybatisQueryableStream6`（见 2.2）。

### 2.2 新增 MybatisQueryableStream（如 MybatisQueryableStream6）

**最复杂的一档**。流程：

1. 复制 `MybatisQueryableStream5.java` → `MybatisQueryableStream6.java`
2. 改类名、泛型参数（增加 `R6`）、构造函数
3. 把所有 `MapKey5<R1, R2, R3, R4, R5>` 换成 `MapKey6<R1, R2, R3, R4, R5, R6>`
4. 在 `IStreamService` 和 `MybatisStream` 顶层抽象增加返回 `MybatisQueryableStream6` 的链式方法
5. 跑 `mvn compile` 验证

---

## 3. 为什么不做编译期代码生成

业界 arity-N 处理主流方案：

| 方案 | 项目案例 | 评价 |
|---|---|---|
| Scala 脚本 + Maven `generate-sources` | Vavr | 强，但需 Scala/Groovy 模板引擎依赖（违反"不引入第三方"原则）|
| Annotation Processor | 个别小项目 | AST 改写复杂、IDE 友好度低 |
| **手写**（本项目当前方案） | jOOL、Spring Tuple | 简单可靠 |
| Manifold AST 插件 | 0 库采纳 | 不推荐 |

**本项目决策**：

- `MapKey` / `MybatisQueryableStream`：每族 4-6 个类、改动频次低（新增 arity = 业务真有需求才扩档），手写成本可控。**不引入构建依赖**，符合用户"干净"原则。
- 真的频繁扩 arity 时（>10 档），再评估 codegen，4.1 单独 spec 决策。

---

## 4. Checklist：新增 arity 前确认

- [ ] 业务真的需要新档吗？（避免预防性 over-engineering——参考 Function3..15 教训）
- [ ] 现有最大档位是多少？复制谁？
- [ ] 跨族联动：新档涉及 `MapKeyN + MybatisQueryableStreamN` 配套吗？
- [ ] `IStreamService` / `MybatisStream` 是否需要新方法暴露新档？
- [ ] CHANGELOG 加一条 `### Added`
