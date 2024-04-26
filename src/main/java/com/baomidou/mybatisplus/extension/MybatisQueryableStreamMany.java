package com.baomidou.mybatisplus.extension;

import cn.hutool.core.lang.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.mapper.MysqlBaseMapper;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public class MybatisQueryableStreamMany<T> extends MybatisQueryableStream<T, Object[], MybatisQueryableStreamMany<T>> {

    private final Integer seqNo;

    public MybatisQueryableStreamMany(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Integer seqNo, Type... renameClass) {
        super(entityClass, baseMapper, renameClass);
        this.seqNo = seqNo;
    }

    public MybatisQueryableStreamMany(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Integer seqNo, Type... renameClass) {
        super(queryWrapper, entityClass, baseMapper, renameClass);
        this.seqNo = seqNo;
    }

    public MybatisQueryableStreamMany(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Integer seqNo, TypeReference<?>... renameType) {
        super(entityClass, baseMapper, renameType);
        this.seqNo = seqNo;
    }

    public MybatisQueryableStreamMany(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Integer seqNo, TypeReference<?>... renameType) {
        super(queryWrapper, entityClass, baseMapper, renameType);
        this.seqNo = seqNo;
    }

    @Override
    Function<Object[], Object[]> getReturnMapper() {
        return Function.identity();
    }

    public MybatisQueryableStreamMany<T> appendMap() {
        Type[] newRenameClass = Arrays.copyOf(this.renameClass, this.renameClass.length + 1);
        newRenameClass[this.renameClass.length] = this.entityClass;
        return new MybatisQueryableStreamMany<>(queryWrapper, entityClass, baseMapper, this.seqNo + 1, newRenameClass);
    }

    /**
     * 查询
     *
     * @param select      查询表达式
     * @param renameClass 返回类型
     * @param <R>         返回类型
     * @return 实例本身
     */
    public <R> MybatisQueryableStreamMany<T> map(Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        SelectLambdaQueryWrapper<R> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, this.seqNo.toString());
        if (select != null) {
            select.accept(selectLambda);
        }
        this.renameClass[this.renameClass.length - 1] = renameClass;
        return new MybatisQueryableStreamMany<>(queryWrapper, entityClass, baseMapper, this.seqNo, this.renameClass);
    }

    public <R> MybatisQueryableStreamMany<T> map(Consumer<SelectLambdaQueryWrapper<R>> select, TypeReference<R> renameTypeReference) {
        SelectLambdaQueryWrapper<R> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, this.seqNo.toString());
        if (select != null) {
            select.accept(selectLambda);
        }
        this.renameClass[this.renameClass.length - 1] = renameTypeReference.getType();
        return new MybatisQueryableStreamMany<>(queryWrapper, entityClass, baseMapper, this.seqNo, this.renameClass);
    }

    public <V> MybatisQueryableStreamMany<T> mapToValue(Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        SelectLambdaQueryWrapper<SingleValue<V>> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, this.seqNo.toString());
        selectLambda.selectFunc(selectFunc, SingleValue::getValue);
        this.renameClass[this.renameClass.length - 1] = Object.class;
        return new MybatisQueryableStreamMany<>(queryWrapper, entityClass, baseMapper, this.seqNo, this.renameClass);
    }

    /**
     * 查询指定的某个字段
     *
     * @param selectColumn 查询的字端
     */
    public <R, V> MybatisQueryableStreamMany<T> mapToColumn(SFunction<R, V> selectColumn) {
        return mapToColumn(selectColumn, null);
    }

    public <R, V> MybatisQueryableStreamMany<T> mapToColumn(SFunction<R, V> selectColumn, String rename) {
        return mapToValue(x -> x.column(selectColumn, rename));
    }
}
