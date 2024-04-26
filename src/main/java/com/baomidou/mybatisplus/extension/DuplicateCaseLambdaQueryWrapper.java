package com.baomidou.mybatisplus.extension;


/**
 * 不带聚会函数Case语句表达式包装
 *
 * @param <V> 值类型
 */
public final class DuplicateCaseLambdaQueryWrapper<T, V> extends AbstractCaseLambdaQueryWrapper<DuplicateWhereLambdaQueryWrapper<T>, DuplicateFunctionLambdaQueryWrapper<T>, V, DuplicateCaseLambdaQueryWrapper<T, V>> {
    public DuplicateCaseLambdaQueryWrapper() {
        this(null);
    }

    public DuplicateCaseLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }
}
