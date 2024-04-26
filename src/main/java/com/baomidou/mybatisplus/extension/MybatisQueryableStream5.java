package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.bo.MapKey5;
import com.baomidou.mybatisplus.extension.mapper.MysqlBaseMapper;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;
import org.apache.ibatis.type.TypeReference;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public class MybatisQueryableStream5<T, R1, R2, R3, R4, R5> extends MybatisQueryableStream<T, MapKey5<R1, R2, R3, R4, R5>, MybatisQueryableStream5<T, R1, R2, R3, R4, R5>> {

    public MybatisQueryableStream5(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Type... renameClass) {
        super(entityClass, baseMapper, renameClass);
    }

    public MybatisQueryableStream5(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Type... renameClass) {
        super(queryWrapper, entityClass, baseMapper, renameClass);
    }

    public MybatisQueryableStream5(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        super(entityClass, baseMapper, renameType);
    }

    public MybatisQueryableStream5(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        super(queryWrapper, entityClass, baseMapper, renameType);
    }

    @SuppressWarnings("unchecked")
    @Override
    Function<Object[], MapKey5<R1, R2, R3, R4, R5>> getReturnMapper() {
        return x -> new MapKey5<>((R1) x[0], (R2) x[1], (R3) x[2], (R4) x[3], (R5) x[4]);
    }

    public MybatisQueryableStreamMany<T> appendMap() {
        Type[] newRenameClass = Arrays.copyOf(this.renameClass, this.renameClass.length + 1);
        newRenameClass[this.renameClass.length] = this.entityClass;
        return new MybatisQueryableStreamMany<>(queryWrapper, entityClass, baseMapper, 6, newRenameClass);
    }

    /**
     * 查询
     *
     * @param select      查询表达式
     * @param renameClass 返回类型
     * @param <R>         返回类型
     * @return 实例本身
     */
    public <R> MybatisQueryableStream5<T, R1, R2, R3, R4, R> map(Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        SelectLambdaQueryWrapper<R> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, "5");
        if (select != null) {
            select.accept(selectLambda);
        }
        this.renameClass[this.renameClass.length - 1] = renameClass;
        return new MybatisQueryableStream5<>(queryWrapper, entityClass, baseMapper, this.renameClass);
    }

    public <R> MybatisQueryableStream5<T, R1, R2, R3, R4, R> map(Consumer<SelectLambdaQueryWrapper<R>> select, TypeReference<R> renameTypeReference) {
        SelectLambdaQueryWrapper<R> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, "5");
        if (select != null) {
            select.accept(selectLambda);
        }
        this.renameClass[this.renameClass.length - 1] = ((Type) ReflectUtils.invokeMethod(renameTypeReference, "getSuperclassTypeParameter"));
        return new MybatisQueryableStream5<>(queryWrapper, entityClass, baseMapper, this.renameClass);
    }

    public <V> MybatisQueryableStream5<T, R1, R2, R3, R4, V> mapToValue(Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        SelectLambdaQueryWrapper<SingleValue<V>> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, "5");
        selectLambda.selectFunc(selectFunc, SingleValue::getValue);
        this.renameClass[this.renameClass.length - 1] = Object.class;
        return new MybatisQueryableStream5<>(queryWrapper, entityClass, baseMapper, this.renameClass);
    }

    /**
     * 查询指定的某个字段
     *
     * @param selectColumn 查询的字端
     */
    public <R, V> MybatisQueryableStream5<T, R1, R2, R3, R4, V> mapToColumn(SFunction<R, V> selectColumn) {
        return mapToColumn(selectColumn, null);
    }

    public <R, V> MybatisQueryableStream5<T, R1, R2, R3, R4, V> mapToColumn(SFunction<R, V> selectColumn, String rename) {
        return mapToValue(x -> x.column(selectColumn, rename));
    }
}
