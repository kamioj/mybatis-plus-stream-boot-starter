package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.function.Consumer;

/**
 * duplicate语句中函数表达式包装
 *
 * @param <T> 实体类
 */
public final class DuplicateFunctionLambdaQueryWrapper<T> extends AbstractFunctionLambdaQueryWrapper<DuplicateWhereLambdaQueryWrapper<T>, DuplicateFunctionLambdaQueryWrapper<T>> {

    public DuplicateFunctionLambdaQueryWrapper() {
        super();
    }

    @Override
    @SuppressWarnings({"unchecked", "RedundantCast"})
    protected <C extends AbstractCaseLambdaQueryWrapper<DuplicateWhereLambdaQueryWrapper<T>, DuplicateFunctionLambdaQueryWrapper<T>, V, C>, V> C getCaseLambdaQueryWrapperInstance() {
        return (C) (Object) new DuplicateCaseLambdaQueryWrapper<T, V>();
    }

    public <V> V _case(Consumer<DuplicateCaseLambdaQueryWrapper<T, V>> _case) {
        return _case0(_case);
    }

    @SuppressWarnings("unchecked")
    public <V> V duplicateValue(SFunction<T, ?> column) {
        return function("VALUES", (Class<DuplicateFunctionLambdaQueryWrapper<T>>) getClass(), x -> x.column(column));
    }

}
