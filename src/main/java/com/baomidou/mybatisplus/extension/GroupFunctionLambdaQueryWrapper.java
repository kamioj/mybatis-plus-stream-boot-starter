package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 带聚会函数函数表达式包装
 */
public final class GroupFunctionLambdaQueryWrapper extends AbstractFunctionLambdaQueryWrapper<GroupWhereLambdaQueryWrapper, GroupFunctionLambdaQueryWrapper> {

    public GroupFunctionLambdaQueryWrapper() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <C extends AbstractCaseLambdaQueryWrapper<GroupWhereLambdaQueryWrapper, GroupFunctionLambdaQueryWrapper, V, C>, V> C getCaseLambdaQueryWrapperInstance() {
        return (C) new GroupCaseLambdaQueryWrapper<V>();
    }

    public <V> V _case(Consumer<GroupCaseLambdaQueryWrapper<V>> _case) {
        return _case0(_case);
    }

    /**
     * 数量
     *
     * @return 数字类型
     */
    public Long count() {
        return countFunc(null);
    }

    /**
     * 非空数量
     *
     * @param column 非空列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V> Long count(SFunction<T, V> column) {
        return count(column, null);
    }

    /**
     * 非空数量
     *
     * @param column 非空列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V> Long count(SFunction<T, V> column, String rename) {
        return countFunc(x -> x.column(column, rename));
    }

    /**
     * 两列非空去重数量
     *
     * @param column 非空去重列
     * @param <T>    实体类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <T> Long countDistinct(SFunction<T, ?>... column) {
        Function<NormalFunctionLambdaQueryWrapper, ?>[] func = new Function[column.length];
        for (int i = 0; i < column.length; i++) {
            int finalI = i;
            func[i] = x -> x.column(column[finalI]);
        }
        return countDistinctFunc(func);
    }

    public <T1, T2> Long countDistinct(SFunction<T1, ?> column1, SFunction<T2, ?> column2) {
        return countDistinctFunc(x -> x.column(column1), x -> x.column(column2));
    }

    /**
     * 非空去重数量
     *
     * @param column 去重列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V> Long countDistinct(SFunction<T, V> column, String rename) {
        return countDistinctFunc(x -> x.column(column, rename));
    }

    /**
     * 数量
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    @SafeVarargs
    public final Long countDistinctFunc(Function<NormalFunctionLambdaQueryWrapper, ?>... func) {
        Func<?>[] subFunc = new Func<?>[func.length == 0 ? 0 : func.length * 2];
        int i = 0;
        for (Function<NormalFunctionLambdaQueryWrapper, ?> f : func) {
            if (i == 0) {
                subFunc[i++] = Func.distinct;
            } else {
                subFunc[i++] = Func.comma;
            }
            subFunc[i++] = new Func<>(NormalFunctionLambdaQueryWrapper.class, f);
        }
        return function("COUNT", subFunc);
    }

    /**
     * 数量
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    public Long countFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func) {
        if (func == null) {
            return function("COUNT", NormalFunctionLambdaQueryWrapper.class, x -> x.value(1));
        } else {
            return function("COUNT", NormalFunctionLambdaQueryWrapper.class, func);
        }
    }

    /**
     * 条件数量
     *
     * @param predicate 表达式
     * @return 数字类型
     */
    public Long countPredicate(Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return countFunc(x -> x._case(y -> y.whenThenFunc(predicate, z -> z.value(1))));
    }

    /**
     * 条件非空数量
     *
     * @param predicate 表达式
     * @param column    非空列
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 数字类型
     */
    public <T, V> Long countPredicate(Consumer<NormalWhereLambdaQueryWrapper> predicate, SFunction<T, V> column) {
        return countFunc(x -> x._case(y -> y.whenThenFunc(predicate, z -> z.column(column))));
    }

    /**
     * 条件非空数量
     *
     * @param predicate 表达式
     * @param column    非空列
     * @param rename    表重命名
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 数字类型
     */
    public <T, V> Long countPredicate(Consumer<NormalWhereLambdaQueryWrapper> predicate, SFunction<T, V> column, String rename) {
        return countFunc(x -> x._case(y -> y.whenThenFunc(predicate, z -> z.column(column, rename))));
    }

    /**
     * 条件非空去重数量
     *
     * @param predicate 条件表达式
     * @param column    非空去重列
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 数字类型
     */
    public <T, V> Long countPredicateDistinct(Consumer<NormalWhereLambdaQueryWrapper> predicate, SFunction<T, V> column) {
        return countPredicateDistinct(predicate, column, null);
    }

    /**
     * 条件非空去重数量
     *
     * @param predicate 条件表达式
     * @param column    非空去重列
     * @param rename    表重命名
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 数字类型
     */
    public <T, V> Long countPredicateDistinct(Consumer<NormalWhereLambdaQueryWrapper> predicate, SFunction<T, V> column, String rename) {
        return countDistinctFunc(x -> x._case(y -> y.whenThenFunc(predicate, z -> z.column(column, rename))));
    }

    /**
     * 合计
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 数字类型
     */
    public <T> BigDecimal sum(SFunction<T, Number> column) {
        return sum(column, null);
    }

    /**
     * 合计
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal sum(SFunction<T, Number> column, String rename) {
        return sumFunc(x -> x.column(column, rename));
    }

    /**
     * 去重合计
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal sumDistinct(SFunction<T, Number> column) {
        return sumDistinct(column, null);
    }

    /**
     * 去重合计
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal sumDistinct(SFunction<T, Number> column, String rename) {
        return sumDistinctFunc(x -> x.column(column, rename));
    }

    /**
     * 合计
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    public BigDecimal sumFunc(Function<NormalFunctionLambdaQueryWrapper, Number> func) {
        return function("SUM", NormalFunctionLambdaQueryWrapper.class, func);
    }

    /**
     * 去重合计
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    public BigDecimal sumDistinctFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func) {
        return function("SUM", Func.distinct, new Func<>(NormalFunctionLambdaQueryWrapper.class, func));
    }

    /**
     * 合计
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 数字类型
     */
    public <T> BigDecimal sumDefaultZero(SFunction<T, Number> column) {
        return ifnullFunc(
                fun -> fun.sumFunc(x -> x.column(column)),
                fun -> fun.value(BigDecimal.valueOf(0))
        );
    }

    /**
     * 合计
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal sumDefaultZero(SFunction<T, Number> column, String rename) {
        return ifnullFunc(
                fun -> fun.sumFunc(x -> x.column(column, rename)),
                fun -> fun.value(BigDecimal.valueOf(0))
        );
    }

    /**
     * 平均值
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 数字类型
     */
    public <T> BigDecimal avg(SFunction<T, Number> column) {
        return avg(column, null);
    }

    /**
     * 平均值
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal avg(SFunction<T, Number> column, String rename) {
        return avgFunc(x -> x.column(column, rename));
    }

    /**
     * 去重平均值
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal avgDistinct(SFunction<T, Number> column) {
        return avgDistinct(column, null);
    }

    /**
     * 去重平均值
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal avgDistinct(SFunction<T, Number> column, String rename) {
        return avgDistinctFunc(x -> x.column(column, rename));
    }

    /**
     * 平均值
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    public BigDecimal avgFunc(Function<NormalFunctionLambdaQueryWrapper, Number> func) {
        return function("AVG", NormalFunctionLambdaQueryWrapper.class, func);
    }


    /**
     * 去重平均值
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    public BigDecimal avgDistinctFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func) {
        return function("AVG", Func.distinct, new Func<>(NormalFunctionLambdaQueryWrapper.class, func));
    }

    /**
     * 最小值
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 值类型
     */
    public <T, V> V min(SFunction<T, V> column) {
        return min(column, null);
    }

    /**
     * 最小值
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 值类型
     */
    public <T, V> V min(SFunction<T, V> column, String rename) {
        return minFunc(x -> x.column(column, rename));
    }

    /**
     * 去重最小值
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal minDistinct(SFunction<T, Number> column) {
        return minDistinct(column, null);
    }

    /**
     * 去重最小值
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal minDistinct(SFunction<T, Number> column, String rename) {
        return minDistinctFunc(x -> x.column(column, rename));
    }

    /**
     * 最小值
     *
     * @param func 函数表达式
     * @param <V>  值类型
     * @return 值类型
     */
    public <V> V minFunc(Function<NormalFunctionLambdaQueryWrapper, V> func) {
        return function("MIN", NormalFunctionLambdaQueryWrapper.class, func);
    }

    /**
     * 去重最小值
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    public BigDecimal minDistinctFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func) {
        return function("MIN", Func.distinct, new Func<>(NormalFunctionLambdaQueryWrapper.class, func));
    }

    /**
     * 最大值
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 值类型
     */
    public <T, V> V max(SFunction<T, V> column) {
        return max(column, null);
    }

    /**
     * 最大值
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 值类型
     */
    public <T, V> V max(SFunction<T, V> column, String rename) {
        return maxFunc(x -> x.column(column, rename));
    }

    /**
     * 去重最大值
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal maxDistinct(SFunction<T, Number> column) {
        return maxDistinct(column, null);
    }

    /**
     * 去重最大值
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 值类型
     */
    public <T> BigDecimal maxDistinct(SFunction<T, Number> column, String rename) {
        return maxDistinctFunc(x -> x.column(column, rename));
    }

    /**
     * 最大值
     *
     * @param func 函数表达式
     * @param <V>  值类型
     * @return 值类型
     */
    public <V> V maxFunc(Function<NormalFunctionLambdaQueryWrapper, V> func) {
        return function("MAX", NormalFunctionLambdaQueryWrapper.class, func);
    }

    /**
     * 去重最大值
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    public BigDecimal maxDistinctFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func) {
        return function("MAX", Func.distinct, new Func<>(NormalFunctionLambdaQueryWrapper.class, func));
    }

    /**
     * 将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 字符串类型
     */
    public <T, V> String groupConcat(SFunction<T, V> column) {
        return groupConcat(column, null, null);
    }

    /**
     * 将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param column 列
     * @param order  排序表达式
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 字符串类型
     */
    public <T, V> String groupConcat(SFunction<T, V> column, Consumer<OrderLambdaQueryWrapper> order) {
        return groupConcat(column, order, null);
    }

    /**
     * 将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param column    列
     * @param separator 分隔符
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 字符串类型
     */
    public <T, V> String groupConcat(SFunction<T, V> column, String separator) {
        return groupConcat(column, null, separator);
    }

    /**
     * 将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param column    列
     * @param order     排序表达式
     * @param separator 分隔符
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 字符串类型
     */
    public <T, V> String groupConcat(SFunction<T, V> column, Consumer<OrderLambdaQueryWrapper> order, String separator) {
        return groupConcatFunc(x -> x.column(column), order, separator);
    }

    /**
     * 去重将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 字符串类型
     */
    public <T, V> String groupConcatDistinct(SFunction<T, V> column) {
        return groupConcatDistinct(column, null, null);
    }

    /**
     * 去重将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param column 列
     * @param order  排序表达式
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 字符串类型
     */
    public <T, V> String groupConcatDistinct(SFunction<T, V> column, Consumer<OrderLambdaQueryWrapper> order) {
        return groupConcatDistinct(column, order, null);
    }

    /**
     * 去重将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param column    列
     * @param separator 分隔符
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 字符串类型
     */
    public <T, V> String groupConcatDistinct(SFunction<T, V> column, String separator) {
        return groupConcatDistinct(column, null, separator);
    }

    /**
     * 去重将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param column    列
     * @param order     排序表达式
     * @param separator 分隔符
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 字符串类型
     */
    public <T, V> String groupConcatDistinct(SFunction<T, V> column, Consumer<OrderLambdaQueryWrapper> order, String separator) {
        return groupConcatDistinctFunc(x -> x.column(column), order, separator);
    }

    /**
     * 将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param func 函数表达式
     * @return 字符串类型
     */
    public String groupConcatFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func) {
        return groupConcatFunc(func, null, null);
    }

    /**
     * 将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param func  函数表达式
     * @param order 排序表达式
     * @return 字符串类型
     */
    public String groupConcatFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func, Consumer<OrderLambdaQueryWrapper> order) {
        return groupConcatFunc(func, order, null);
    }

    /**
     * 将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param func      函数表达式
     * @param separator 分隔符
     * @return 字符串类型
     */
    public String groupConcatFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func, String separator) {
        return groupConcatFunc(func, null, separator);
    }

    /**
     * 将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param func      函数表达式
     * @param order     排序表达式
     * @param separator 分隔符
     * @return 字符串类型
     */
    public String groupConcatFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func, Consumer<OrderLambdaQueryWrapper> order, String separator) {
        try {
            OrderLambdaQueryWrapper orderByLambda = new OrderLambdaQueryWrapper();
            String orderBySql = "";
            String separatorSql = "";
            if (order != null) {
                order.accept(orderByLambda);
                orderBySql = " " + orderByLambda.getQueryWrapper().getCustomSqlSegment();
            }
            if (!StringUtils.isEmpty(separator)) {
                separatorSql = " SEPARATOR " + separator;
            }
            sqlSegment = "GROUP_CONCAT(" + getSubSqlSegment(func, NormalFunctionLambdaQueryWrapper.class) + orderBySql + separatorSql + ")";
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    /**
     * 将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param func  函数表达式
     * @param order 排序表达式
     * @return 字符串类型
     */
    public String groupConcatDistinctFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func, Consumer<OrderLambdaQueryWrapper> order) {
        return groupConcatDistinctFunc(func, order, null);
    }

    /**
     * 去重将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param func      函数表达式
     * @param separator 分隔符
     * @return 字符串类型
     */
    public String groupConcatDistinctFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func, String separator) {
        return groupConcatDistinctFunc(func, null, separator);
    }

    /**
     * 去重将组中的字符串连接成为具有各种选项的单个字符串
     *
     * @param func      函数表达式
     * @param order     排序表达式
     * @param separator 分隔符
     * @return 字符串类型
     */
    public String groupConcatDistinctFunc(Function<NormalFunctionLambdaQueryWrapper, ?> func, Consumer<OrderLambdaQueryWrapper> order, String separator) {
        try {
            OrderLambdaQueryWrapper orderByLambda = new OrderLambdaQueryWrapper();
            String orderBySql = "";
            String separatorSql = "";
            if (order != null) {
                order.accept(orderByLambda);
                orderBySql = " " + orderByLambda.getQueryWrapper().getCustomSqlSegment();
            }
            if (!StringUtils.isEmpty(separator)) {
                separatorSql = " SEPARATOR " + separator;
            }
            sqlSegment = "GROUP_CONCAT(DISTINCT " + getSubSqlSegment(func, NormalFunctionLambdaQueryWrapper.class) + orderBySql + separatorSql + ")";
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    /**
     * 获取分组第一个元素
     *
     * @param column 列
     * @param order  排序表达式
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 字符串类型
     */
    public <T, V> String groupFirst(SFunction<T, V> column, Consumer<OrderLambdaQueryWrapper> order) {
        return groupFirst(column, null, order);
    }

    /**
     * 获取分组第一个元素
     *
     * @param column 列
     * @param rename 表重命名
     * @param order  排序表达式
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 字符串类型
     */
    public <T, V> String groupFirst(SFunction<T, V> column, String rename, Consumer<OrderLambdaQueryWrapper> order) {
        return substringIndexFunc(x -> x.groupConcatFunc(y -> y.column(column, rename), order), ",", 1);
    }

}
