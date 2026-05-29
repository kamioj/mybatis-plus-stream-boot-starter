package com.baomidou.mybatisplus.extension.stream;

import org.apache.ibatis.type.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.mapper.StreamBaseMapper;
import com.baomidou.mybatisplus.toolkit.MybatisUtil;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import com.baomidou.mybatisplus.extension.core.ExQueryWrapper;
import com.baomidou.mybatisplus.extension.value.SingleValue;
import com.baomidou.mybatisplus.extension.wrapper.GroupFunctionLambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.wrapper.SelectLambdaQueryWrapper;

public class MybatisQueryableStreamMany<T> extends MybatisQueryableStream<T, Object[], MybatisQueryableStreamMany<T>> {

    private final Integer seqNo;

    public MybatisQueryableStreamMany(Class<T> entityClass, StreamBaseMapper<T> baseMapper, Integer seqNo, Type... renameClass) {
        super(entityClass, baseMapper, renameClass);
        this.seqNo = seqNo;
    }

    public MybatisQueryableStreamMany(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, StreamBaseMapper<T> baseMapper, Integer seqNo, Type... renameClass) {
        super(queryWrapper, entityClass, baseMapper, renameClass);
        this.seqNo = seqNo;
    }

    public MybatisQueryableStreamMany(Class<T> entityClass, StreamBaseMapper<T> baseMapper, Integer seqNo, TypeReference<?>... renameType) {
        super(entityClass, baseMapper, renameType);
        this.seqNo = seqNo;
    }

    public MybatisQueryableStreamMany(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, StreamBaseMapper<T> baseMapper, Integer seqNo, TypeReference<?>... renameType) {
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
        return new MybatisQueryableStreamMany<>(queryWrapper, entityClass, baseMapper, this.seqNo, copyRenameWithLast(renameClass));
    }

    public <R> MybatisQueryableStreamMany<T> map(Consumer<SelectLambdaQueryWrapper<R>> select, TypeReference<R> renameTypeReference) {
        SelectLambdaQueryWrapper<R> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, this.seqNo.toString());
        if (select != null) {
            select.accept(selectLambda);
        }
        return new MybatisQueryableStreamMany<>(queryWrapper, entityClass, baseMapper, this.seqNo, copyRenameWithLast(renameTypeReference.getRawType()));
    }

    public <V> MybatisQueryableStreamMany<T> mapToValue(Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        SelectLambdaQueryWrapper<SingleValue<V>> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, this.seqNo.toString());
        selectLambda.selectFunc(selectFunc, SingleValue::getValue);
        return new MybatisQueryableStreamMany<>(queryWrapper, entityClass, baseMapper, this.seqNo, copyRenameWithLast(Object.class));
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
        SelectLambdaQueryWrapper<SingleValue<V>> selectLambda = new SelectLambdaQueryWrapper<>(queryWrapper, this.seqNo.toString());
        selectLambda.selectFunc(x -> x.column(selectColumn, rename), SingleValue::getValue);
        // 用列的声明 Java 类型作结果映射目标，使方言差异（如达梦 TINYINT→Byte）能在 mapStream 转回 Boolean
        return new MybatisQueryableStreamMany<>(queryWrapper, entityClass, baseMapper, this.seqNo,
            copyRenameWithLast(MybatisUtil.valueTypeOf(selectColumn)));
    }
}
