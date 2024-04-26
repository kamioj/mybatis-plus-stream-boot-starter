package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.function.Consumer;

/**
 * 分组表达式包装
 */
public final class GroupLambdaQueryWrapper extends LambdaQueryWrapper<GroupLambdaQueryWrapper> {

    public GroupLambdaQueryWrapper() {
        this(null);
    }

    public GroupLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

    /**
     * 分组
     *
     * @param groupColumn 分组列
     * @param rename      表重命名
     * @param <T>         实体类型
     * @param <V>         值类型
     * @return 实例本身
     */
    public <T, V> GroupLambdaQueryWrapper groupBy(SFunction<T, V> groupColumn, String rename) {
        groupByFunc(x -> x.column(groupColumn, rename));
        return typedThis;
    }

    /**
     * 分组
     *
     * @param groupColumns 分组列
     * @param <T>          实体类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T> GroupLambdaQueryWrapper groupBy(SFunction<T, ?>... groupColumns) {
        for (SFunction<T, ?> groupColumn : groupColumns) {
            groupBy(groupColumn, null);
        }
        return typedThis;
    }

    /**
     * 分组
     *
     * @param groupFunc 分组表达式
     * @return 实例本身
     */
    public GroupLambdaQueryWrapper groupByFunc(Consumer<NormalFunctionLambdaQueryWrapper> groupFunc) {
        try {
            String sqlGroup = this.getSubSqlSegment(groupFunc, NormalFunctionLambdaQueryWrapper.class);
            if (!StringUtils.isEmpty(sqlGroup)) {
                getQueryWrapper().groupBy(sqlGroup);
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 分组筛选
     *
     * @param predicate 分组条件
     * @return 实例本身
     */
    public GroupLambdaQueryWrapper having(Consumer<GroupWhereLambdaQueryWrapper> predicate) {
        try {
            String sqlHaving = this.getSubSqlSegment(predicate, GroupWhereLambdaQueryWrapper.class);
            if (!StringUtils.isEmpty(sqlHaving)) {
                getQueryWrapper().having(sqlHaving);
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }
}
