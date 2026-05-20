package com.baomidou.mybatisplus.extension.dialect;

/**
 * 渲染边界的「token 翻译器」：把 wrapper 内部生成的方言中立 token 翻译为当前方言的最终 SQL。
 *
 * <h3>处理两类 token</h3>
 * <ol>
 *   <li><b>BACKTICK 标识符 token</b>：{@code `name`} → 当前方言引号（MySQL 反引号；PG/DM 双引号），
 *       通过 {@link SqlDialect#quoteIdentifier(String)}。</li>
 *   <li><b>INCOMING token</b>（4.x Phase 4 引入）：{@code duplicateValue(col)} 产出的「取插入行新值」
 *       占位符 → 当前方言的 upsert 新值引用（MySQL {@code VALUES(col)}；PG {@code EXCLUDED.col}；
 *       DM {@code src.col}），通过 {@link SqlDialect#incomingColumnRef(String)}。</li>
 * </ol>
 *
 * <h3>设计哲学</h3>
 * <p>wrapper 内部只产出方言中立占位符，最终方言形态在渲染边界统一翻译——避免在已渲染 SQL 上做
 * 文本改写（如旧 DM {@code mergeSetter} 的 {@code String.replace}）。
 *
 * <h3>转义规则</h3>
 * <ul>
 *   <li>SQL 单引号字符串字面值（{@code '...'}）内的反引号不被翻译</li>
 *   <li>不成对的反引号 / INCOMING token 保留原样</li>
 * </ul>
 */
public final class DialectQuoteTranslator {

    /**
     * INCOMING token 定界符。{@code [[MPS_INCOMING:<裸列名>]]}。
     * <p>方括号对在 MySQL/PG/DM 中均非标识符引号、不会出现在真实 SQL 表达式里，
     * 且非 {@code #{}}/{@code ${}} 形态，MyBatis 不会处理它——故可安全作为内部 token 定界符。
     */
    private static final String INCOMING_PREFIX = "[[MPS_INCOMING:";
    private static final String INCOMING_SUFFIX = "]]";

    private DialectQuoteTranslator() {}

    /**
     * 构造一个 INCOMING token（包裹裸列名）。供 {@code duplicateValue} 产出。
     */
    public static String incomingToken(String bareColumn) {
        return INCOMING_PREFIX + bareColumn + INCOMING_SUFFIX;
    }

    /**
     * 把 token 化的 SQL 片段翻译为当前方言形态。
     */
    public static String translate(String rawSql) {
        if (rawSql == null || rawSql.isEmpty()) {
            return rawSql;
        }
        return translate(rawSql, DialectRegistry.current());
    }

    /**
     * 显式 dialect 版本（用于测试 / 双向兼容）。
     */
    public static String translate(String rawSql, SqlDialect dialect) {
        if (rawSql == null || rawSql.isEmpty()) {
            return rawSql;
        }
        boolean hasBacktick = rawSql.indexOf('`') >= 0;
        boolean hasIncoming = rawSql.contains(INCOMING_PREFIX);
        if (!hasBacktick && !hasIncoming) {
            return rawSql;
        }
        String result = rawSql;
        // MySQL 反引号即正确引号，跳过反引号翻译；其余方言翻译为各自引号
        if (hasBacktick && dialect.dbType() != DbType.MYSQL) {
            result = translateBacktick(result, dialect);
        }
        if (hasIncoming) {
            result = translateIncoming(result, dialect);
        }
        return result;
    }

    /** 把 BACKTICK 标识符 token 翻译为方言引号（跳过 SQL 字符串字面值内的反引号）。 */
    private static String translateBacktick(String rawSql, SqlDialect dialect) {
        StringBuilder out = new StringBuilder(rawSql.length() + 16);
        boolean inLiteral = false;
        int len = rawSql.length();
        int i = 0;
        while (i < len) {
            char c = rawSql.charAt(i);
            if (c == '\'') {
                // 进入或退出 SQL 字符串字面值；处理 SQL 标准的 '' 转义
                if (inLiteral && i + 1 < len && rawSql.charAt(i + 1) == '\'') {
                    out.append("''");
                    i += 2;
                    continue;
                }
                inLiteral = !inLiteral;
                out.append(c);
                i++;
            } else if (c == '`' && !inLiteral) {
                int close = rawSql.indexOf('`', i + 1);
                if (close < 0) {
                    out.append(c);
                    i++;
                } else {
                    out.append(dialect.quoteIdentifier(rawSql.substring(i + 1, close)));
                    i = close + 1;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** 把 INCOMING token 翻译为当前方言的 upsert 新值引用。 */
    private static String translateIncoming(String sql, SqlDialect dialect) {
        StringBuilder out = new StringBuilder(sql.length() + 16);
        int len = sql.length();
        int i = 0;
        while (i < len) {
            int start = sql.indexOf(INCOMING_PREFIX, i);
            if (start < 0) {
                out.append(sql, i, len);
                break;
            }
            out.append(sql, i, start);
            int end = sql.indexOf(INCOMING_SUFFIX, start + INCOMING_PREFIX.length());
            if (end < 0) {
                // 不成对，原样输出避免破坏
                out.append(sql, start, len);
                break;
            }
            String bareColumn = sql.substring(start + INCOMING_PREFIX.length(), end);
            out.append(dialect.incomingColumnRef(bareColumn));
            i = end + INCOMING_SUFFIX.length();
        }
        return out.toString();
    }
}
