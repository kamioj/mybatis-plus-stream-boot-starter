package com.baomidou.mybatisplus.extension.bo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 作用：实现map多个key;两个key的功能
 *
 * @param <T1> key1
 * @param <T2> key2
 */
public class BiMapKey<T1, T2> implements Serializable {
    private final T1 key1;
    private final T2 key2;

    public BiMapKey(T1 key1, T2 key2) {
        this.key1 = key1;
        this.key2 = key2;
    }

    public T1 getKey1() {
        return key1;
    }

    public T2 getKey2() {
        return key2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiMapKey<?, ?> that = (BiMapKey<?, ?>) o;
        return Objects.equals(key1, that.key1) && Objects.equals(key2, that.key2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key1, key2);
    }
}
