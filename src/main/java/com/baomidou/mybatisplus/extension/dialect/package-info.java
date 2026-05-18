/**
 * SQL 方言 SPI。<b>4.0 引入</b>。
 *
 * <p>本包定义了让 {@code mybatis-plus-stream} 支持多种数据库（MySQL / PostgreSQL / DM / 用户自定义）的扩展点。
 *
 * <h3>用户如何切换内置方言</h3>
 * <pre>{@code
 * // 应用启动时
 * DialectRegistry.use(DbType.POSTGRESQL);
 * }</pre>
 *
 * <h3>用户如何注册自定义方言</h3>
 * <ol>
 *   <li>实现 {@link com.baomidou.mybatisplus.extension.dialect.SqlDialect}
 *       （建议继承 {@link com.baomidou.mybatisplus.extension.dialect.impl.MySqlDialect} 只覆写差异方法）</li>
 *   <li>在 jar 的 {@code META-INF/services/com.baomidou.mybatisplus.extension.dialect.SqlDialect}
 *       中写一行：你的实现类全限定名</li>
 *   <li>启动时调 {@link com.baomidou.mybatisplus.extension.dialect.DialectRegistry#use} 切换</li>
 * </ol>
 *
 * <p>本库自身不依赖任何 JDBC 驱动 —— 方言只负责拼 SQL 字符串，让用户自由选择驱动。
 *
 * <h3>4.0 现状</h3>
 * <ul>
 *   <li>{@link com.baomidou.mybatisplus.extension.dialect.impl.MySqlDialect}：默认实现，行为与 3.x 完全一致</li>
 *   <li>PostgreSQL、DM 方言：规划 4.0.1 内置</li>
 * </ul>
 */
package com.baomidou.mybatisplus.extension.dialect;
