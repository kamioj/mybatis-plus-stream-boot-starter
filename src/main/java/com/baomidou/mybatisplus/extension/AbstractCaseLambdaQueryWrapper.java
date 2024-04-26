package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 抽象Case语句表达式包装
 *
 * @param <W>        Where语句表达式包装类型
 * @param <F>        函数表达式包装类型
 * @param <V>        值类型
 * @param <Children> 子类类型
 */
public abstract class AbstractCaseLambdaQueryWrapper<W extends AbstractWhereLambdaQueryWrapper<F, W>,
        F extends AbstractFunctionLambdaQueryWrapper<W, F>,
        V,
        Children extends AbstractCaseLambdaQueryWrapper<W, F, V, Children>>
        extends AbstractSubLambdaQueryWrapper<Children> {
    protected final Class<W> wClazz;
    protected final Class<F> fClazz;

    private final List<String> whenList;
    private String elseValue;

    public AbstractCaseLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
        this.whenList = new ArrayList<>();

        wClazz = ReflectUtils.getGenericClass(getClass().getGenericSuperclass(), AbstractCaseLambdaQueryWrapper.class, 0);
        fClazz = ReflectUtils.getGenericClass(getClass().getGenericSuperclass(), AbstractCaseLambdaQueryWrapper.class, 1);
    }

    /**
     * 获取case语句sql并传入注入参数
     *
     * @param queryWrapper 待传入注入参数的queryWrapper
     * @return case语句sql
     */
    @Override
    public String getSqlSegment(ExQueryWrapper<?> queryWrapper) {
        StringBuilder sql = new StringBuilder("CASE");
        for (String when : whenList) {
            sql.append(" ").append(when);
        }
        if (!StringUtils.isEmpty(elseValue)) {
            sql.append(" ELSE ").append(elseValue);
        }
        sql.append(" END");

        return super.getSqlSegmentWithParam(sql.toString(), getQueryWrapper(), queryWrapper);
    }

    /**
     * case语句when/then
     *
     * @param caseWhen when条件
     * @param column   then列
     * @param <T>      列类型
     * @return 实例本身
     */
    public <T> Children whenThenColumn(Consumer<W> caseWhen, SFunction<T, V> column) {
        return whenThenFunc(caseWhen, x -> x.column(column));
    }

    /**
     * case语句when/then
     *
     * @param caseWhen      when条件
     * @param caseThenValue then值
     * @return 实例本身
     */
    public Children whenThenValue(Consumer<W> caseWhen, V caseThenValue) {
        return whenThenFunc(caseWhen, x -> x.value(caseThenValue));
    }

    /**
     * case语句when/then
     *
     * @param caseWhen     when条件
     * @param caseThenFunc then表达式
     * @return 实例本身
     */
    public Children whenThenFunc(Consumer<W> caseWhen, Function<F, V> caseThenFunc) {
        try {
            String subSqlSegment = this.getSubSqlSegment(caseWhen, wClazz);
            whenList.add("WHEN " + subSqlSegment + " THEN " + getSubSqlSegment(caseThenFunc, fClazz));
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * case语句else
     *
     * @param column else列
     * @param <T>    列类型
     * @return 实例本身
     */
    public <T> Children elseColumn(SFunction<T, V> column) {
        return elseFunc(x -> x.column(column));
    }

    /**
     * case语句else
     *
     * @param caseElseValue else值
     * @return 实例本身
     */
    public Children elseValue(V caseElseValue) {
        return elseFunc(x -> x.value(caseElseValue));
    }

    /**
     * case语句else
     *
     * @param caseElseFunc else表达式
     * @return 实例本身
     */
    public Children elseFunc(Function<F, V> caseElseFunc) {
        try {
            elseValue = getSubSqlSegment(caseElseFunc, fClazz);
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }
}
