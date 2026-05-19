package com.baomidou.mybatisplus.extension.wrapper;
import com.baomidou.mybatisplus.extension.core.ExQueryWrapper;

/**
 * 不带聚会函数Where语句表达式包装
 */
public final class DuplicateWhereLambdaQueryWrapper<T> extends AbstractWhereLambdaQueryWrapper<DuplicateFunctionLambdaQueryWrapper<T>, DuplicateWhereLambdaQueryWrapper<T>> {

    public DuplicateWhereLambdaQueryWrapper() {
        this(null);
    }

    public DuplicateWhereLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

}
