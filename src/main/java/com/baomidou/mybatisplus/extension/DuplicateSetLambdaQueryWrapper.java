package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.toolkit.MybatisUtil;

/**
 * 去重赋值表达式包装
 *
 * @param <T> 实体类型
 */
public final class DuplicateSetLambdaQueryWrapper<T> extends AbstractSetLambdaQueryWrapper<DuplicateFunctionLambdaQueryWrapper<T>, DuplicateSetLambdaQueryWrapper<T>> {

    public DuplicateSetLambdaQueryWrapper(Class<T> clazz) {
        this(new ExecutableQueryWrapper<>(clazz), clazz);
    }

    public DuplicateSetLambdaQueryWrapper(ExecutableQueryWrapper<?> queryWrapper, Class<T> clazz) {
        super(queryWrapper);
        getQueryWrapper().setFromTable(MybatisUtil.getTableInfo(clazz), null);
    }

    /**
     * 重复修改
     *
     * @param columns 重复修改列
     * @return 实例本身
     */
    @SafeVarargs
    public final DuplicateSetLambdaQueryWrapper<T> duplicate(SFunction<T, ?>... columns) {
        for (SFunction<T, ?> column : columns) {
            setFunc(column, x -> x.duplicateValue(column));
        }
        return typedThis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExecutableQueryWrapper<T> getQueryWrapper() {
        return ((ExecutableQueryWrapper<T>) super.getQueryWrapper());
    }

}
