package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.mapper.MysqlBaseMapper;
import com.baomidou.mybatisplus.toolkit.MybatisUtil;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;
import org.apache.ibatis.type.TypeReference;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class MybatisQueryableStream<T, R, Children extends MybatisQueryableStream<T, R, Children>> extends MybatisStream<T, R, ExQueryWrapper<T>, Children> {
    protected final Type[] renameClass;

    abstract Function<Object[], R> getReturnMapper();

    public MybatisQueryableStream(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Type... renameClass) {
        this(null, entityClass, baseMapper, renameClass);
    }

    public MybatisQueryableStream(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Type... renameClass) {
        super(queryWrapper == null ? new ExQueryWrapper<T>() {{
            setFromTable(MybatisUtil.getTableInfo(entityClass), null);
        }} : queryWrapper, entityClass, baseMapper);
        this.renameClass = renameClass;
    }

    @SafeVarargs
    public MybatisQueryableStream(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        this(null, entityClass, baseMapper, renameType);
    }

    @SafeVarargs
    public MybatisQueryableStream(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        super(queryWrapper == null ? new ExQueryWrapper<T>() {{
            setFromTable(MybatisUtil.getTableInfo(entityClass), null);
        }} : queryWrapper, entityClass, baseMapper);
        this.renameClass = Arrays.stream(renameType).map(item -> ReflectUtils.invokeMethod(item, "getSuperclassTypeParameter")).toArray(Type[]::new);
    }

//    /**
//     * 获取子查询sql并传入注入参数
//     *
//     * @param queryWrapper 待传入注入参数的queryWrapper
//     * @return 子查询sql
//     */
//    @Override
//    public String getSqlSegment(ExQueryWrapper<?> queryWrapper) {
//        String sqlSelect = getQueryWrapper().getSqlSelect();
//        if (StringUtils.isEmpty(sqlSelect)) {
//            sqlSelect = "*";
//        }
//
//        String customSqlSegment = getSqlSegmentWithParam(getQueryWrapper().getCustomSqlSegment(), getQueryWrapper(), queryWrapper);
//        return "SELECT " + sqlSelect + " FROM " + getQueryWrapper().getSqlFrom() + " " + customSqlSegment;
//    }


    /**
     * 分组
     *
     * @param group 分组表达式
     * @return 实例本身
     */
    public Children group(Consumer<GroupLambdaQueryWrapper> group) {
        GroupLambdaQueryWrapper groupLambda = new GroupLambdaQueryWrapper(queryWrapper);
        group.accept(groupLambda);
        return typedThis;
    }

    /**
     * 去重
     *
     * @return 实例本身
     */
    @Override
    public Children distinct() {
        this.queryWrapper.setDistinct(true);
        return typedThis;
    }

    /**
     * 排序
     *
     * @param order 排序表达式
     * @return 实例本身
     */
    @Override
    public Children sorted(Consumer<OrderLambdaQueryWrapper> order) {
        OrderLambdaQueryWrapper orderLambda = new OrderLambdaQueryWrapper(queryWrapper);
        if (order != null) {
            order.accept(orderLambda);
        }
        return typedThis;
    }

    /**
     * 限制条数
     *
     * @param maxSize 限制条数
     * @return 实例本身
     */
    @Override
    public Children limit(long maxSize) {
        queryWrapper.setLimit(maxSize);
        queryWrapper.last("LIMIT " + queryWrapper.getSkip() + ", " + queryWrapper.getLimit() + (queryWrapper.isForUpdate() ? " FOR UPDATE" : ""));
        return typedThis;
    }

    /**
     * 跳过条数
     *
     * @param n 跳过条数
     * @return 实例本身
     */
    @Override
    public Children skip(long n) {
        queryWrapper.setSkip(n);
        queryWrapper.last("LIMIT " + queryWrapper.getSkip() + ", " + queryWrapper.getLimit() + (queryWrapper.isForUpdate() ? " FOR UPDATE" : ""));
        return typedThis;
    }

    @Override
    public boolean exist() {
        ExQueryWrapper<T> existQueryWrapper = this.queryWrapper.toExistWrapper();
        boolean exist = baseMapper.list(existQueryWrapper).size() > 0;
        this.queryWrapper.reset();
        return exist;
    }

    @Override
    public long count() {
        ExQueryWrapper<T> countQueryWrapper = this.queryWrapper.toCountQueryWrapper("C");
        Long count = (Long) baseMapper.list(countQueryWrapper).get(0).get("C");
        this.queryWrapper.reset();
        return count;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<R> toStream() {
        String sqlSelect = queryWrapper.getSqlSelect();
        if (renameClass.length == 1 && renameClass[0].equals(Object.class)) {
            sqlSelect = sqlSelect.split(",(?![^()]*+\\))")[0].split("(?i) as ")[0] + " AS V";
        }
        if (queryWrapper.isDistinct()) {
            sqlSelect = "DISTINCT " + sqlSelect.trim().replaceAll("^(?i)distinct\\s+", "");
        }
        queryWrapper.select(sqlSelect);
        List<Map<String, Object>> result = baseMapper.list(queryWrapper);
        this.queryWrapper.reset();
        if (renameClass.length == 1 && renameClass[0].equals(Object.class)) {
            // 单值类型
            return result.stream().filter(Objects::nonNull).map(x -> (R) x.get("V")).filter(Objects::nonNull);
        } else {
            // 实体类型
            return MybatisUtil.mapStream(result, renameClass).map(getReturnMapper());
        }
    }

    public Children forUpdate() {
        queryWrapper.setForUpdate(true);
        queryWrapper.last("LIMIT " + queryWrapper.getSkip() + ", " + queryWrapper.getLimit() + (queryWrapper.isForUpdate() ? " FOR UPDATE" : ""));
        return typedThis;
    }

    /**
     * 分页查询
     *
     * @param page 分页参数
     * @param <P>  分页实体类
     * @return 分页
     */
    public <P extends IPage<R>> P page(P page) {
        return page(page, Function.identity());
    }

    /**
     * 分页查询
     *
     * @param page   分页参数
     * @param mapper 转换方法
     * @param <V>    值类型
     * @param <P>    分页实体类
     * @return 分页
     */
    @SuppressWarnings("unchecked")
    public <V, P extends IPage<V>> P page(P page, Function<R, V> mapper) {
//        Page<Map<String, Object>> p = new Page<>(page.getCurrent(), page.getSize());
//        String sqlSelect = queryWrapper.getSqlSelect();
//        if (renameClass == null) {
//            sqlSelect = sqlSelect.split(",")[0].split("(?i) as ")[0] + " AS V";
//        }
//        if (queryWrapper.isDistinct()) {
//            sqlSelect = "DISTINCT " + sqlSelect.trim().replaceAll("^(?i)distinct\\s+", "");
//        }
//        queryWrapper.select(sqlSelect);
//        if (!CollectionUtils.isEmpty(page.orders())) {
//            for (OrderItem orderItem : page.orders()) {
//                queryWrapper.orderBy(true, orderItem.isAsc(), orderItem.getColumn());
//            }
//        }
//        IPage<Map<String, Object>> page1 = baseMapper.page(new Page<>(page.getCurrent(), page.getSize()), queryWrapper);
        ExQueryWrapper<T> countQueryWrapper = queryWrapper.toCountQueryWrapper("C");
        Long total = (Long) baseMapper.list(countQueryWrapper).get(0).get("C");
        if (total > 0) {
            List<ReflectUtils.Property[]> propertiesArr = Arrays.stream(this.renameClass).map(ReflectUtils::getDeclaredProperties).collect(Collectors.toList());
            List<OrderItem> orders = page.orders().stream().map(
                    x -> {
                        OrderItem orderItem = new OrderItem();
                        String column = x.getColumn().replaceAll(StringPool.BACKTICK, "");
                        if (x instanceof LambdaOrderItem) {
                            for (int i = 0; i < this.renameClass.length; i++) {
                                if (ReflectUtils.getGenericClass(this.renameClass[i]).equals(((LambdaOrderItem<?>) x).getClazz())) {
                                    column = StringPool.BACKTICK + column + StringPool.SLASH + (i + 1) + StringPool.BACKTICK;
                                    break;
                                }
                            }
                        } else {
                            boolean b = false;
                            for (int i = 0; i < this.renameClass.length; i++) {
                                if (!Object.class.equals(this.renameClass[i]) && !ReflectUtils.isPrimitive(ReflectUtils.getGenericClass(this.renameClass[i]))) {
                                    for (ReflectUtils.Property property : propertiesArr.get(i)) {
                                        if (property.getName().equals(column)) {
                                            column = StringPool.BACKTICK + column + StringPool.SLASH + (i + 1) + StringPool.BACKTICK;
                                            b = true;
                                            break;
                                        }
                                    }
                                    if (b) {
                                        break;
                                    }
                                } else {
                                    column = StringPool.BACKTICK + column + StringPool.SLASH + (i + 1) + StringPool.BACKTICK;
                                }
                            }
                        }
                        orderItem.setColumn(column);
                        orderItem.setAsc(x.isAsc());
                        return orderItem;
                    }
            ).collect(Collectors.toList());
            ExQueryWrapper<T> pageQueryWrapper = queryWrapper.toPageQueryWrapper(page.getCurrent(), page.getSize(), orders);
            List<Map<String, Object>> pageResult = baseMapper.list(pageQueryWrapper);
            List<V> result;
            if (renameClass.length == 1 && renameClass[0].equals(SingleValue.class)) {
                // 单值类型
                result = pageResult.stream().filter(Objects::nonNull).map(x -> (V) x.entrySet().iterator().next().getValue()).filter(Objects::nonNull).collect(Collectors.toList());
            } else {
                // 实体类型
                result = MybatisUtil.mapStream(pageResult, renameClass).map(getReturnMapper()).map(mapper).collect(Collectors.toList());
            }
            page.setRecords(result);
        }
        page.setTotal(total);
        queryWrapper.reset();
        return page;
    }

}
