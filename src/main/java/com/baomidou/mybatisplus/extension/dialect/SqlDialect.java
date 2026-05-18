package com.baomidou.mybatisplus.extension.dialect;

import com.baomidou.mybatisplus.extension.metadata.SqlDataType;

/**
 * 数据库方言 SPI。<b>给用户扩展的最小稳定接口</b>。
 *
 * <p>设计原则：
 * <ol>
 *   <li><b>只暴露方言间真正有差异的 SQL 片段</b>——分页、UPSERT、字符串拼接、聚合字符串、CAST、行锁、标识符引用</li>
 *   <li>默认实现 {@link com.baomidou.mybatisplus.extension.dialect.impl.MySqlDialect} 保证 3.x → 4.0 用户无感升级</li>
 *   <li>用户可通过 {@link java.util.ServiceLoader} 注册自定义方言 —— 在用户 jar 的
 *       {@code META-INF/services/com.baomidou.mybatisplus.extension.dialect.SqlDialect} 中
 *       声明实现类即可，无需修改本库代码</li>
 * </ol>
 *
 * <p>查询使用当前方言时通过 {@link DialectRegistry#current()} 获取。
 */
public interface SqlDialect {

    /** 本方言对应的数据库类型枚举 */
    DbType dbType();

    /* ============== 分页 ============== */

    /**
     * 给已有 SQL 追加分页子句。
     * <ul>
     *   <li>MySQL: {@code sql + " LIMIT offset, limit"}</li>
     *   <li>PostgreSQL / DM: {@code sql + " LIMIT limit OFFSET offset"}</li>
     * </ul>
     *
     * @param baseSql 已构造的查询主体（不含 LIMIT/OFFSET）
     * @param offset  跳过行数，&gt;= 0
     * @param limit   返回行数上限，&gt; 0
     */
    String paginate(String baseSql, long offset, long limit);

    /* ============== 字符串与函数 ============== */

    /**
     * 字符串拼接表达式。
     * <ul>
     *   <li>MySQL: {@code CONCAT(a, b, c)}</li>
     *   <li>PG / DM: {@code (a || b || c)} 或 {@code CONCAT(a, b, c)}</li>
     * </ul>
     */
    String concat(String... parts);

    /**
     * 分组字符串聚合。
     * <ul>
     *   <li>MySQL: {@code GROUP_CONCAT(col SEPARATOR ',')}</li>
     *   <li>PG / DM: {@code STRING_AGG(col, ',')}</li>
     * </ul>
     */
    String groupConcat(String columnExpr, String separator);

    /**
     * 类型转换表达式。
     * <ul>
     *   <li>MySQL: {@code CONVERT(expr, SIGNED)} 或 {@code CAST(expr AS SIGNED)}</li>
     *   <li>PG: {@code CAST(expr AS BIGINT)}</li>
     *   <li>DM: {@code CAST(expr AS INTEGER)}</li>
     * </ul>
     */
    String cast(String expr, SqlDataType type);

    /**
     * 把通用 {@link SqlDataType} 映射到本方言的实际类型名（用于 cast / DDL）。
     * 实现示例：MySQL 把 {@code SIGNED} 直接返回 {@code "SIGNED"}；PG 返回 {@code "BIGINT"}。
     */
    String dataTypeName(SqlDataType type);

    /* ============== 行锁 ============== */

    /**
     * 行锁子句。
     *
     * @param mode    锁模式
     * @param waitSeconds 仅在 mode == {@link LockMode#WAIT} 时使用；其他模式忽略
     */
    String forUpdate(LockMode mode, int waitSeconds);

    default String forUpdate(LockMode mode) {
        return forUpdate(mode, 0);
    }

    /* ============== 标识符 ============== */

    /**
     * 用方言要求的引号包裹标识符（表名、列名、别名）。
     * <ul>
     *   <li>MySQL: {@code `name`}</li>
     *   <li>PG / DM: {@code "name"}</li>
     * </ul>
     */
    String quoteIdentifier(String name);

    /* ============== UPSERT（4.0 仅声明，MySQL 实现；4.0.1 起 PG/DM 完整实现）============== */

    /**
     * 是否原生支持类似 MySQL 的 {@code ON DUPLICATE KEY UPDATE}。
     * <p>未支持时 {@link com.baomidou.mybatisplus.extension.service.IStreamService#saveDuplicate} 应
     * fail-fast 抛 {@link UnsupportedOperationException}（或由 dialect 通过 MERGE / ON CONFLICT 替换实现）。
     */
    default boolean supportsUpsert() {
        return true;
    }

    /**
     * 是否原生支持 {@code INSERT IGNORE} 语义。
     */
    default boolean supportsInsertIgnore() {
        return true;
    }

    /**
     * 是否原生支持 {@code REPLACE INTO} 语义。
     */
    default boolean supportsInsertReplace() {
        return true;
    }
}
