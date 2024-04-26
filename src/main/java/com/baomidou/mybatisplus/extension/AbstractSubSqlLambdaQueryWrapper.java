package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.toolkit.MybatisUtil;

import java.util.function.Consumer;

/**
 * 子语句表达式包装
 */
public abstract class AbstractSubSqlLambdaQueryWrapper<Children extends AbstractSubSqlLambdaQueryWrapper<Children>> extends AbstractSubLambdaQueryWrapper<Children> {

    public AbstractSubSqlLambdaQueryWrapper() {
        this(null);
    }

    public AbstractSubSqlLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

    /**
     * 获取子查询sql并传入注入参数
     *
     * @param queryWrapper 待传入注入参数的queryWrapper
     * @return 子查询sql
     */
    @Override
    public String getSqlSegment(ExQueryWrapper<?> queryWrapper) {
        String sqlSelect = getQueryWrapper().getSqlSelect();
        if (StringUtils.isEmpty(sqlSelect)) {
            sqlSelect = "*";
        }

        String customSqlSegment = "SELECT " + sqlSelect + " FROM " + getQueryWrapper().getSqlFrom() + " " + getQueryWrapper().getCustomSqlSegment();
        return getSqlSegmentWithParam(customSqlSegment, getQueryWrapper(), queryWrapper);
    }

    /**
     * 表
     *
     * @param clazz 实体类型
     * @param <T>   实体类型
     * @return 实例本身
     */
    public <T> Children from(Class<T> clazz) {
        return from(clazz, null, null);
    }

    /**
     * 表
     *
     * @param clazz  实体类型
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 实例本身
     */
    public <T> Children from(Class<T> clazz, String rename) {
        return from(clazz, rename, null);
    }

    /**
     * 表
     *
     * @param clazz 实体类型
     * @param join  连表表达式
     * @param <T>   实体类型
     * @return 实例本身
     */
    public <T> Children from(Class<T> clazz, Consumer<JoinLambdaQueryWrapper<T>> join) {
        return from(clazz, null, join);
    }

    /**
     * 表
     *
     * @param clazz  实体类型
     * @param rename 表重命名
     * @param join   连表表达式
     * @param <T>    实体类型
     * @return 实例本身
     */
    @SuppressWarnings("unchecked")
    public <T> Children from(Class<T> clazz, String rename, Consumer<JoinLambdaQueryWrapper<T>> join) {
        ((ExQueryWrapper<T>) getQueryWrapper()).setFromTable(MybatisUtil.getTableInfo(clazz), rename);

        if (join != null) {
            JoinLambdaQueryWrapper<T> joinLambda = new JoinLambdaQueryWrapper<>(getQueryWrapper(), clazz, rename);
            join.accept(joinLambda);
        }
        return typedThis;
    }

    /**
     * 条件
     *
     * @param predicate 条件表达式
     * @return 实例本身
     */
    public Children where(Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        NormalWhereLambdaQueryWrapper whereLambda = new NormalWhereLambdaQueryWrapper(getQueryWrapper());
        predicate.accept(whereLambda);
        return typedThis;
    }

    /**
     * 分组
     *
     * @param group 分组表达式
     * @return 实例本身
     */
    public Children group(Consumer<GroupLambdaQueryWrapper> group) {
        GroupLambdaQueryWrapper groupLambda = new GroupLambdaQueryWrapper(getQueryWrapper());
        group.accept(groupLambda);
        return typedThis;
    }

    /**
     * 排序
     *
     * @param order 排序表达式
     * @return 实例本身
     */
    public Children order(Consumer<OrderLambdaQueryWrapper> order) {
        OrderLambdaQueryWrapper orderLambda = new OrderLambdaQueryWrapper(getQueryWrapper());
        if (order != null) {
            order.accept(orderLambda);
        }
        return typedThis;
    }

    /**
     * 限制条数
     *
     * @param limit 限制条数
     * @return 实例本身
     */
    public Children limit(int limit) {
        getQueryWrapper().last("LIMIT " + limit);
        return typedThis;
    }
}
