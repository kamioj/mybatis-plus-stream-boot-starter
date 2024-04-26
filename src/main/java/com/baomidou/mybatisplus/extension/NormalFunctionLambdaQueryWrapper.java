package com.baomidou.mybatisplus.extension;

import java.util.function.Consumer;

/**
 * 不带聚会函数函数表达式包装
 */
public final class NormalFunctionLambdaQueryWrapper extends AbstractFunctionLambdaQueryWrapper<NormalWhereLambdaQueryWrapper, NormalFunctionLambdaQueryWrapper> {

    public NormalFunctionLambdaQueryWrapper() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <C extends AbstractCaseLambdaQueryWrapper<NormalWhereLambdaQueryWrapper, NormalFunctionLambdaQueryWrapper, V, C>, V> C getCaseLambdaQueryWrapperInstance() {
        return (C) new NormalCaseLambdaQueryWrapper<V>();
    }

    public <V> V _case(Consumer<NormalCaseLambdaQueryWrapper<V>> _case) {
        return _case0(_case);
    }

}
