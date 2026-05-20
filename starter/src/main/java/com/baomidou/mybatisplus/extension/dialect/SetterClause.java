package com.baomidou.mybatisplus.extension.dialect;

/**
 * 结构化 setter 子句（4.x Phase 4 引入）。
 *
 * <p>取代旧的「预渲染 SQL 字符串」表示，使各方言能按自己的语法渲染 UPDATE SET 与
 * 冲突子句（{@code ON DUPLICATE} / {@code ON CONFLICT} / {@code MERGE}），
 * 无需对已渲染 SQL 做文本改写。
 *
 * <ul>
 *   <li>{@code tableQualifier}：目标列的表限定符（裸表名 / 别名），可空。
 *       连表 UPDATE 中同名列消歧需要它；冲突子句为单表，渲染时忽略。</li>
 *   <li>{@code targetColumn}：裸目标列名（不带引号、不带表前缀）。</li>
 *   <li>{@code valueExpr}：RHS 表达式，方言中立。内部可含 BACKTICK 标识符 token 与
 *       INCOMING token（见 {@link DialectQuoteTranslator}），在渲染边界翻译。</li>
 * </ul>
 */
public final class SetterClause {

    private final String tableQualifier;
    private final String targetColumn;
    private final String valueExpr;

    public SetterClause(String tableQualifier, String targetColumn, String valueExpr) {
        this.tableQualifier = tableQualifier;
        this.targetColumn = targetColumn;
        this.valueExpr = valueExpr;
    }

    /** 目标列的表限定符（裸名），可空 */
    public String getTableQualifier() {
        return tableQualifier;
    }

    /** 裸目标列名 */
    public String getTargetColumn() {
        return targetColumn;
    }

    /** RHS 表达式（方言中立，可含 BACKTICK / INCOMING token） */
    public String getValueExpr() {
        return valueExpr;
    }
}
