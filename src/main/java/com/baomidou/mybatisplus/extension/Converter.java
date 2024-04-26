package com.baomidou.mybatisplus.extension;


import com.baomidou.mybatisplus.toolkit.ReflectUtils;

/**
 * 类型转换器
 *
 * @param <T> 源类型
 * @param <R> 目标类型
 */
public abstract class Converter<T, R> {
    public final Class<T> fromClass = ReflectUtils.getGenericClass(getClass().getGenericSuperclass(), Converter.class, 0);
    public final Class<R> toClass = ReflectUtils.getGenericClass(getClass().getGenericSuperclass(), Converter.class, 1);

    public abstract R convert(T o);
}

