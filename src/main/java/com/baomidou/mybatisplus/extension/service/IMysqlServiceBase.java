package com.baomidou.mybatisplus.extension.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.*;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IMysqlServiceBase<T> extends IService<T> {


    /**
     * 获取流
     *
     * @return 流
     */
    MybatisQueryableStream1<T, T> stream();

    /**
     * 获取更新流
     *
     * @return 流
     */
    MybatisExecutableStream<T> executableStream();


    boolean exist(Consumer<NormalWhereLambdaQueryWrapper> predicate);

    /**
     * 获取数量
     *
     * @param predicate 匹配条件
     * @return 满足条件数量
     */
    int count(Consumer<NormalWhereLambdaQueryWrapper> predicate);

    /**
     * 获取实体
     *
     * @param eqColumn 匹配列
     * @param eqValue  匹配值/匹配集合
     * @param <U>      匹配值类型
     * @return 实体
     */
    <U> T get(SFunction<T, U> eqColumn, Object eqValue);

    /**
     * 获取实体
     *
     * @param eqColumn 匹配列
     * @param eqValue  匹配值/匹配集合
     * @param <U>      匹配值类型
     * @return 实体
     */
    <U> T getOrDefault(SFunction<T, U> eqColumn, Object eqValue, T defaultValue);

    /**
     * 获取实体
     *
     * @param predicate 匹配条件
     * @return 实体
     */
    T get(Consumer<NormalWhereLambdaQueryWrapper> predicate);

    /**
     * 获取实体
     *
     * @param predicate 匹配条件
     * @return 实体
     */
    T getOrDefault(Consumer<NormalWhereLambdaQueryWrapper> predicate, T defaultValue);

    /**
     * 获取返回实体
     *
     * @param predicate   匹配条件
     * @param select      选择表达式
     * @param renameClass 返回实体类型
     * @param <R>         返回实体类型
     * @return 返回实体
     */
    <R> R get(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 锁行获取实体
     *
     * @param key 实体主键
     * @return 实体
     */
    T getByKeyForUpdate(Object key);

    /**
     * 锁行获取实体
     *
     * @param entity 实体
     * @return 实体
     */
    T getByEntityForUpdate(T entity);

    /**
     * 获取值
     *
     * @param eqColumn     匹配列
     * @param eqValue      匹配值/匹配集合
     * @param selectColumn 选择列
     * @param <U>          匹配值类型
     * @param <V>          值类型
     * @return 值
     */
    <U, V> V getValue(SFunction<T, U> eqColumn, Object eqValue, SFunction<T, V> selectColumn);

    /**
     * 获取值
     *
     * @param predicate    匹配条件
     * @param selectColumn 选择列
     * @param <V>          值类型
     * @return 值
     */
    <V> V getValue(Consumer<NormalWhereLambdaQueryWrapper> predicate, SFunction<T, V> selectColumn);

    /**
     * 获取值
     *
     * @param predicate  匹配条件
     * @param selectFunc 选择函数表达式
     * @param <V>        值类型
     * @return 值
     */
    <V> V getValue(Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc);

    /**
     * 获取集合
     *
     * @param eqColumn 匹配列
     * @param eqValue  匹配值/匹配集合
     * @param <U>      匹配值类型
     * @return 实体集合
     */
    <U> List<T> list(SFunction<T, U> eqColumn, Object eqValue);

    /**
     * 获取集合
     *
     * @param predicate 匹配条件
     * @return 实体集合
     */
    List<T> list(Consumer<NormalWhereLambdaQueryWrapper> predicate);

    /**
     * 获取返回实体集合
     *
     * @param predicate   匹配条件
     * @param select      选择表达式
     * @param renameClass 返回实体类型
     * @param <R>         返回实体类型
     * @return 返回实体集合
     */
    <R> List<R> list(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 获取集合
     *
     * @param predicate 匹配条件
     * @param order     排序表达式
     * @param limit     查询条数
     * @return 实体集合
     */
    List<T> list(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit);

    /**
     * 获取返回实体集合
     *
     * @param predicate   匹配条件
     * @param order       排序表达式
     * @param limit       查询条数
     * @param select      选择表达式
     * @param renameClass 返回实体类型
     * @param <R>         返回实体类型
     * @return 返回实体集合
     */
    <R> List<R> list(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 获取值集合
     *
     * @param eqColumn     匹配列
     * @param eqValue      匹配值/匹配集合
     * @param selectColumn 选择列
     * @param <U>          匹配值类型
     * @param <V>          值类型
     * @return 值集合
     */
    <U, V> List<V> listValues(SFunction<T, U> eqColumn, Object eqValue, SFunction<T, V> selectColumn);

    /**
     * 获取值集合
     *
     * @param predicate    匹配条件
     * @param selectColumn 选择列
     * @param <V>          值类型
     * @return 值集合
     */
    <V> List<V> listValues(Consumer<NormalWhereLambdaQueryWrapper> predicate, SFunction<T, V> selectColumn);

    /**
     * 获取值集合
     *
     * @param predicate  匹配条件
     * @param selectFunc 选择函数表达式
     * @param <V>        值类型
     * @return 值集合
     */
    <V> List<V> listValues(Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc);

    /**
     * 获取值集合
     *
     * @param predicate    匹配条件
     * @param order        排序表达式
     * @param limit        查询条数
     * @param selectColumn 选择列
     * @param <V>          值类型
     * @return 值集合
     */
    <V> List<V> listValues(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, SFunction<T, V> selectColumn);

    /**
     * 获取值集合
     *
     * @param predicate  匹配条件
     * @param order      排序表达式
     * @param limit      查询条数
     * @param selectFunc 选择函数表达式
     * @param <V>        值类型
     * @return 值集合
     */
    <V> List<V> listValues(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc);

    /**
     * 连表获取实体集合
     *
     * @param joinPredicate 连表条件
     * @param predicate     匹配条件
     * @return 实体集合
     */
    List<T> listJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate);

    /**
     * 连表获取返回实体集合
     *
     * @param joinPredicate 连表条件
     * @param predicate     匹配条件
     * @param select        选择表达式
     * @param renameClass   返回实体类型
     * @param <R>           返回实体类型
     * @return 返回实体集合
     */
    <R> List<R> listJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 连表获取实体集合
     *
     * @param joinPredicate 连表条件
     * @param predicate     匹配条件
     * @param order         排序表达式
     * @param limit         查询条数
     * @return 实体集合
     */
    List<T> listJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit);

    /**
     * 连表获取返回实体集合
     *
     * @param joinPredicate 连表条件
     * @param predicate     匹配条件
     * @param order         排序表达式
     * @param limit         查询条数
     * @param select        选择表达式
     * @param renameClass   返回实体类型
     * @param <R>           返回实体类型
     * @return 返回实体集合
     */
    <R> List<R> listJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 连表获取值集合
     *
     * @param joinPredicate 连表条件
     * @param predicate     匹配条件
     * @param selectFunc    选择函数表达式
     * @param <V>           值类型
     * @return 值集合
     */
    <V> List<V> listJoinValues(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc);


    /**
     * 连表获取值集合
     *
     * @param joinPredicate 连表条件
     * @param predicate     匹配条件
     * @param order         排序表达式
     * @param limit         查询条数
     * @param selectFunc    选择函数表达式
     * @param <V>           值类型
     * @return 值集合
     */
    <V> List<V> listJoinValues(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc);

    /**
     * 分组获取返回实体集合
     *
     * @param group       分组表达式
     * @param predicate   匹配条件
     * @param select      选择表达式
     * @param renameClass 返回实体类型
     * @param <R>         返回实体类型
     * @return 返回实体集合
     */
    <R> List<R> listGroup(Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 分组获取返回实体集合
     *
     * @param group       分组表达式
     * @param predicate   匹配条件
     * @param order       排序表达式
     * @param limit       查询条数
     * @param select      选择表达式
     * @param renameClass 返回实体类型
     * @param <R>         返回实体类型
     * @return 返回实体集合
     */
    <R> List<R> listGroup(Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 分组获取值集合
     *
     * @param group      分组表达式
     * @param predicate  匹配条件
     * @param selectFunc 选择函数表达式
     * @param <V>        值类型
     * @return 值集合
     */
    <V> List<V> listGroupValues(Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc);

    /**
     * 分组获取值集合
     *
     * @param group      分组表达式
     * @param predicate  匹配条件
     * @param order      排序表达式
     * @param limit      查询条数
     * @param selectFunc 选择函数表达式
     * @param <V>        值类型
     * @return 值集合
     */
    <V> List<V> listGroupValues(Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc);

    /**
     * 连表分组获取返回实体集合
     *
     * @param joinPredicate 连表条件
     * @param group         分组表达式
     * @param predicate     匹配条件
     * @param select        选择表达式
     * @param renameClass   返回实体类型
     * @param <R>           返回实体类型
     * @return 返回实体集合
     */
    <R> List<R> listGroupJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 连表分组获取返回实体集合
     *
     * @param joinPredicate 连表条件
     * @param group         分组表达式
     * @param predicate     匹配条件
     * @param order         排序表达式
     * @param limit         查询条数
     * @param select        选择表达式
     * @param renameClass   返回实体类型
     * @param <R>           返回实体类型
     * @return 返回实体集合
     */
    <R> List<R> listGroupJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 连表分组获取值集合
     *
     * @param joinPredicate 连表条件
     * @param group         分组表达式
     * @param predicate     匹配条件
     * @param selectFunc    选择函数表达式
     * @param <V>           值类型
     * @return 值集合
     */
    <V> List<V> listGroupJoinValues(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc);

    /**
     * 连表分组获取值集合
     *
     * @param joinPredicate 连表条件
     * @param group         分组表达式
     * @param predicate     匹配条件
     * @param order         排序表达式
     * @param limit         查询条数
     * @param selectFunc    选择函数表达式
     * @param <V>           值类型
     * @return 值集合
     */
    <V> List<V> listGroupJoinValues(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc);

    /**
     * 分页查询
     *
     * @param page      分页参数
     * @param predicate 匹配条件
     * @return 分页结果
     */
    IPage<T> page(IPage<T> page, Consumer<NormalWhereLambdaQueryWrapper> predicate);

    /**
     * 分页查询
     *
     * @param page        分页参数
     * @param predicate   匹配条件
     * @param select      选择表达式
     * @param renameClass 返回实体类型
     * @param <R>         返回实体类型
     * @return 分页结果
     */
    <R> IPage<R> page(IPage<R> page, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 连表分页查询
     *
     * @param page          分页参数
     * @param joinPredicate 连表条件
     * @param predicate     匹配条件
     * @param select        选择表达式
     * @param renameClass   返回实体类型
     * @param <R>           返回实体类型
     * @return 分页结果
     */
    <R> IPage<R> pageJoin(IPage<R> page, Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 分组分页查询
     *
     * @param page        分页参数
     * @param group       分组表达式
     * @param predicate   匹配条件
     * @param select      选择表达式
     * @param renameClass 返回实体类型
     * @param <R>         返回实体类型
     * @return 分页结果
     */
    <R> IPage<R> pageGroup(IPage<R> page, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 连表分组分页查询
     *
     * @param page          分页参数
     * @param joinPredicate 连表条件
     * @param group         分组表达式
     * @param predicate     匹配条件
     * @param select        选择表达式
     * @param renameClass   返回实体类型
     * @param <R>           返回实体类型
     * @return 分页结果
     */

    <R> IPage<R> pageGroupJoin(IPage<R> page, Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass);

    /**
     * 批量插入实体
     *
     * @param entityList 实体集合
     * @return 成功条数
     */
    int saveBatchWithoutId(Collection<T> entityList);

    /**
     * 去重插入实体
     * 如果是新增一条数据,则影响+1
     * 如果是更新一条数据,先执行插入,影响行数+1,插入失败执行更新,如果更新成功,则影响行数再追加1
     *
     * @param entityList   实体集合
     * @param duplicateSet 去重更新表达式
     * @return 成功条数（插入算1，去重更新算2）
     */
    int saveDuplicate(Collection<T> entityList, Consumer<DuplicateSetLambdaQueryWrapper<T>> duplicateSet);

    /**
     * 忽略插入实体
     *
     * @param entityList 实体集合
     * @return 成功条数
     */
    int saveIgnore(Collection<T> entityList);

    /**
     * 替换插入实体
     *
     * @param entityList 实体集合
     * @return 成功条数
     */
    int saveReplace(Collection<T> entityList);

    /**
     * 删除
     *
     * @param predicate 匹配条件
     * @return 成功条数
     */
    int remove(Consumer<NormalWhereLambdaQueryWrapper> predicate);

    /**
     * 更新
     *
     * @param setter    更新表达式
     * @param predicate 匹配条件
     * @return 成功条数
     */
    int update(Consumer<NormalSetLambdaQueryWrapper> setter, Consumer<NormalWhereLambdaQueryWrapper> predicate);

    /**
     * 多表联合更新
     *
     * @param joinPredicate 连表条件
     * @param setter        更新表达式
     * @param predicate     匹配条件
     * @return 成功条数
     */
    int updateJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalSetLambdaQueryWrapper> setter, Consumer<NormalWhereLambdaQueryWrapper> predicate);

}
