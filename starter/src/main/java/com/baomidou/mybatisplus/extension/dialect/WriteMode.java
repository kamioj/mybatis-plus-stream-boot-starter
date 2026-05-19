package com.baomidou.mybatisplus.extension.dialect;

/**
 * 批量写入操作的语义模式。<b>4.0.2 引入</b>。
 *
 * <p>由 {@code ExecutableQueryWrapper} 在执行 {@code saveDuplicate / saveIgnore / saveReplace}
 * 时设置，{@link SqlDialect} 据此选择对应的 SQL 片段（INSERT 前缀 + 冲突处理子句）。
 */
public enum WriteMode {

    /** 普通批量插入（无冲突处理）*/
    INSERT,

    /** UPSERT —— 冲突时按 setter 指定列更新（{@code saveDuplicate}）*/
    DUPLICATE,

    /** 冲突时忽略（{@code saveIgnore}）*/
    IGNORE,

    /** 冲突时全列覆盖（{@code saveReplace}）*/
    REPLACE
}
