package com.baomidou.mybatisplus.extension.bo;

import java.util.ArrayList;

public class BiList<E> extends ArrayList<E> {

    private static final long serialVersionUID = 5446429520116520654L;

    @Override
    public E get(int index) {
        if (index >= 2 || index < 0) {
            return null;
        } else if (index >= size()) {
            return null;
        }
        return super.get(index);
    }

}
