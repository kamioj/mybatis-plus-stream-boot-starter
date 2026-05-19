package com.baomidou.mybatisplus.extension.dialect;

/**
 * BACKTICK token 适配器：把 wrapper 内部生成的 {@code `name`} 风格 SQL 片段
 * 翻译为当前方言的标识符引号（MySQL: 反引号；PG/DM: 双引号；用户自定义方言：
 * 调 {@link SqlDialect#quoteIdentifier(String)}）。
 *
 * <h3>设计哲学（4.1 引入）</h3>
 * <p>{@code BACKTICK} 在本库 wrapper 内部有<b>双重角色</b>：
 * <ol>
 *   <li>SQL 标识符引号（最终输出到数据库）</li>
 *   <li>wrapper 内部 keySet/列名 token 分隔符（不出现在最终 SQL）</li>
 * </ol>
 *
 * <p>4.0 在写入路径已让 dialect 直接控制引号（{@link SqlDialect#quoteIdentifier(String)}）。
 * 但 wrapper 内部协议（如 {@code ExQueryWrapper.keySet} 用反引号作 token 边界）若也
 * 改为方言引号，会破坏 18+ 个地方的字符串匹配协议。
 *
 * <p>所以本类作为<b>渲染入口的最后一公里适配器</b>：
 * <ul>
 *   <li>wrapper 内部继续用 BACKTICK 做 token（保持现有逻辑、零回归）</li>
 *   <li>每个 SQL 渲染 getter 在 return 前调本类 {@link #translate(String)} 统一翻译</li>
 *   <li>MySQL 方言下 translator 是 no-op（反引号原样保留）</li>
 *   <li>PG/DM/用户自定义方言下 translator 把 {@code `x`} 换成 {@code dialect.quoteIdentifier("x")}</li>
 * </ul>
 *
 * <h3>转义规则</h3>
 * <ul>
 *   <li>SQL 单引号字符串字面值（{@code '...'}）内的反引号不被翻译</li>
 *   <li>不成对的反引号保留原样（视为内部 token 残留，输出可能在 MySQL 下仍合法）</li>
 * </ul>
 */
public final class DialectQuoteTranslator {

    private DialectQuoteTranslator() {}

    /**
     * 把 BACKTICK 风格的 SQL 片段翻译为当前方言引号。
     * <p>MySQL 方言时直接返回原字符串（最快路径）。
     */
    public static String translate(String rawSql) {
        if (rawSql == null || rawSql.isEmpty() || rawSql.indexOf('`') < 0) {
            return rawSql;
        }
        SqlDialect dialect = DialectRegistry.current();
        return translate(rawSql, dialect);
    }

    /**
     * 显式 dialect 版本（用于测试 / 双向兼容）。
     */
    public static String translate(String rawSql, SqlDialect dialect) {
        if (rawSql == null || rawSql.isEmpty() || rawSql.indexOf('`') < 0) {
            return rawSql;
        }
        // MySQL 方言下反引号就是正确引号，no-op
        if (dialect.dbType() == DbType.MYSQL) {
            return rawSql;
        }
        StringBuilder out = new StringBuilder(rawSql.length() + 16);
        boolean inLiteral = false;
        int len = rawSql.length();
        int i = 0;
        while (i < len) {
            char c = rawSql.charAt(i);
            if (c == '\'') {
                // 进入或退出 SQL 字符串字面值；处理 SQL 标准的 '' 转义（连续两个单引号代表一个）
                if (inLiteral && i + 1 < len && rawSql.charAt(i + 1) == '\'') {
                    out.append("''");
                    i += 2;
                    continue;
                }
                inLiteral = !inLiteral;
                out.append(c);
                i++;
            } else if (c == '`' && !inLiteral) {
                // 寻找配对的闭合反引号（最近一个未转义的）
                int close = rawSql.indexOf('`', i + 1);
                if (close < 0) {
                    // 不成对，原样输出避免破坏
                    out.append(c);
                    i++;
                } else {
                    String bare = rawSql.substring(i + 1, close);
                    out.append(dialect.quoteIdentifier(bare));
                    i = close + 1;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
