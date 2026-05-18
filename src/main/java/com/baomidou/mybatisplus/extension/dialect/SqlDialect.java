package com.baomidou.mybatisplus.extension.dialect;

import com.baomidou.mybatisplus.extension.metadata.ColumnInfo;
import com.baomidou.mybatisplus.extension.metadata.SqlDataType;

import java.util.List;

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

    /* ============== 批量写入路径（4.0.2 起方言接管）============== */

    /**
     * 返回 INSERT 语句的<b>动词前缀</b>。
     * <ul>
     *   <li>MySQL INSERT/DUPLICATE: {@code "INSERT INTO"}；IGNORE: {@code "INSERT IGNORE INTO"}；REPLACE: {@code "REPLACE INTO"}</li>
     *   <li>PG 三种 mode 都是: {@code "INSERT INTO"}（用末尾子句区分语义）</li>
     *   <li>DM 用 MERGE INTO 完全不同结构，4.0.2 在 DUPLICATE/IGNORE/REPLACE 三种 mode 下都
     *       throw {@link UnsupportedOperationException}（4.0.3 起完整实现）</li>
     * </ul>
     */
    String insertPrefix(WriteMode mode);

    /**
     * 返回 INSERT 语句末尾的<b>冲突处理子句</b>（含前导空格/换行；INSERT 模式返回空串）。
     *
     * @param mode      写入模式
     * @param setters   用户通过 {@code DuplicateSetLambdaQueryWrapper.set(...)} 收集的赋值表达式
     *                  （形如 {@code "update_time = #{ew.paramNameValuePairs.X}"}），仅 {@link WriteMode#DUPLICATE} 用到
     * @param pkColumn  实体主键列（PG ON CONFLICT 子句必需），null 表示未声明 PK
     * @param allColumns 表的所有列（{@link WriteMode#REPLACE} 全列覆盖语义所需）
     */
    String conflictClause(WriteMode mode, List<String> setters, ColumnInfo pkColumn, String[] allColumns);

    /* ============== MERGE INTO 路径（4.0.3 引入，DM 专用）============== */

    /**
     * 是否使用 {@code MERGE INTO} 完整不同的语句结构（非 "INSERT + 末尾子句" 风格）。
     * 默认返回 false（MySQL / PG 都用 INSERT 风格）；DM 在 DUPLICATE/IGNORE/REPLACE 三种模式下返回 true。
     */
    default boolean useMergeInto(WriteMode mode) {
        return false;
    }

    /**
     * 当 {@link #useMergeInto(WriteMode)} 返回 true 时，生成完整的 {@code <script>...</script>} 字符串
     * 供 {@code @InsertProvider} 渲染。
     *
     * @param columns      列名数组（已带方言引号）
     * @param wrapper      执行 wrapper（含表名、PK、setters、writeMode 等元数据）
     * @return MyBatis {@code <script>} 形式的 SQL 字符串
     */
    default String buildMergeIntoScript(String[] columns, com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper<?> wrapper) {
        throw new UnsupportedOperationException("Dialect " + dbType() + " 不需要 MERGE INTO 路径");
    }
}
