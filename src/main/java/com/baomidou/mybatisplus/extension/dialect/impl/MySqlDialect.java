package com.baomidou.mybatisplus.extension.dialect.impl;

import com.baomidou.mybatisplus.extension.dialect.DbType;
import com.baomidou.mybatisplus.extension.dialect.LockMode;
import com.baomidou.mybatisplus.extension.dialect.SqlDialect;
import com.baomidou.mybatisplus.extension.metadata.SqlDataType;

/**
 * MySQL / MariaDB 方言。<b>4.0 默认实现</b>。
 *
 * <p>所有 SQL 片段与 3.x 行为保持一致，保证 3.x → 4.0 用户无感升级。
 * 其他方言（PostgreSQL / DM / Oracle 等）建议继承本类后覆写差异方法。
 */
public class MySqlDialect implements SqlDialect {

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
    public String cast(String expr, SqlDataType type) {
        // MySQL 习惯用 CONVERT(expr, TYPE)
        return "CONVERT(" + expr + ", " + dataTypeName(type) + ")";
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

    private static String escapeStringLiteral(String s) {
        return s.replace("\\", "\\\\").replace("'", "''");
    }
}
