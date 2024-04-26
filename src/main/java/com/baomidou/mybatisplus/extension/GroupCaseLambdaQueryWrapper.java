package com.baomidou.mybatisplus.extension;


/**
 * 带聚会函数Case语句表达式包装
 *
 * @param <V> 值类型
 */
public final class GroupCaseLambdaQueryWrapper<V> extends AbstractCaseLambdaQueryWrapper<GroupWhereLambdaQueryWrapper, GroupFunctionLambdaQueryWrapper, V, GroupCaseLambdaQueryWrapper<V>> {
    public GroupCaseLambdaQueryWrapper() {
        this(null);
    }

    public GroupCaseLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }
}
