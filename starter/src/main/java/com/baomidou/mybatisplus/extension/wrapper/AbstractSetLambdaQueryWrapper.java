package com.baomidou.mybatisplus.extension.wrapper;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.util.function.Function;
import com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper;
import com.baomidou.mybatisplus.extension.core.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.dialect.SetterClause;

/**
 * 赋值表达式包装
 */
public abstract class AbstractSetLambdaQueryWrapper<F extends AbstractFunctionLambdaQueryWrapper<?, F>,
        Children extends AbstractSetLambdaQueryWrapper<F, Children>>
        extends LambdaQueryWrapper<Children> {
    protected final Class<F> fClazz;

    public AbstractSetLambdaQueryWrapper() {
        this(new ExecutableQueryWrapper<>());
    }

    public AbstractSetLambdaQueryWrapper(ExecutableQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
        fClazz = ReflectUtils.getGenericClass(getClass().getGenericSuperclass(), AbstractSetLambdaQueryWrapper.class, 0);
    }

    @Override
    public ExecutableQueryWrapper<?> getQueryWrapper() {
        return (ExecutableQueryWrapper<?>) super.getQueryWrapper();
    }

    /**
     * 赋值
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children set(Boolean condition, SFunction<T, V> column, V val) {
        return setFunc(condition, column, x -> x.value(val));
    }

    /**
     * 赋值
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children set(SFunction<T, V> column, V val) {
        return setFunc(true, column, x -> x.value(val));
    }

    /**
     * 赋值
     *
     * @param column 列
     * @param func   函数表达式
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children setFunc(SFunction<T, V> column, Function<F, V> func) {
        return setFunc(true, column, func);
    }

    /**
     * 赋值
     *
     * @param column 列
     * @param func   函数表达式
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children setFunc(Boolean condition, SFunction<T, V> column, Function<F, V> func) {
        if (condition != null && !condition) {
            return typedThis;
        }
        try {
            // Phase 4: 构造结构化 setter（裸表名 + 裸列名 + 方言中立 valueExpr）
            String[] qc = getQualifierAndColumn(column);
            getQueryWrapper().addSetter(new SetterClause(qc[0], qc[1], getSubSqlSegment(func, fClazz)));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

}
