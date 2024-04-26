package com.baomidou.mybatisplus.extension.bo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 作用：实现map多个key;四个key的功能
 *
 * @param <T1> key1
 * @param <T2> key2
 * @param <T3> key3
 * @param <T4> key4
 */
public class MapKey4<T1, T2, T3, T4> implements Serializable {
    private final T1 key1;
    private final T2 key2;
    private final T3 key3;
    private final T4 key4;

    public MapKey4(T1 key1, T2 key2, T3 key3, T4 key4) {
        this.key1 = key1;
        this.key2 = key2;
        this.key3 = key3;
        this.key4 = key4;
    }

    public T1 getKey1() {
        return key1;
    }

    public T2 getKey2() {
        return key2;
    }

    public T3 getKey3() {
        return key3;
    }

    public T4 getKey4() {
        return key4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapKey4<?, ?, ?, ?> that = (MapKey4<?, ?, ?, ?>) o;
        return Objects.equals(key1, that.key1) && Objects.equals(key2, that.key2) && Objects.equals(key3, that.key3) && Objects.equals(key4, that.key4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key1, key2, key3, key4);
    }
}
