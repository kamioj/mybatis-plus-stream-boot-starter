/**
 * 多元函数式接口——弥补 JDK {@code java.util.function} 只有 0/1/2 元的不足。
 *
 * <ul>
 *   <li>{@code Function3..15} —— 3 到 15 元的函数（接收 N 个参数，返回 1 个值）</li>
 *   <li>{@code Consumer3..10} —— 3 到 10 元的消费者（接收 N 个参数，无返回值）</li>
 * </ul>
 *
 * <p>主要用于 {@code MybatisQueryableStreamN}（投影 N 列）的 {@code .map(...)}
 * 操作，让用户能用强类型 lambda 把 N 个列值组合成一个 DTO。
 *
 * <p><b>arity 维护</b>：新增 arity 时按现有模板复制即可，
 * 详见 {@code dev-docs/ARITY-TEMPLATE.md}。
 */
package com.baomidou.mybatisplus.extension.bo.functional;
