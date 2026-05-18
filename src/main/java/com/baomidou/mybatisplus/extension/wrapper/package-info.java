/**
 * Wrapper 家族：按 <b>上下文 × 角色</b> 两维度命名网格化。
 *
 * <p><b>上下文前缀</b>（决定该 wrapper 出现在什么 SQL 场景）：
 * <ul>
 *   <li>{@code Abstract*} —— 抽象基类，不直接 new</li>
 *   <li>{@code Normal*}   —— 普通 SELECT / UPDATE 场景</li>
 *   <li>{@code Group*}    —— GROUP BY + HAVING 场景，比 Normal 多 HAVING 接口</li>
 *   <li>{@code Duplicate*}—— ON DUPLICATE KEY UPDATE 场景（INSERT 时冲突分支）</li>
 *   <li>{@code Sub*}      —— 子查询场景</li>
 * </ul>
 *
 * <p><b>角色后缀</b>（决定该 wrapper 暴露的方法集合）：
 * <ul>
 *   <li>{@code *WhereLambdaQueryWrapper}    —— WHERE / HAVING 条件构造</li>
 *   <li>{@code *SelectLambdaQueryWrapper}   —— SELECT 字段选择</li>
 *   <li>{@code *SetLambdaQueryWrapper}      —— UPDATE SET 子句</li>
 *   <li>{@code *FunctionLambdaQueryWrapper} —— SQL 函数（COUNT/SUM/AVG/字符串/日期等）</li>
 *   <li>{@code *CaseLambdaQueryWrapper}     —— CASE WHEN 表达式</li>
 * </ul>
 *
 * <p>组合示例：{@code GroupWhereLambdaQueryWrapper} = "分组场景的 WHERE+HAVING 条件构造"，
 * 比 {@code NormalWhereLambdaQueryWrapper} 多了 HAVING 相关 API。
 *
 * <p><b>用户入口</b>（可直接 new）：
 * {@link com.baomidou.mybatisplus.extension.wrapper.JoinLambdaQueryWrapper},
 * {@link com.baomidou.mybatisplus.extension.wrapper.GroupLambdaQueryWrapper},
 * {@link com.baomidou.mybatisplus.extension.wrapper.SelectLambdaQueryWrapper},
 * {@link com.baomidou.mybatisplus.extension.wrapper.OrderLambdaQueryWrapper}。
 *
 * <p>其他 Normal/Group/Duplicate 系列通常作为 lambda 参数类型出现在
 * {@code IStreamService} 的方法签名里，用户在 {@code Consumer<...>} 体内使用。
 *
 * <p><b>新增维护提醒</b>：扩展一种条件能力时，先找对应的 {@code Abstract*} 基类；
 * 再决定要不要在 {@code Normal/Group/Duplicate/Sub} 变体中各补一份——
 * 这些子类的存在意义就是按"使用场景"隔离可调用方法集合。
 */
package com.baomidou.mybatisplus.extension.wrapper;
