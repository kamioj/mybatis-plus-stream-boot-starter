package com.baomidou.mybatisplus.extension.wrapper;
import com.baomidou.mybatisplus.extension.core.ExQueryWrapper;

/**
 * 不带聚会函数Where语句表达式包装
 */
public final class NormalWhereLambdaQueryWrapper extends AbstractWhereLambdaQueryWrapper<NormalFunctionLambdaQueryWrapper, NormalWhereLambdaQueryWrapper> {

    public NormalWhereLambdaQueryWrapper() {
        this(null);
    }

    public NormalWhereLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

}
