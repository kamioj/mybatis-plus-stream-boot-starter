package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.lang.invoke.SerializedLambda;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LambdaOrderItem<T> extends OrderItem {
    private final Class<T> clazz;

    public Class<T> getClazz() {
        return clazz;
    }

    @SuppressWarnings("unchecked")
    public <V> LambdaOrderItem(final SFunction<T, V> column, final boolean asc) throws ReflectiveOperationException {
        SerializedLambda serializedLambda = ReflectUtils.getLambda(column);
        if (serializedLambda == null) {
            throw new ReflectiveOperationException();
        }
        this.clazz = (Class<T>) Class.forName(serializedLambda.getImplClass().replace("/", "."));
        this.setColumn(StringPool.BACKTICK + ReflectUtils.getMethodPropertyName(serializedLambda.getImplMethodName()) + StringPool.BACKTICK);
        this.setAsc(asc);
    }

    public static <T, V> LambdaOrderItem<T> asc(SFunction<T, V> column) throws ReflectiveOperationException {
        return build(column, true);
    }

    public static <T, V> LambdaOrderItem<T> desc(SFunction<T, V> column) throws ReflectiveOperationException {
        return build(column, false);
    }

    @SafeVarargs
    public static <T> List<LambdaOrderItem<T>> ascs(SFunction<T, ?>... columns) {
        return Arrays.stream(columns).map(x -> {
            try {
                return LambdaOrderItem.asc(x);
            } catch (ReflectiveOperationException ignored) {
            }
            return null;
        }).collect(Collectors.toList());
    }

    @SafeVarargs
    public static <T> List<LambdaOrderItem<T>> descs(SFunction<T, ?>... columns) {
        return Arrays.stream(columns).map(x -> {
            try {
                return LambdaOrderItem.desc(x);
            } catch (ReflectiveOperationException ignored) {
            }
            return null;
        }).collect(Collectors.toList());
    }

    private static <T, V> LambdaOrderItem<T> build(SFunction<T, V> column, boolean asc) throws ReflectiveOperationException {
        return new LambdaOrderItem<>(column, asc);
    }
}
