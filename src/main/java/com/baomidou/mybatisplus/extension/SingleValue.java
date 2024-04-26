package com.baomidou.mybatisplus.extension;

import java.io.Serializable;

public class SingleValue<V> implements Serializable {
    private V value;

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}
