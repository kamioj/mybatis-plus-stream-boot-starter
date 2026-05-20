package com.baomidou.mybatisplus.extension.dialect.impl;

import com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper;
import com.baomidou.mybatisplus.extension.dialect.AbstractSqlDialect;
import com.baomidou.mybatisplus.extension.dialect.MergeIntoContext;
import com.baomidou.mybatisplus.extension.dialect.SetterClause;
import com.baomidou.mybatisplus.extension.dialect.DialectQuoteTranslator;
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
 *   <li>聚合字符串用 {@code LISTAGG}（DM 8 验证基线支持）</li>
 *   <li><b>UPSERT / INSERT IGNORE / REPLACE 统一用 {@code MERGE INTO}</b>（4.0.3 起完整支持）</li>
 *   <li>行锁支持 {@code FOR UPDATE / NOWAIT / WAIT n}（Oracle 兼容）</li>
 *   <li>标识符引用：双引号</li>
 * </ul>
 */
public class DamengDialect extends AbstractSqlDialect {

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
            return "LISTAGG(" + columnExpr + ", ',')";
        }
        return "LISTAGG(" + columnExpr + ", '" + separator.replace("'", "''") + "')";
    }

    @Override
    public String regexp(String leftExpr, String rightExpr) {
        return "REGEXP_LIKE(" + leftExpr + ", " + rightExpr + ")";
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
    public String conflictClause(WriteMode mode, List<SetterClause> setters, ColumnInfo pkColumn, String[] allColumns) {
        return "";  // INSERT 模式无冲突子句；其他模式走 MERGE 不会进这里
    }

    @Override
    public String incomingColumnRef(String bareColumn) {
        // DM MERGE 取插入行新值：src."col"
        return "src." + quoteIdentifier(bareColumn);
    }

    @Override
    public String updateSetTarget(String tableQualifier, String bareColumn) {
        // DM（Oracle 风格）UPDATE SET 目标用裸列。返回 BACKTICK token。
        return "`" + bareColumn + "`";
    }

    @Override
    public String buildMergeIntoScript(MergeIntoContext ctx) {
        String[] columns = ctx.getColumns();
        Object[][] values = ctx.getValues();
        Object[] flatValues = ctx.getFlatValues();
        ExecutableQueryWrapper<?> wrapper = ctx.getWrapper();
        WriteMode mode = wrapper.getWriteMode();
        ColumnInfo pk = wrapper.getFromTableInfo() == null ? null : wrapper.getFromTableInfo().getKeyColumn();
        if (pk == null) {
            throw new IllegalStateException("DM " + mode + " 需要实体声明 @TableId 主键（MERGE INTO ON 子句必需）");
        }
        String pkColQuoted = quoteIdentifier(pk.getColumnName());
        String tableExpr = wrapper.getSqlFrom();
        List<SetterClause> setters = wrapper.getSetters();

        StringBuilder sb = new StringBuilder();
        sb.append("<script>\n");
        // Phase 4 评审修正：MERGE 的 target 不起别名，直接用表名。
        // 这样 valueExpr 里按表名限定的列引用（如累加 col = col + delta 的右侧）在 MERGE 内能正确解析。
        sb.append("MERGE INTO ").append(tableExpr).append("\n");

        // USING (SELECT v1 col1, v2 col2 FROM DUAL UNION ALL SELECT ...) src
        sb.append("USING (\n");
        int valueIndex = 0;
        for (int row = 0; row < values.length; row++) {
            if (row > 0) {
                sb.append(" UNION ALL\n");
            }
            sb.append("    SELECT ");
            for (int col = 0; col < columns.length; col++) {
                if (col > 0) sb.append(", ");
                sb.append("#{flatValues[").append(valueIndex++).append("]} AS ").append(columns[col]);
            }
            sb.append(" FROM DUAL\n");
        }
        sb.append(") src ON (").append(tableExpr).append(".").append(pkColQuoted)
            .append(" = src.").append(pkColQuoted).append(")\n");

        // WHEN MATCHED 子句（DUPLICATE / REPLACE 才需要；IGNORE 不要）
        if (mode == WriteMode.DUPLICATE) {
            if (setters != null && !setters.isEmpty()) {
                sb.append("WHEN MATCHED THEN UPDATE SET ");
                for (int i = 0; i < setters.size(); i++) {
                    if (i > 0) sb.append(", ");
                    SetterClause s = setters.get(i);
                    // SET 目标用裸列（MERGE WHEN MATCHED 的 SET 列即目标表列，无歧义）；
                    // valueExpr 含 BACKTICK / INCOMING token，本路径不经 getSqlConflictClause 的
                    // translate，故在此显式翻译为 DM 形态
                    sb.append(quoteIdentifier(s.getTargetColumn()))
                      .append(" = ")
                      .append(DialectQuoteTranslator.translate(s.getValueExpr(), this));
                }
                sb.append("\n");
            }
            // setters 为空时退化为：行已存在不更新（等价 IGNORE 行为，但用户主动选了 DUPLICATE 模式，行为安全）
        } else if (mode == WriteMode.REPLACE) {
            // 全列覆盖（除 PK 外，PK 列在 ON 子句已对齐）
            sb.append("WHEN MATCHED THEN UPDATE SET ");
            boolean first = true;
            for (String col : columns) {
                if (col.equals(pkColQuoted)) continue;  // 跳过 PK
                if (!first) sb.append(", ");
                sb.append(col).append(" = src.").append(col);
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
