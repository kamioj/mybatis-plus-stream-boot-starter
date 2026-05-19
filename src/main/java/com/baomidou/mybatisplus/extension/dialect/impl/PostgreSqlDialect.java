package com.baomidou.mybatisplus.extension.dialect.impl;

import com.baomidou.mybatisplus.extension.dialect.DbType;
import com.baomidou.mybatisplus.extension.dialect.LockMode;
import com.baomidou.mybatisplus.extension.dialect.WriteMode;
import com.baomidou.mybatisplus.extension.metadata.ColumnInfo;
import com.baomidou.mybatisplus.extension.metadata.SqlDataType;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreSQL 方言（验证基线：PG 17 LTS）。
 *
 * <p>本类继承 {@link MySqlDialect} 仅覆写差异方法，便于审计两方言间的 SQL 分歧。
 *
 * <h3>关键差异（vs MySQL）</h3>
 * <ul>
 *   <li>分页：{@code LIMIT n OFFSET m}（注意语序与 MySQL 相反）</li>
 *   <li>字符串拼接：{@code (a || b || c)}（PG 原生操作符；{@code CONCAT(...)} 也支持但少见）</li>
 *   <li>分组聚合字符串：{@code STRING_AGG(col, sep)}（无 SEPARATOR 关键字）</li>
 *   <li>类型转换：{@code CAST(expr AS type)} 或 {@code expr::type}（这里用 CAST 风格保持可读）</li>
 *   <li>SQL 类型名差异：{@code SIGNED → BIGINT}、{@code UNSIGNED → BIGINT}（PG 无 unsigned）、
 *       {@code BINARY → BYTEA}、{@code DATETIME → TIMESTAMP}</li>
 *   <li>行锁：支持 NOWAIT / SKIP LOCKED，不支持 WAIT n（同 MySQL 8）</li>
 *   <li>标识符引用：双引号（区分大小写！）</li>
 *   <li>UPSERT：{@code ON CONFLICT(pk) DO UPDATE SET ...}（原生）</li>
 *   <li>INSERT IGNORE 等价：{@code ON CONFLICT DO NOTHING}（原生）</li>
 *   <li>REPLACE INTO：无原生等价；需 DELETE+INSERT 或 ON CONFLICT DO UPDATE 模拟</li>
 * </ul>
 */
public class PostgreSqlDialect extends MySqlDialect {

    @Override
    public DbType dbType() {
        return DbType.POSTGRESQL;
    }

    @Override
    public String paginate(String baseSql, long offset, long limit) {
        // PG: LIMIT n OFFSET m（注意 LIMIT 在前）
        if (offset > 0) {
            return baseSql + " LIMIT " + limit + " OFFSET " + offset;
        }
        return baseSql + " LIMIT " + limit;
    }

    @Override
    public String concat(String... parts) {
        if (parts == null || parts.length == 0) return "''";
        if (parts.length == 1) return parts[0];
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(" || ");
            sb.append(parts[i]);
        }
        return sb.append(')').toString();
    }

    @Override
    public String groupConcat(String columnExpr, String separator) {
        if (separator == null) {
            return "STRING_AGG(" + columnExpr + "::text, ',')";
        }
        // PG 的 STRING_AGG 第一参必须是 text；强制 cast 避免类型不匹配
        return "STRING_AGG(" + columnExpr + "::text, '" + separator.replace("'", "''") + "')";
    }

    @Override
    public String cast(String expr, SqlDataType type) {
        return "CAST(" + expr + " AS " + dataTypeName(type) + ")";
    }

    @Override
    public String dataTypeName(SqlDataType type) {
        return switch (type) {
            case DATE      -> "DATE";
            case DATETIME  -> "TIMESTAMP";
            case TIME      -> "TIME";
            case CHAR      -> "TEXT";       // PG 推荐 TEXT 而非 CHAR
            case SIGNED    -> "BIGINT";
            case UNSIGNED  -> "BIGINT";     // PG 无 unsigned，落回 BIGINT
            case BINARY    -> "BYTEA";
        };
    }

    @Override
    public String forUpdate(LockMode mode, int waitSeconds) {
        return switch (mode) {
            case FOR_UPDATE   -> " FOR UPDATE";
            case NOWAIT       -> " FOR UPDATE NOWAIT";
            case SKIP_LOCKED  -> " FOR UPDATE SKIP LOCKED";
            case WAIT         -> throw new UnsupportedOperationException(
                "PostgreSQL does not support FOR UPDATE WAIT n. Use NOWAIT or SKIP_LOCKED.");
        };
    }

    @Override
    public String quoteIdentifier(String name) {
        // PG 用双引号；注意：双引号包裹会让标识符变得大小写敏感
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String insertPrefix(WriteMode mode) {
        // PG 三种 mode 都用 INSERT INTO，差异在末尾子句
        return "INSERT INTO";
    }

    @Override
    public String conflictClause(WriteMode mode, List<String> setters, ColumnInfo pkColumn, String[] allColumns) {
        return switch (mode) {
            case INSERT -> "";
            case DUPLICATE -> {
                if (setters == null || setters.isEmpty()) yield "";
                if (pkColumn == null) {
                    throw new IllegalStateException(
                        "PostgreSQL saveDuplicate 需要实体声明 @TableId 主键（ON CONFLICT 子句必需）。");
                }
                String pk = quoteIdentifier(pkColumn.getColumnName());
                // PG SET 子句不允许列名带表名前缀（"tbl"."col" → "col"）
                StringBuilder sb = new StringBuilder("\nON CONFLICT (").append(pk).append(") DO UPDATE SET\n");
                for (int i = 0; i < setters.size(); i++) {
                    if (i > 0) sb.append(",\n");
                    sb.append(stripTablePrefix(setters.get(i)));
                }
                yield sb.toString();
            }
            case IGNORE ->
                // PG 等价 INSERT IGNORE：行已存在时不动
                "\nON CONFLICT DO NOTHING";
            case REPLACE -> {
                if (pkColumn == null) {
                    throw new IllegalStateException(
                        "PostgreSQL saveReplace 需要实体声明 @TableId 主键（ON CONFLICT 子句必需）。");
                }
                if (allColumns == null || allColumns.length == 0) {
                    yield "\nON CONFLICT DO NOTHING";
                }
                String pk = quoteIdentifier(pkColumn.getColumnName());
                StringBuilder sb = new StringBuilder("\nON CONFLICT (").append(pk).append(") DO UPDATE SET\n");
                boolean first = true;
                for (String col : allColumns) {
                    // allColumns 元素可能已带 dialect 引号；统一抽出"裸列名"再加 EXCLUDED.<裸列>
                    String bare = stripQuotes(col);
                    String quoted = quoteIdentifier(bare);
                    if (!first) sb.append(",\n");
                    sb.append(quoted).append(" = EXCLUDED.").append(quoted);
                    first = false;
                }
                yield sb.toString();
            }
        };
    }

    private static final Pattern UNQUOTE = Pattern.compile("^[\"`]?(.*?)[\"`]?$");
    private static final Pattern TABLE_PREFIX = Pattern.compile("^[\"`][^\"`]+[\"`]\\.");

    private static String stripQuotes(String identifier) {
        Matcher m = UNQUOTE.matcher(identifier);
        return m.matches() ? m.group(1) : identifier;
    }

    /**
     * 4.1.1: PG ON CONFLICT DO UPDATE SET 子句不允许列名带表名前缀。
     * 把 setter 形如 {@code "tbl"."col"=#{val}} 或 {@code `tbl`.`col`=#{val}} 中的表前缀剥掉。
     */
    private static String stripTablePrefix(String setter) {
        return TABLE_PREFIX.matcher(setter).replaceFirst("");
    }
}
