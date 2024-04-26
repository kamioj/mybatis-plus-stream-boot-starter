package com.baomidou.mybatisplus.extension;

import java.util.function.Consumer;

/**
 * 子语句表达式包装
 */
public final class SubSqlLambdaQueryWrapper extends AbstractSubSqlLambdaQueryWrapper<SubSqlLambdaQueryWrapper> {

    public SubSqlLambdaQueryWrapper() {
        this(null);
    }

    public SubSqlLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

    /**
     * 查询
     *
     * @param select 查询表达式
     * @return 实例本身
     */
    public SubSqlLambdaQueryWrapper select(Consumer<SubSelectLambdaQueryWrapper> select) {
        SubSelectLambdaQueryWrapper selectLambda = new SubSelectLambdaQueryWrapper(getQueryWrapper());
        if (select != null) {
            select.accept(selectLambda);
        }
        return typedThis;
    }

}
