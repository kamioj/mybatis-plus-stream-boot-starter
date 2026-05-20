package com.baomidou.mybatisplus.extension.dialect.impl;

import com.baomidou.mybatisplus.extension.dialect.AbstractSqlDialect;
import com.baomidou.mybatisplus.extension.dialect.SetterClause;
import com.baomidou.mybatisplus.extension.dialect.DbType;
import com.baomidou.mybatisplus.extension.dialect.LockMode;
import com.baomidou.mybatisplus.extension.dialect.WriteMode;
import com.baomidou.mybatisplus.extension.metadata.ColumnInfo;
import com.baomidou.mybatisplus.extension.metadata.SqlDataType;

import java.util.List;

/**
 * MySQL / MariaDB 方言。<b>4.0 默认实现</b>。
 *
 * <p>所有 SQL 片段与 3.x 行为保持一致，保证 3.x → 4.0 用户无感升级。
 * 其他方言（PostgreSQL / DM / Oracle 等）应继承 {@link AbstractSqlDialect}，
 * <b>不要</b>继承本类——避免静默继承 MySQL 的 SQL 写法。
 */
public class MySqlDialect extends AbstractSqlDialect {

    @Override
    public DbType dbType() {
        return DbType.MYSQL;
    }

    @Override
    public String paginate(String baseSql, long offset, long limit) {
        // MySQL: LIMIT offset, count
        if (offset > 0) {
            return baseSql + " LIMIT " + offset + ", " + limit;
        }
        return baseSql + " LIMIT " + limit;
    }

    @Override
    public String concat(String... parts) {
        if (parts == null || parts.length == 0) return "''";
        StringBuilder sb = new StringBuilder("CONCAT(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(parts[i]);
        }
        return sb.append(')').toString();
    }

    @Override
    public String groupConcat(String columnExpr, String separator) {
        if (separator == null) {
            return "GROUP_CONCAT(" + columnExpr + ")";
        }
        return "GROUP_CONCAT(" + columnExpr + " SEPARATOR '" + escapeStringLiteral(separator) + "')";
    }

    @Override
    public String regexp(String leftExpr, String rightExpr) {
        // MySQL 支持中缀 REGEXP
        return leftExpr + " REGEXP " + rightExpr;
    }

    @Override
    public String cast(String expr, SqlDataType type) {
        // MySQL 习惯用 CONVERT(expr, TYPE)
        return "CONVERT(" + expr + ", " + dataTypeName(type) + ")";
    }

    @Override
    public String convertCharset(String expr, String charset) {
        // MySQL 字符集转换：CONVERT(expr USING charset)
        return "CONVERT(" + expr + " USING " + charset + ")";
    }

    @Override
    public String dataTypeName(SqlDataType type) {
        // MySQL 直接用枚举名（DATE / DATETIME / TIME / CHAR / SIGNED / UNSIGNED / BINARY）
        return type.name();
    }

    @Override
    public String forUpdate(LockMode mode, int waitSeconds) {
        return switch (mode) {
            case FOR_UPDATE   -> " FOR UPDATE";
            case NOWAIT       -> " FOR UPDATE NOWAIT";           // MySQL 8.0+
            case SKIP_LOCKED  -> " FOR UPDATE SKIP LOCKED";      // MySQL 8.0+
            case WAIT         -> throw new UnsupportedOperationException(
                "MySQL does not support FOR UPDATE WAIT n. Consider NOWAIT or SKIP_LOCKED.");
        };
    }

    @Override
    public String quoteIdentifier(String name) {
        // MySQL 用反引号
        return "`" + name.replace("`", "``") + "`";
    }

    @Override
    public String insertPrefix(WriteMode mode) {
        return switch (mode) {
            case INSERT, DUPLICATE -> "INSERT INTO";
            case IGNORE            -> "INSERT IGNORE INTO";
            case REPLACE           -> "REPLACE INTO";
        };
    }

    @Override
    public String conflictClause(WriteMode mode, List<SetterClause> setters, ColumnInfo pkColumn, String[] allColumns) {
        if (mode == WriteMode.DUPLICATE) {
            if (setters == null || setters.isEmpty()) return "";
            StringBuilder sb = new StringBuilder("\nON DUPLICATE KEY UPDATE\n");
            for (int i = 0; i < setters.size(); i++) {
                if (i > 0) sb.append(",\n");
                SetterClause s = setters.get(i);
                // 目标列用 BACKTICK token；冲突子句为单表，忽略 tableQualifier
                sb.append('`').append(s.getTargetColumn()).append('`')
                  .append(" = ").append(s.getValueExpr());
            }
            return sb.toString();
        }
        // IGNORE / REPLACE 的语义已在 insertPrefix 表达；末尾不需要子句
        return "";
    }

    @Override
    public String incomingColumnRef(String bareColumn) {
        // MySQL upsert 取插入行新值：VALUES(`col`)
        return "VALUES(" + quoteIdentifier(bareColumn) + ")";
    }

    @Override
    public String updateSetTarget(String tableQualifier, String bareColumn) {
        // MySQL 多表 UPDATE 需表限定符消歧；qualifier 空则裸列。返回 BACKTICK token。
        if (tableQualifier == null || tableQualifier.isEmpty()) {
            return "`" + bareColumn + "`";
        }
        return "`" + tableQualifier + "`.`" + bareColumn + "`";
    }

    private static String escapeStringLiteral(String s) {
        return s.replace("\\", "\\\\").replace("'", "''");
    }
}
