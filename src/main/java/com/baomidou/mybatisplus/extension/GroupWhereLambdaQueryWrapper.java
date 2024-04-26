package com.baomidou.mybatisplus.extension;

/**
 * 带聚会函数Where语句表达式包装
 */
public final class GroupWhereLambdaQueryWrapper extends AbstractWhereLambdaQueryWrapper<GroupFunctionLambdaQueryWrapper, GroupWhereLambdaQueryWrapper> {

    public GroupWhereLambdaQueryWrapper() {
        this(null);
    }

    public GroupWhereLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

}
