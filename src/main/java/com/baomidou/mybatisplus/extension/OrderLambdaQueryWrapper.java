package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 排序表达式包装
 */
public final class OrderLambdaQueryWrapper extends LambdaQueryWrapper<OrderLambdaQueryWrapper> {

    public OrderLambdaQueryWrapper() {
        this(null);
    }

    public OrderLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

    /**
     * 正序排序
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> OrderLambdaQueryWrapper orderAsc(SFunction<T, V> column) {
        return order(column, true);
    }

    /**
     * 倒序排序
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> OrderLambdaQueryWrapper orderDesc(SFunction<T, V> column) {
        return order(column, false);
    }

    /**
     * 排序
     *
     * @param column 列
     * @param asc    是否升序
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> OrderLambdaQueryWrapper order(SFunction<T, V> column, boolean asc) {
        return order(true, column, asc);
    }

    /**
     * 排序
     *
     * @param func 函数表达式
     * @param asc  是否升序
     * @return 实例本身
     */
    public OrderLambdaQueryWrapper orderFunc(Function<GroupFunctionLambdaQueryWrapper, ?> func, boolean asc) {
        return orderFunc(true, func, asc);
    }

    /**
     * 正序排序
     *
     * @param condition 条件
     * @param column    列
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> OrderLambdaQueryWrapper orderAsc(boolean condition, SFunction<T, V> column) {
        return order(condition, column, true);
    }

    /**
     * 倒序排序
     *
     * @param condition 条件
     * @param column    列
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> OrderLambdaQueryWrapper orderDesc(boolean condition, SFunction<T, V> column) {
        return order(condition, column, false);
    }

    /**
     * 排序
     *
     * @param condition 条件
     * @param column    列
     * @param asc       是否升序
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> OrderLambdaQueryWrapper order(boolean condition, SFunction<T, V> column, boolean asc) {
        return orderFunc(condition, x -> x.column(column), asc);
    }

    /**
     * 排序
     *
     * @param condition 条件
     * @param func      函数表达式
     * @param asc       是否升序
     * @return 实例本身
     */
    public OrderLambdaQueryWrapper orderFunc(boolean condition, Function<GroupFunctionLambdaQueryWrapper, ?> func, boolean asc) {
        try {
            getQueryWrapper().orderBy(condition, asc, getSubSqlSegment(func, GroupFunctionLambdaQueryWrapper.class));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 随机排序
     *
     * @param condition 条件
     * @param seed      随机种子
     * @return 实例本身
     */
    public OrderLambdaQueryWrapper orderByRandom(boolean condition, Serializable seed) {
        return orderFunc(condition, x -> x.rand(seed), true);
    }

    /**
     * 随机排序
     *
     * @param condition 条件
     * @return 实例本身
     */
    public OrderLambdaQueryWrapper orderByRandom(boolean condition) {
        return orderFunc(condition, x -> x.randFunc(AbstractFunctionLambdaQueryWrapper::now), true);
    }
}
