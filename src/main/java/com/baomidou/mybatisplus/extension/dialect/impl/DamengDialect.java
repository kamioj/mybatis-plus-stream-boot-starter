package com.baomidou.mybatisplus.extension.dialect.impl;

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
 * （COMPATIBLE_MODE=4）。本方言采取的策略：
 * <ul>
 *   <li><b>语法选择以 Oracle 优先</b>（DM 主推；功能更完整），降级时考虑 MySQL 兼容</li>
 *   <li>分页用 {@code LIMIT n OFFSET m}（DM 8 在两种模式下均支持，避免 ROWNUM 嵌套子查询）</li>
 *   <li>字符串拼接用 {@code ||}（Oracle 风格；MySQL 兼容模式也支持）</li>
 *   <li>聚合字符串用 {@code STRING_AGG}（DM 8 + 兼容 PG 风格；Oracle 风格 LISTAGG 在子集模式不全支持）</li>
 *   <li>UPSERT 用 {@code MERGE INTO}（Oracle 风格；DM 不支持 ON DUPLICATE KEY 也不支持 ON CONFLICT）</li>
 *   <li>不支持 INSERT IGNORE / REPLACE INTO 原生</li>
 *   <li>行锁支持 FOR UPDATE / NOWAIT / <b>WAIT n</b>（Oracle 兼容；MySQL 没有 WAIT n）；SKIP LOCKED 取决于 DM 版本</li>
 *   <li>标识符引用：双引号（Oracle 风格）</li>
 * </ul>
 */
public class DamengDialect extends MySqlDialect {

    @Override
    public DbType dbType() {
        return DbType.DAMENG;
    }

    @Override
    public String paginate(String baseSql, long offset, long limit) {
        // DM 8 在 Oracle 和 MySQL 兼容模式都支持 LIMIT n OFFSET m，避免 ROWNUM 嵌套
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
            case CHAR      -> "VARCHAR";       // DM 推荐 VARCHAR
            case SIGNED    -> "BIGINT";
            case UNSIGNED  -> "BIGINT";        // DM 无 unsigned 概念，落回 BIGINT
            case BINARY    -> "BLOB";          // DM 二进制用 BLOB
        };
    }

    @Override
    public String forUpdate(LockMode mode, int waitSeconds) {
        return switch (mode) {
            case FOR_UPDATE   -> " FOR UPDATE";
            case NOWAIT       -> " FOR UPDATE NOWAIT";
            case SKIP_LOCKED  -> " FOR UPDATE SKIP LOCKED";  // DM 较新版本支持，旧版可能报错
            case WAIT         -> " FOR UPDATE WAIT " + waitSeconds;  // DM 独有（Oracle 兼容）
        };
    }

    @Override
    public String quoteIdentifier(String name) {
        // DM 默认 Oracle 兼容用双引号；双引号包裹的标识符大小写敏感
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String insertPrefix(WriteMode mode) {
        if (mode == WriteMode.INSERT) {
            return "INSERT INTO";
        }
        // DM saveDuplicate/saveIgnore/saveReplace 必须用 MERGE INTO 完全不同的语句结构；
        // 4.0.2 暂时 fail-fast；4.0.3 起通过 @InsertProvider 走 MERGE INTO 完整实现
        throw new UnsupportedOperationException(
            "DM dialect 暂不支持 " + mode + " 模式的批量写入（4.0.2）。" +
            "DM 的 MERGE INTO 语句结构与 INSERT 完全不同，需通过 @InsertProvider 重构，" +
            "规划在 4.0.3 提供。临时方案：" +
            "1) 切到 MySQL 兼容模式调用方+设 DbType.MYSQL；" +
            "2) 或自行执行原生 MERGE INTO SQL。");
    }

    @Override
    public String conflictClause(WriteMode mode, List<String> setters, ColumnInfo pkColumn, String[] allColumns) {
        if (mode == WriteMode.INSERT) {
            return "";
        }
        throw new UnsupportedOperationException(
            "DM dialect 暂不支持 " + mode + " 写入路径（4.0.2）。规划 4.0.3 通过 MERGE INTO 完整实现。");
    }
}
