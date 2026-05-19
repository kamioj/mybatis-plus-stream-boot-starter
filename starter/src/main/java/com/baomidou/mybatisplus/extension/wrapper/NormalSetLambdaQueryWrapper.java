package com.baomidou.mybatisplus.extension.wrapper;
import com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper;

/**
 * 去重赋值表达式包装
 */
public final class NormalSetLambdaQueryWrapper extends AbstractSetLambdaQueryWrapper<NormalFunctionLambdaQueryWrapper, NormalSetLambdaQueryWrapper> {

    public NormalSetLambdaQueryWrapper() {
        this(new ExecutableQueryWrapper<>());
    }

    public NormalSetLambdaQueryWrapper(ExecutableQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

}
