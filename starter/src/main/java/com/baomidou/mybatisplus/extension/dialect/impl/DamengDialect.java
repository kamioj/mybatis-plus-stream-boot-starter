package com.baomidou.mybatisplus.extension.dialect.impl;

import com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper;
import com.baomidou.mybatisplus.extension.dialect.DbType;
import com.baomidou.mybatisplus.extension.dialect.LockMode;
import com.baomidou.mybatisplus.extension.dialect.WriteMode;
import com.baomidou.mybatisplus.extension.metadata.ColumnInfo;
import com.baomidou.mybatisplus.extension.metadata.SqlDataType;

import java.util.List;

/**
 * 达梦数据库方言（验证基线：DM 8 / DmJdbcDriver18 8.1.3.x）。
 *
 * <p>DM 8 同时支持 <b>Oracle 兼容模式</b>（默认 COMPATIBLE_MODE=2）和 <b>MySQL 兼容模式</b>
 * （COMPATIBLE_MODE=4）。本方言策略：
 * <ul>
 *   <li><b>语法选择以 Oracle 优先</b>（DM 主推；功能更完整），降级时考虑 MySQL 兼容</li>
 *   <li>分页用 {@code LIMIT n OFFSET m}（DM 8 在两种模式下均支持）</li>
 *   <li>字符串拼接用 {@code ||}（Oracle 风格）</li>
 *   <li>聚合字符串用 {@code STRING_AGG}（DM 8 + 兼容 PG 风格）</li>
 *   <li><b>UPSERT / INSERT IGNORE / REPLACE 统一用 {@code MERGE INTO}</b>（4.0.3 起完整支持）</li>
 *   <li>行锁支持 {@code FOR UPDATE / NOWAIT / WAIT n}（Oracle 兼容）</li>
 *   <li>标识符引用：双引号</li>
 * </ul>
 */
public class DamengDialect extends MySqlDialect {

    @Override
    public DbType dbType() {
        return DbType.DAMENG;
    }

    @Override
    public String paginate(String baseSql, long offset, long limit) {
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
            return "STRING_AGG(" + columnExpr + ", ',')";
        }
        return "STRING_AGG(" + columnExpr + ", '" + separator.replace("'", "''") + "')";
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
            case CHAR      -> "VARCHAR";
            case SIGNED    -> "BIGINT";
            case UNSIGNED  -> "BIGINT";
            case BINARY    -> "BLOB";
        };
    }

    @Override
    public String forUpdate(LockMode mode, int waitSeconds) {
        return switch (mode) {
            case FOR_UPDATE   -> " FOR UPDATE";
            case NOWAIT       -> " FOR UPDATE NOWAIT";
            case SKIP_LOCKED  -> " FOR UPDATE SKIP LOCKED";
            case WAIT         -> " FOR UPDATE WAIT " + waitSeconds;
        };
    }

    @Override
    public String quoteIdentifier(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    /* ============== 4.0.3：DM 三种写入用 MERGE INTO 实现 ============== */

    @Override
    public boolean useMergeInto(WriteMode mode) {
        // DM 的 DUPLICATE / IGNORE / REPLACE 都要走 MERGE INTO（INSERT 仍走标准 INSERT 路径）
        return mode == WriteMode.DUPLICATE
            || mode == WriteMode.IGNORE
            || mode == WriteMode.REPLACE;
    }

    @Override
    public String insertPrefix(WriteMode mode) {
        // 仅 INSERT 模式会进入此方法（其他模式由 useMergeInto 重定向到 buildMergeIntoScript）
        return "INSERT INTO";
    }

    @Override
    public String conflictClause(WriteMode mode, List<String> setters, ColumnInfo pkColumn, String[] allColumns) {
        return "";  // INSERT 模式无冲突子句；其他模式走 MERGE 不会进这里
    }

    @Override
    public String buildMergeIntoScript(String[] columns, ExecutableQueryWrapper<?> wrapper) {
        WriteMode mode = wrapper.getWriteMode();
        ColumnInfo pk = wrapper.getFromTableInfo() == null ? null : wrapper.getFromTableInfo().getKeyColumn();
        if (pk == null) {
            throw new IllegalStateException("DM " + mode + " 需要实体声明 @TableId 主键（MERGE INTO ON 子句必需）");
        }
        String pkColQuoted = quoteIdentifier(pk.getColumnName());
        String tableExpr = wrapper.getSqlFrom();
        List<String> setters = wrapper.getSetters();

        StringBuilder sb = new StringBuilder();
        sb.append("<script>\n");
        sb.append("MERGE INTO ").append(tableExpr).append(" t\n");

        // USING (SELECT v1 col1, v2 col2 FROM DUAL UNION ALL SELECT ...) src
        sb.append("USING (\n");
        sb.append("  <foreach collection='values' item='item' separator=' UNION ALL '>\n");
        sb.append("    SELECT ");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("#{item[").append(i).append("]} AS ").append(columns[i]);
        }
        sb.append(" FROM DUAL\n");
        sb.append("  </foreach>\n");
        sb.append(") src ON (t.").append(pkColQuoted).append(" = src.").append(pkColQuoted).append(")\n");

        // WHEN MATCHED 子句（DUPLICATE / REPLACE 才需要；IGNORE 不要）
        if (mode == WriteMode.DUPLICATE) {
            if (setters != null && !setters.isEmpty()) {
                sb.append("WHEN MATCHED THEN UPDATE SET ").append(String.join(", ", setters)).append("\n");
            }
            // setters 为空时退化为：行已存在不更新（等价 IGNORE 行为，但用户主动选了 DUPLICATE 模式，行为安全）
        } else if (mode == WriteMode.REPLACE) {
            // 全列覆盖（除 PK 外，PK 列在 ON 子句已对齐）
            sb.append("WHEN MATCHED THEN UPDATE SET ");
            boolean first = true;
            for (String col : columns) {
                if (col.equals(pkColQuoted)) continue;  // 跳过 PK
                if (!first) sb.append(", ");
                sb.append("t.").append(col).append(" = src.").append(col);
                first = false;
            }
            sb.append("\n");
        }
        // IGNORE 模式不写 WHEN MATCHED；行已存在时什么都不做

        // WHEN NOT MATCHED THEN INSERT
        sb.append("WHEN NOT MATCHED THEN INSERT (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(columns[i]);
        }
        sb.append(") VALUES (");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("src.").append(columns[i]);
        }
        sb.append(")\n");
        sb.append("</script>");
        return sb.toString();
    }
}
