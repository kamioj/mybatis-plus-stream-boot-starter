package com.baomidou.mybatisplus.extension;

import java.util.function.Consumer;

/**
 * 子语句表达式包装
 */
public final class SingleValueSubSqlLambdaQueryWrapper<V> extends AbstractSubSqlLambdaQueryWrapper<SingleValueSubSqlLambdaQueryWrapper<V>> {

    public SingleValueSubSqlLambdaQueryWrapper() {
        this(null);
    }

    public SingleValueSubSqlLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

    /**
     * 查询
     *
     * @param select 查询表达式
     * @return 实例本身
     */
    public SingleValueSubSqlLambdaQueryWrapper<V> select(Consumer<SelectLambdaQueryWrapper<SingleValue<V>>> select) {
        SelectLambdaQueryWrapper<SingleValue<V>> selectLambda = new SelectLambdaQueryWrapper<>(getQueryWrapper());
        if (select != null) {
            select.accept(selectLambda);
        }
        return typedThis;
    }

}
