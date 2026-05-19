/**
 * 单列投影时 ResultSet → Java 值的容器。
 *
 * <p>当 stream 投影只有一列且类型已知时，用具体的 {@code Single*Value} 拿到强类型值
 * （比 {@code Object} 安全、比 {@code Map<String,Object>} 简洁）。
 *
 * <ul>
 *   <li>{@link com.baomidou.mybatisplus.extension.value.SingleValue} —— 泛型基类</li>
 *   <li>{@code SingleStringValue / SingleLongValue / SingleIntegerValue /
 *       SingleBooleanValue / SingleDateValue} —— 各 JDBC 类型特化</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.value.NonValue} —— 无值占位
 *       （用于聚合查询无结果的兜底）</li>
 * </ul>
 */
package com.baomidou.mybatisplus.extension.value;
