/**
 * 实体 / 表 / 列 / 存储过程参数等元数据载体。
 *
 * <ul>
 *   <li>{@link com.baomidou.mybatisplus.extension.metadata.ColumnInfo} —— 单列元数据
 *       （字段名、列名、JDBC 类型、主键标记）</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.metadata.TableInfo} —— 表元数据
 *       （表名、所有列、主键、逻辑删除列）</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.metadata.SqlDataType} —— MySQL
 *       数据类型枚举</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.metadata.ProcedureParam} /
 *       {@link com.baomidou.mybatisplus.extension.metadata.ProcedureParamDef} ——
 *       存储过程入参 / 出参定义</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.metadata.Struct} —— 复合结构体抽象</li>
 * </ul>
 *
 * <p>本包的类由 {@code com.baomidou.mybatisplus.toolkit.MybatisUtil} 通过反射填充，
 * 是整套 Lambda 类型安全 API 的元数据基础。
 */
package com.baomidou.mybatisplus.extension.metadata;
