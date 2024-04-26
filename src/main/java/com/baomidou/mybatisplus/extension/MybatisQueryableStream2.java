package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.bo.BiMapKey;
import com.baomidou.mybatisplus.extension.mapper.MysqlBaseMapper;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;
import org.apache.ibatis.type.TypeReference;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public class MybatisQueryableStream2<T, R1, R2> extends MybatisQueryableStream<T, BiMapKey<R1, R2>, MybatisQueryableStream2<T, R1, R2>> {

    public MybatisQueryableStream2(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Type... renameClass) {
        super(entityClass, baseMapper, renameClass);
    }

    public MybatisQueryableStream2(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Type... renameClass) {
        super(queryWrapper, entityClass, baseMapper, renameClass);
    }

    public MybatisQueryableStream2(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        super(entityClass, baseMapper, renameType);
    }

    public MybatisQueryableStream2(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        super(queryWrapper, entityClass, baseMapper, renameType);
    }

    @SuppressWarnings("unchecked")
    @Override
    Function<Object[], BiMapKey<R1, R2>> getReturnMapper() {
        return x -> new BiMapKey<>((R1) x[0], (R2) x[1]);
    }

    public MybatisQueryableStream3<T, R1, R2, T> appendMap() {
        Type[] newRenameClass = Arrays.copyOf(this.renameClass, this.renameClass.length + 1);
        newRenameClass[this.renameClass.length] = this.entityClass;
        return new MybatisQueryableStream3<>(queryWrapper, entityClass, baseMapper, newRenameClass);
    }

    /**
     * 查询
     *
     * @param select      查询表达式
     * @param renameClass 返回类型
     * @param <R>         返回类型
     * @return 实例本身
     */
    public <R> MybatisQueryableStream2<T, R1, R> map(Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        SelectLambdaQueryWrapper<R> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, "2");
        if (select != null) {
            select.accept(selectLambda);
        }
        this.renameClass[this.renameClass.length - 1] = renameClass;
        return new MybatisQueryableStream2<>(queryWrapper, entityClass, baseMapper, this.renameClass);
    }

    public <R> MybatisQueryableStream2<T, R1, R> map(Consumer<SelectLambdaQueryWrapper<R>> select, TypeReference<R> renameTypeReference) {
        SelectLambdaQueryWrapper<R> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, "2");
        if (select != null) {
            select.accept(selectLambda);
        }
        this.renameClass[this.renameClass.length - 1] = ((Type) ReflectUtils.invokeMethod(renameTypeReference, "getSuperclassTypeParameter"));
        return new MybatisQueryableStream2<>(queryWrapper, entityClass, baseMapper, this.renameClass);
    }

    public <V> MybatisQueryableStream2<T, R1, V> mapToValue(Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        SelectLambdaQueryWrapper<SingleValue<V>> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, "2");
        selectLambda.selectFunc(selectFunc, SingleValue::getValue);
        this.renameClass[this.renameClass.length - 1] = Object.class;
        return new MybatisQueryableStream2<>(queryWrapper, entityClass, baseMapper, this.renameClass);
    }

    /**
     * 查询指定的某个字段
     *
     * @param selectColumn 查询的字端
     */
    public <R, V> MybatisQueryableStream2<T, R1, V> mapToColumn(SFunction<R, V> selectColumn) {
        return mapToColumn(selectColumn, null);
    }

    public <R, V> MybatisQueryableStream2<T, R1, V> mapToColumn(SFunction<R, V> selectColumn, String rename) {
        return mapToValue(x -> x.column(selectColumn, rename));
    }
}
