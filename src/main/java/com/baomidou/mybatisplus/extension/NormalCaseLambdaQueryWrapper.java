package com.baomidou.mybatisplus.extension;

/**
 * 不带聚会函数Case语句表达式包装
 *
 * @param <V> 值类型
 */
public final class NormalCaseLambdaQueryWrapper<V> extends AbstractCaseLambdaQueryWrapper<NormalWhereLambdaQueryWrapper, NormalFunctionLambdaQueryWrapper, V, NormalCaseLambdaQueryWrapper<V>> {
    public NormalCaseLambdaQueryWrapper() {
        this(null);
    }

    public NormalCaseLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }
}
