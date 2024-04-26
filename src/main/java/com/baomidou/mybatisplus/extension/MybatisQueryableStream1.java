package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.mapper.MysqlBaseMapper;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;
import org.apache.ibatis.type.TypeReference;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public class MybatisQueryableStream1<T, R> extends MybatisQueryableStream<T, R, MybatisQueryableStream1<T, R>> {

    public MybatisQueryableStream1(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Type... renameClass) {
        super(entityClass, baseMapper, renameClass);
    }

    public MybatisQueryableStream1(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, Type... renameClass) {
        super(queryWrapper, entityClass, baseMapper, renameClass);
    }

    public MybatisQueryableStream1(Class<T> entityClass, MysqlBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        super(entityClass, baseMapper, renameType);
    }

    public MybatisQueryableStream1(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        super(queryWrapper, entityClass, baseMapper, renameType);
    }

    public MybatisQueryableStream2<T, R, T> appendMap() {
        Type[] newRenameClass = Arrays.copyOf(this.renameClass, this.renameClass.length + 1);
        newRenameClass[this.renameClass.length] = this.entityClass;
        return new MybatisQueryableStream2<>(queryWrapper, entityClass, baseMapper, newRenameClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    Function<Object[], R> getReturnMapper() {
        return x -> (R) x[0];
    }

    /**
     * 查询
     *
     * @param select      查询表达式
     * @param renameClass 返回类型
     * @param <R2>        返回类型
     * @return 实例本身
     */
    public <R2> MybatisQueryableStream1<T, R2> map(Consumer<SelectLambdaQueryWrapper<R2>> select, Class<R2> renameClass) {
        SelectLambdaQueryWrapper<R2> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, "1");
        if (select != null) {
            select.accept(selectLambda);
        }
        return new MybatisQueryableStream1<>(queryWrapper, entityClass, baseMapper, renameClass);
    }

    public <R2> MybatisQueryableStream1<T, R2> map(Consumer<SelectLambdaQueryWrapper<R2>> select, TypeReference<R2> renameTypeReference) {
        SelectLambdaQueryWrapper<R2> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, "1");
        if (select != null) {
            select.accept(selectLambda);
        }
        return new MybatisQueryableStream1<>(queryWrapper, entityClass, baseMapper,((Type) ReflectUtils.invokeMethod(renameTypeReference, "getSuperclassTypeParameter")));
    }

    public <V> MybatisQueryableStream1<T, V> mapToValue(Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        SelectLambdaQueryWrapper<SingleValue<V>> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, "1");
        selectLambda.selectFunc(selectFunc, SingleValue::getValue);
        return new MybatisQueryableStream1<>(queryWrapper, entityClass, baseMapper, Object.class);
    }

    /**
     * 查询指定的某个字段
     *
     * @param selectColumn 查询的字端
     */
    public <R2, V> MybatisQueryableStream1<T, V> mapToColumn(SFunction<R2, V> selectColumn) {
        return mapToColumn(selectColumn, null);
    }

    public <R2, V> MybatisQueryableStream1<T, V> mapToColumn(SFunction<R2, V> selectColumn, String tableRename) {
        return mapToValue(x -> x.column(selectColumn, tableRename));
    }
}
