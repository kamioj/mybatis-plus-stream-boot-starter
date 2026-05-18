/**
 * 查询执行内核。
 *
 * <p>本包是 wrapper 家族与 stream 家族共同依赖的"底盘"，包含：
 * <ul>
 *   <li>{@link com.baomidou.mybatisplus.extension.core.ExQueryWrapper} —— 扩展自
 *       MyBatis-Plus {@code QueryWrapper}，附加 rename / joinSql / limit 等渲染能力，
 *       是所有 SELECT 语句的 wrapper 载体</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper} ——
 *       UPDATE/INSERT/DELETE 场景的 wrapper 载体</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.core.LambdaQueryWrapper} ——
 *       Lambda 类型安全 wrapper 的抽象基类（顶层入口）</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.core.Converter} ——
 *       类型转换器抽象基类</li>
 * </ul>
 *
 * <p>真正的 SELECT 字段 / FROM / JOIN / WHERE / GROUP BY / ORDER BY / LIMIT 由
 * wrapper 在运行时拼到 {@code customSqlFromSegment} / {@code customSqlSegment} /
 * {@code sqlSelect} 占位符中；StreamBaseMapper 上的注解 SQL 只是骨架。
 */
package com.baomidou.mybatisplus.extension.core;
