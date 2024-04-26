package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 抽象函数表达式包装
 *
 * @param <W>
 * @param <Children> 子类类型
 */
public abstract class AbstractFunctionLambdaQueryWrapper<W extends AbstractWhereLambdaQueryWrapper<Children, W>,
        Children extends AbstractFunctionLambdaQueryWrapper<W, Children>>
        extends AbstractSubLambdaQueryWrapper<Children> {
    private final Class<W> wClazz;

    protected String sqlSegment;

    public AbstractFunctionLambdaQueryWrapper() {
        super();
        wClazz = ReflectUtils.getGenericClass(getClass().getGenericSuperclass(), AbstractFunctionLambdaQueryWrapper.class, 0);
    }

    /**
     * 获取sql，并添加输入参数
     *
     * @param queryWrapper queryWrapper
     * @return sqlSegment
     */
    @Override
    public String getSqlSegment(ExQueryWrapper<?> queryWrapper) {
        return getSqlSegmentWithParam(sqlSegment, getQueryWrapper(), queryWrapper);
    }

    /**
     * 空（不可单独使用）
     */
    public void empty() {
        sqlSegment = "";
    }

    /**
     * 空值
     */
    public <V> V _null() {
        sqlSegment = "NULL";
        return null;
    }

    /**
     * 保留字段
     *
     * @param keyword 保留字段（',' 'DISTINCT' 'ORDER BY' ...）
     */
    protected void keyword(String keyword) {
        sqlSegment = keyword;
    }

    /**
     * 值
     *
     * @param val 值
     * @param <V> 值类型
     * @return 值类型
     */
    public <V> V value(V val) {
        ExQueryWrapper<?> queryWrapper = new ExQueryWrapper<>();
        queryWrapper.apply("{0}", val);
        sqlSegment = getSqlSegmentWithParam(queryWrapper.getSqlSegment(), queryWrapper);
        sqlSegment = StringUtils.strip(sqlSegment, "()");
        return null;
    }

    /**
     * 值
     *
     * @param subSql 子查询
     * @param <V>    值类型
     * @return 值类型
     */
    public <V> V valueSubSql(Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        SingleValueSubSqlLambdaQueryWrapper<V> subSqlLambda = new SingleValueSubSqlLambdaQueryWrapper<>();
        subSql.accept(subSqlLambda);
        sqlSegment = "(" + subSqlLambda.getSqlSegment(getQueryWrapper()) + ")";
        return null;
    }

    /**
     * 集合（不可单独使用）
     *
     * @param col 集合
     */
    public <V> ArrayList<V> values(Collection<V> col) {
        ExQueryWrapper<?> queryWrapper = new ExQueryWrapper<>();
        queryWrapper.apply(IntStream.range(0, col.size()).mapToObj(x -> "{" + x + "}").collect(Collectors.joining(",")), col.toArray());
        sqlSegment = getSqlSegmentWithParam(queryWrapper.getSqlSegment(), queryWrapper);
        return null;
    }

    /**
     * 集合（不可单独使用）
     *
     * @param val 集合
     * @param <V> 值类型
     * @return 值集合
     */
    @SafeVarargs
    public final <V> ArrayList<V> values(V... val) {
        return values(Arrays.stream(val).collect(Collectors.toList()));
    }

    /**
     * 集合（不可单独使用）
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 值集合
     */
    @SuppressWarnings("unchecked")
    public <V> ArrayList<V> valuesFunc(Function<Children, V>... func) {
        return valuesFunc(Arrays.stream(func).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    public <V> ArrayList<V> valuesFunc(Collection<Function<Children, V>> func) {
        try {
            List<String> values = new ArrayList<>();
            for (Function<Children, ?> f : func) {
                String subSqlSegment = getSubSqlSegment(f, (Class<Children>) getClass());
                values.add(subSqlSegment);
            }
            sqlSegment = "(" + String.join(",", values) + ")";
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    /**
     * 集合（不可单独使用）
     *
     * @param subSql 子查询
     * @param <V>    值类型
     * @return 值集合
     */
    public <V> ArrayList<V> valuesSubSql(Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        SingleValueSubSqlLambdaQueryWrapper<V> subSqlLambda = new SingleValueSubSqlLambdaQueryWrapper<>();
        subSql.accept(subSqlLambda);
        sqlSegment = "(" + subSqlLambda.getSqlSegment(getQueryWrapper()) + ")";
        return null;
    }

    @SuppressWarnings("unchecked")
    public Struct struct(Collection<?> col) {
        if (CollectionUtils.isEmpty(col)) {
            return _null();
        }
        Function<Children, ?>[] func = new Function[col.size()];
        int i = 0;
        for (Object value : col) {
            func[i++] = x -> x.value(value);
        }
        return structFunc(func);
    }

    /**
     * 结构体
     *
     * @param val 集合
     * @return 结构体
     */
    public Struct struct(Object...val) {
        return struct(Arrays.stream(val).collect(Collectors.toList()));
    }

    /**
     * 结构体
     *
     * @param columns 列
     * @param <T>     实体类型
     * @return 结构体
     */
    @SuppressWarnings("unchecked")
    public <T> Struct structColumn(Collection<SFunction<T, ?>> columns) {
        if (CollectionUtils.isEmpty(columns)) {
            return _null();
        }
        Function<Children, ?>[] func = new Function[columns.size()];
        int i = 0;
        for (SFunction<T, ?> column : columns) {
            func[i++] = x -> x.column(column);
        }
        return structFunc(func);
    }

    /**
     * 结构体
     *
     * @param columns 列
     * @param <T>     实体类型
     * @return 结构体
     */
    @SafeVarargs
    public final <T> Struct structColumn(SFunction<T, ?>... columns) {
        return structColumn(Arrays.stream(columns).collect(Collectors.toList()));
    }

    /**
     * 结构体
     *
     * @param func 表达式
     * @return 结构体
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final Struct structFunc(Function<Children, ?>... func) {
        if (func.length == 0) {
            return _null();
        }
        try {
            List<String> values = new ArrayList<>();
            for (Function<Children, ?> f : func) {
                String subSqlSegment = getSubSqlSegment(f, (Class<Children>) getClass());
                values.add(subSqlSegment);
            }
            sqlSegment = "(" + String.join(",", values) + ")";
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    /**
     * 结构体
     *
     * @param subSql 子查询
     * @return 结构体
     */
    public Struct structSubSql(Consumer<SubSqlLambdaQueryWrapper> subSql) {
        SubSqlLambdaQueryWrapper subSqlLambda = new SubSqlLambdaQueryWrapper();
        subSql.accept(subSqlLambda);
        sqlSegment = "(" + subSqlLambda.getSqlSegment(getQueryWrapper()) + ")";
        return null;
    }

    /**
     * 列
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 值类型
     */
    public <T, V> V column(SFunction<T, V> column) {
        return column(column, null);
    }

    /**
     * 列
     *
     * @param column      列
     * @param tableRename 表重命名
     * @param <T>         实体类型
     * @param <V>         值类型
     * @return 值类型
     */
    public <T, V> V column(SFunction<T, V> column, String tableRename) {
        try {
            sqlSegment = getFullColumnName(column, tableRename);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    /**
     * 使用选择列
     *
     * @param voColumn 选择列
     * @param <T>      实体类型
     * @param <V>      值类型
     * @return 值类型
     */
    public <T, V> V selectColumn(SFunction<T, V> voColumn) {
        return selectColumn(voColumn, "1");
    }

    /**
     * 使用选择列
     *
     * @param voColumn 选择列
     * @param seqKey   索引键
     * @param <T>      实体类型
     * @param <V>      值类型
     * @return 值类型
     */
    public <T, V> V selectColumn(SFunction<T, V> voColumn, String seqKey) {
        try {
            sqlSegment = getColumnRename(voColumn, seqKey);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    /**
     * 自定义字段
     *
     * @param columnName 自定义字段名
     * @param <V>        值类型
     * @return 值类型
     */
    public <V> V customColumn(String columnName) {
        sqlSegment = columnName;
        return null;
    }

    /**
     * 存在（不可单独使用）
     *
     * @param subSql 子查询
     * @param <V>    值类型
     * @return 值类型
     */
    @SuppressWarnings("unchecked")
    public <V> V any(Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return function("ANY", (Class<Children>) getClass(), x -> x.valueSubSql(subSql));
    }

    /**
     * 全部（不可单独使用）
     *
     * @param subSql 子查询
     * @param <V>    值类型
     * @return 值类型
     */
    @SuppressWarnings("unchecked")
    public <V> V all(Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return function("ALL", (Class<Children>) getClass(), x -> x.valueSubSql(subSql));
    }

    /**
     * 空值函数
     *
     * @param column 判空列
     * @param val    空值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 值类型
     */
    public <T, V> V ifnull(SFunction<T, V> column, V val) {
        return ifnull(column, null, val);
    }

    /**
     * 空值函数
     *
     * @param column      判空列
     * @param tableRename 表重命名
     * @param val         空值
     * @param <T>         实体类型
     * @param <V>         值类型
     * @return 值类型
     */
    public <T, V> V ifnull(SFunction<T, V> column, String tableRename, V val) {
        return ifnullFunc(x -> x.column(column, tableRename), x -> x.value(val));
    }

    /**
     * 空值函数
     *
     * @param column1 判空列
     * @param column2 空值列
     * @param <T1>    实体类型
     * @param <T2>    实体类型
     * @param <V>     值类型
     * @return 值类型
     */
    public <T1, T2, V> V ifnullColumn(SFunction<T1, V> column1, SFunction<T2, V> column2) {
        return ifnullColumn(column1, null, column2, null);
    }

    /**
     * 空值函数
     *
     * @param column1 判空列
     * @param rename1 判空列表重命名
     * @param column2 空值列
     * @param rename2 空值列表重命名
     * @param <T1>    实体类型
     * @param <T2>    实体类型
     * @param <V>     值类型
     * @return 值类型
     */
    public <T1, T2, V> V ifnullColumn(SFunction<T1, V> column1, String rename1, SFunction<T2, V> column2, String rename2) {
        return ifnullFunc(x -> x.column(column1, rename1), x -> x.column(column2, rename2));
    }

    /**
     * 空值函数
     *
     * @param func1 判空表达式
     * @param func2 空值表达式
     * @param <V>   值类型
     * @return 值类型
     */
    @SuppressWarnings("unchecked")
    public <V> V ifnullFunc(Function<Children, V> func1, Function<Children, V> func2) {
        return function("IFNULL", (Class<Children>) getClass(), func1, func2);
    }

    /**
     * 函数
     *
     * @param functionName 函数名
     * @param fClazz       函数表达式包装类型
     * @param subFunc      子函数表达式
     * @param <F>          函数表达式包装类型
     * @param <V>          值类型
     * @return 值类型
     */
    @SafeVarargs
    protected final <F extends AbstractFunctionLambdaQueryWrapper<?, F>, V> V function(String functionName, Class<F> fClazz, Function<F, ?>... subFunc) {
        Func<?>[] func = new Func<?>[subFunc.length == 0 ? 0 : subFunc.length * 2 - 1];
        int i = 0;
        for (Function<F, ?> f : subFunc) {
            if (i > 0) {
                func[i++] = Func.comma;
            }
            func[i++] = new Func<>(fClazz, f);
        }
        return function(functionName, func);
    }

    @SuppressWarnings("unchecked")
    protected <F extends AbstractFunctionLambdaQueryWrapper<?, F>, V> V function(String functionName, Func<?>... subFunc) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(functionName).append("(");
            String subSqlSegment;
            for (int i = 0; i < subFunc.length; i++) {
                subSqlSegment = getSubSqlSegment((Function<F, ?>) subFunc[i].function, (Class<F>) subFunc[i].clazz);
                // 去除收尾括号
                if (subSqlSegment.startsWith("(") && subSqlSegment.endsWith(")")) {
                    subSqlSegment = subSqlSegment.substring(1, subSqlSegment.length() - 1);
                }
                if (subSqlSegment.equals(",") || i == 0) {
                    sb.append(subSqlSegment);
                } else {
                    sb.append(" ").append(subSqlSegment);
                }
            }
            sb.append(")");
            sqlSegment = sb.toString();
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return null;
    }

    protected static class Func<F extends AbstractFunctionLambdaQueryWrapper<?, F>> {
        protected Class<F> clazz;
        protected Function<F, ?> function;

        protected Func(Class<F> clazz, Function<F, ?> function) {
            this.clazz = clazz;
            this.function = function;
        }

        private static Func<NormalFunctionLambdaQueryWrapper> keywordFunc(String keyword) {
            return new Func<>(NormalFunctionLambdaQueryWrapper.class, x -> {
                x.keyword(keyword);
                return null;
            });
        }

        protected static Func<NormalFunctionLambdaQueryWrapper> comma = keywordFunc(",");
        protected static Func<NormalFunctionLambdaQueryWrapper> distinct = keywordFunc("DISTINCT");
        protected static Func<NormalFunctionLambdaQueryWrapper> order = keywordFunc("ORDER BY");
        protected static Func<NormalFunctionLambdaQueryWrapper> using = keywordFunc("USING");
        protected static Func<NormalFunctionLambdaQueryWrapper> as = keywordFunc("AS");

    }

    @SafeVarargs
    protected final <F extends Children, V> V operate(String operate, Class<F> fClazz, Function<F, ?>... subFunc) {
        try {
            if (subFunc.length == 1) {
                // 单目运算符
                sqlSegment = "(" + operate + getSubSqlSegment(subFunc[0], fClazz) + ")";
            } else {
                // 双目运算符
                StringBuilder sb = new StringBuilder();
                sb.append("(");
                boolean firstParma = true;
                for (Function<F, ?> f : subFunc) {
                    if (firstParma) {
                        firstParma = false;
                    } else {
                        sb.append(" ").append(operate).append(" ");
                    }
                    sb.append(getSubSqlSegment(f, fClazz));
                }
                sb.append(")");
                sqlSegment = sb.toString();
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return null;
    }


    /**
     * 类型转换
     *
     * @param column   列
     * @param dataType 数据类型
     * @param <T>      实体类型
     * @param <V>      待换转的数据类型
     * @param <R>      转换的数据类型
     * @return 转换的数据类型
     */
    public <T, V, R> R convertData(SFunction<T, V> column, String dataType) {
        return convertDataTypeFunc(x -> x.column(column), dataType);
    }

    /**
     * 字符集转换
     *
     * @param val          值
     * @param characterSet 字符集
     * @return 字符串类型
     */
    public String convertCharacterSet(String val, String characterSet) {
        return convertCharacterSetFunc(x -> x.value(val), characterSet);
    }

    /**
     * 类型转换
     *
     * @param func     函数表达式
     * @param dataType 数据类型
     * @param <V>      值类型
     * @param <R>      转换的数据类型
     *                 DATE	将值转换成'YYYY-MM-DD'格式
     *                 DATETIME	将值转换成'YYYY-MM-DD HH：MM：SS'格式
     *                 TIME	将值转换成'HH：MM：SS'格式
     *                 CHAR	将值转换成CHAR(固定长度的字符串)格式
     *                 SIGNED	将值转换成INT(有符号的整数)格式
     *                 UNSIGNED	将值转换成INT(无符号的整数)格式
     *                 DECIMAL	将值转换成FLOAT(浮点数)格式
     *                 BINARY	将值转换成二进制格式
     * @return 转换的数据类型
     */
    @SuppressWarnings("unchecked")
    public <V, R> R convertDataTypeFunc(Function<Children, V> func, String dataType) {
        return function("CONVERT", new Func<>((Class<Children>) getClass(), func), Func.comma, Func.keywordFunc(dataType));
    }

    /**
     * 字符集转换
     *
     * @param func         函数表达式
     * @param characterSet 字符集
     * @return 字符串类型
     */
    @SuppressWarnings("unchecked")
    public String convertCharacterSetFunc(Function<Children, String> func, String characterSet) {
        return function("CONVERT", new Func<>((Class<Children>) getClass(), func), Func.using, Func.keywordFunc(characterSet));
    }

    /**
     * 将一个数字从一个数字基数系统转换为另一个数字基数系统
     *
     * @param val      值
     * @param fromBase 数字的现有基数
     * @param toBase   转换后的数字的基数
     * @param <V>      值类型
     * @return 字符串类型
     */
    public <V> String conv(V val, int fromBase, int toBase) {
        return convFunc(x -> x.value(val), fromBase, toBase);
    }

    /**
     * 将一个数字从一个数字基数系统转换为另一个数字基数系统
     *
     * @param func     函数表达式
     * @param fromBase 数字的现有基数
     * @param toBase   转换后的数字的基数。
     * @param <V>      值类型
     * @return 字符串类型
     */
    @SuppressWarnings("unchecked")
    public <V> String convFunc(Function<Children, V> func, int fromBase, int toBase) {
        return function("CONV", (Class<Children>) getClass(), func, x -> x.value(fromBase), x -> x.value(toBase));
    }

    // 算术运算

    /**
     * 加
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Number> V add(SFunction<T, V> column, V val) {
        return addFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 加
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Number> V addFunc(Function<Children, V>... func) {
        return operate("+", (Class<Children>) getClass(), func);
    }

    /**
     * 减
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Number> V subtract(SFunction<T, V> column, V val) {
        return subtractFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 减
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Number> V subtractFunc(Function<Children, V>... func) {
        return operate("-", (Class<Children>) getClass(), func);
    }

    /**
     * 乘
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Number> V multiply(SFunction<T, Number> column, Number val) {
        return multiplyFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 乘
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Number> V multiplyFunc(Function<Children, Number>... func) {
        return operate("*", (Class<Children>) getClass(), func);
    }

    /**
     * 除
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Number> V divide(SFunction<T, Number> column, Number val) {
        return divideFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 除
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Number> V divideFunc(Function<Children, Number>... func) {
        return operate("/", (Class<Children>) getClass(), func);
    }

    /**
     * 取余
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Number> V mod(SFunction<T, Number> column, Number val) {
        return modFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 取余
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Number> V modFunc(Function<Children, Number>... func) {
        return operate("%", (Class<Children>) getClass(), func);
    }

    // 比较运算符

    /**
     * 等于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends  Comparable<V>> Long eq(SFunction<T, V> column, V val) {
        return eqFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 等于
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> Long eqFunc(Function<Children, V>... func) {
        return operate("=", (Class<Children>) getClass(), func);
    }

    /**
     * 不等于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long ne(SFunction<T, V> column, V val) {
        return neFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 不等于
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> Long neFunc(Function<Children, V>... func) {
        return operate("!=", (Class<Children>) getClass(), func);
    }

    /**
     * 大于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long gt(SFunction<T, V> column, V val) {
        return gtFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 大于
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> Long gtFunc(Function<Children, V>... func) {
        return operate(">", (Class<Children>) getClass(), func);
    }

    /**
     * 大于等于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long ge(SFunction<T, V> column, V val) {
        return geFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 大于等于
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> Long geFunc(Function<Children, V>... func) {
        return operate(">=", (Class<Children>) getClass(), func);
    }

    /**
     * 小于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long lt(SFunction<T, V> column, V val) {
        return ltFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 小于
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> Long ltFunc(Function<Children, V>... func) {
        return operate("<", (Class<Children>) getClass(), func);
    }

    /**
     * 小于等于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long le(SFunction<T, V> column, V val) {
        return leFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 小于等于
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> Long leFunc(Function<Children, V>... func) {
        return operate("<=", (Class<Children>) getClass(), func);
    }

    public <T, V extends Comparable<V>> Long like(SFunction<T, V> column, String val) {
        return likeFunc(x -> x.column(column), x -> x.value(val));
    }

    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long likeFunc(Function<Children, V> func1, Function<Children, String> func2) {
        return operate("LIKE", (Class<Children>) getClass(), func1, func2);
    }

    public <T, V extends Comparable<V>> Long notLike(SFunction<T, V> column, String val) {
        return notLikeFunc(x -> x.column(column), x -> x.value(val));
    }

    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long notLikeFunc(Function<Children, V> func1, Function<Children, String> func2) {
        return operate("NOT LIKE", (Class<Children>) getClass(), func1, func2);
    }

    /**
     * 在...之间
     *
     * @param column 列
     * @param val1   值1
     * @param val2   值2
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long between(SFunction<T, V> column, V val1, V val2) {
        return betweenFunc(x -> x.column(column), x -> x.value(val1), x -> x.value(val2));
    }

    /**
     * 在...之间
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param func3 表达式3
     * @param <V>   值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long betweenFunc(Function<Children, V> func1, Function<Children, V> func2, Function<Children, ?> func3) {
        try {
            sqlSegment = "(" + getSubSqlSegment(func1, (Class<Children>) getClass()) + " BETWEEN " + getSubSqlSegment(func2, (Class<Children>) getClass()) + " AND " + getSubSqlSegment(func3, (Class<Children>) getClass()) + ")";
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return null;
    }

    /**
     * 不在...之间
     *
     * @param column 列
     * @param val1   值1
     * @param val2   值2
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long notBetween(SFunction<T, V> column, V val1, V val2) {
        return notBetweenFunc(x -> x.column(column), x -> x.value(val1), x -> x.value(val2));
    }

    /**
     * 不在...之间
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param func3 表达式3
     * @param <V>   值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long notBetweenFunc(Function<Children, V> func1, Function<Children, V> func2, Function<Children, ?> func3) {
        try {
            sqlSegment = "(" + getSubSqlSegment(func1, (Class<Children>) getClass()) + " NOT BETWEEN " + getSubSqlSegment(func2, (Class<Children>) getClass()) + " AND " + getSubSqlSegment(func3, (Class<Children>) getClass()) + ")";
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return null;
    }

    /**
     * 空值
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long isNull(SFunction<T, V> column) {
        return isNullFunc(x -> x.column(column));
    }

    /**
     * 空值
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long isNullFunc(Function<Children, V> func) {
        return operate("IS", (Class<Children>) getClass(), func, AbstractFunctionLambdaQueryWrapper::_null);
    }

    /**
     * 非空值
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long isNotNull(SFunction<T, V> column) {
        return isNotNullFunc(x -> x.column(column));
    }

    /**
     * 非空值
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long isNotNullFunc(Function<Children, V> func) {
        return operate("IS NOT", (Class<Children>) getClass(), func, AbstractFunctionLambdaQueryWrapper::_null);
    }

    @SuppressWarnings("unchecked")
    public <T, V extends Comparable<V>> Long in(SFunction<T, V> column, Collection<V> coll) {
        if (!CollectionUtils.isEmpty(coll)) {
            return operate("IN", (Class<Children>) getClass(), x -> x.column(column), x -> x.values(coll));
        } else {
            return operate("=", (Class<Children>) getClass(), x -> x.value(1), x -> x.value(1));
        }
    }

    @SafeVarargs
    public final <T, V extends Comparable<V>> Long in(SFunction<T, V> column, V... values) {
        return in(column, Arrays.stream(values).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long inFunc(Function<Children, V> func1, Function<Children, Collection<V>> func2) {
        return operate("IN", (Class<Children>) getClass(), func1, func2);
    }

    @SuppressWarnings("unchecked")
    public <T, V extends Comparable<V>> Long notIn(SFunction<T, V> column, Collection<V> coll) {
        if (!CollectionUtils.isEmpty(coll)) {
            return operate("NOT IN", (Class<Children>) getClass(), x -> x.column(column), x -> x.values(coll));
        } else {
            return operate("=", (Class<Children>) getClass(), x -> x.value(1), x -> x.value(0));
        }
    }

    @SafeVarargs
    public final <T, V extends Comparable<V>> Long notIn(SFunction<T, V> column, V... values) {
        return notIn(column, Arrays.stream(values).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long notInFunc(Function<Children, V> func1, Function<Children, Collection<V>> func2) {
        return operate("NOT IN", (Class<Children>) getClass(), func1, func2);
    }

    /**
     * 存在
     *
     * @param subSql 子查询
     * @return 数字类型
     */
    public Long exists(Consumer<NonValueSubSqlLambdaQueryWrapper> subSql) {
        try {
            sqlSegment = "EXISTS(" + getSubSqlSegment(subSql, NonValueSubSqlLambdaQueryWrapper.class) + ")";
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return null;
    }

    /**
     * 不存在
     *
     * @param subSql 子查询
     * @return 数字类型
     */
    public Long notExists(Consumer<NonValueSubSqlLambdaQueryWrapper> subSql) {
        try {
            sqlSegment = "NOT EXISTS(" + getSubSqlSegment(subSql, NonValueSubSqlLambdaQueryWrapper.class) + ")";
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return null;
    }

    public <T, V extends Comparable<V>> Long regexp(SFunction<T, V> column, String val) {
        return regexpFunc(x -> (String) x.column(column), x -> x.value(val));
    }

    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long regexpFunc(Function<Children, V> func1, Function<Children, String> func2) {
        return operate("REGEXP", (Class<Children>) getClass(), func1, func2);
    }

    // 逻辑运算

    /**
     * 与
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long and(SFunction<T, V> column, V val) {
        return andFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 与
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> Long andFunc(Function<Children, V>... func) {
        return operate("&&", (Class<Children>) getClass(), func);
    }

    /**
     * 或
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long or(SFunction<T, V> column, V val) {
        return orFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 或
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> Long orFunc(Function<Children, V>... func) {
        return operate("||", (Class<Children>) getClass(), func);
    }


    /**
     * 非
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long not(SFunction<T, V> column) {
        return notFunc(x -> x.column(column));
    }

    /**
     * 非
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> Long notFunc(Function<Children, V> func) {
        return operate("!", (Class<Children>) getClass(), func);
    }


    /**
     * 异或
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> Long xor(SFunction<T, V> column, V val) {
        return xorFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 异或
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> Long xorFunc(Function<Children, V>... func) {
        return operate("XOR", (Class<Children>) getClass(), func);
    }

    // 位运算符

    /**
     * 按位与
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> V bAnd(SFunction<T, V> column, V val) {
        return bAndFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 按位与
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> V bAndFunc(Function<Children, V>... func) {
        return operate("&", (Class<Children>) getClass(), func);
    }

    /**
     * 按位或
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> V bOr(SFunction<T, V> column, V val) {
        return bOrFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 按位或
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> V bOrFunc(Function<Children, V>... func) {
        return operate("|", (Class<Children>) getClass(), func);
    }

    /**
     * 按位取反
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> V bNot(SFunction<T, V> column) {
        return bNotFunc(x -> x.column(column));
    }

    /**
     * 按位取反
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> V bNotFunc(Function<Children, V> func) {
        return operate("~", (Class<Children>) getClass(), func);
    }

    /**
     * 按位异或
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> V bXor(SFunction<T, V> column, V val) {
        return bXorFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 按位异或
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final <V extends Comparable<V>> V bXorFunc(Function<Children, V>... func) {
        return operate("~", (Class<Children>) getClass(), func);
    }

    /**
     * 按位左移
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> V bShiftLeft(SFunction<T, V> column, Number val) {
        return bShiftLeftFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 按位左移
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> V bShiftLeftFunc(Function<Children, V> func1, Function<Children, Number> func2) {
        return operate("<<", (Class<Children>) getClass(), func1, func2);
    }

    /**
     * 按位左移
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 数字类型
     */
    public <T, V extends Comparable<V>> V bShiftRight(SFunction<T, V> column, Number val) {
        return bShiftRightFunc(x -> x.column(column), x -> x.value(val));
    }

    /**
     * 按位左移
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V extends Comparable<V>> V bShiftRightFunc(Function<Children, V> func1, Function<Children, Number> func2) {
        return operate(">>", (Class<Children>) getClass(), func1, func2);
    }
    // 数学函数

    /**
     * 绝对值
     *
     * @param val 值
     * @return 数字类型
     */
    public BigDecimal abs(Number val) {
        return absFunc(x -> x.value(val));
    }

    /**
     * 绝对值
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 数字类型
     */
    public <T> BigDecimal abs(SFunction<T, Number> column) {
        return abs(column, null);
    }

    /**
     * 绝对值
     *
     * @param column      列
     * @param tableRename 表重命名
     * @param <T>         实体类型
     * @return 数字类型
     */
    public <T> BigDecimal abs(SFunction<T, Number> column, String tableRename) {
        return absFunc(x -> x.column(column, tableRename));
    }

    /**
     * 绝对值
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public BigDecimal absFunc(Function<Children, Number> func) {
        return function("ABS", (Class<Children>) getClass(), func);
    }

    /**
     * 圆周率π
     *
     * @return 数字类型
     */
    public BigDecimal pi() {
        return function("PI");
    }

    /**
     * 非负数的x的二次方根
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 数字类型
     */
    public <T> BigDecimal sqrt(SFunction<T, Number> column) {
        return sqrt(column, null);
    }

    /**
     * 非负数的x的二次方根
     *
     * @param column      列
     * @param tableRename 表重命名
     * @param <T>         实体类型
     * @return 数字类型
     */
    public <T> BigDecimal sqrt(SFunction<T, Number> column, String tableRename) {
        return sqrtFunc(x -> x.column(column, tableRename));
    }

    /**
     * 非负数的x的二次方根
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public BigDecimal sqrtFunc(Function<Children, Number> func) {
        return function("SQRT", (Class<Children>) getClass(), func);
    }

    /**
     * 返回分段后的结果
     *
     * @param val 值
     * @param n   分段的间隔
     * @param <V> 值类型
     * @return 数字类型
     */
    public <V> Long interval(V val, Number... n) {
        return intervalFunc(x -> x.value(val), n);
    }

    /**
     * 返回分段后的结果
     *
     * @param func 函数表达式
     * @param n    分段的间隔
     * @param <V>  值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V> Long intervalFunc(Function<Children, V> func, Number... n) {
        Function<Children, ?>[] functions = new Function[n.length + 1];
        functions[0] = func;
        for (int i = 0; i < n.length; i++) {
            int index = i;
            functions[i + 1] = x -> x.value(n[index]);
        }
        return function("INTERVAL", (Class<Children>) getClass(), functions);
    }

    // 字符串函数

    /**
     * 计算字符串字符个数
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 数字类型
     */
    public <T> BigDecimal charLength(SFunction<T, String> column) {
        return charLength(column, null);
    }

    /**
     * 计算字符串字符个数
     *
     * @param column      列
     * @param tableRename 表重命名
     * @param <T>         实体类型
     * @return 数字类型
     */
    public <T> BigDecimal charLength(SFunction<T, String> column, String tableRename) {
        return charLengthFunc(x -> x.column(column, tableRename));
    }

    /**
     * 计算字符串字符个数
     *
     * @param func 函数表达式
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public BigDecimal charLengthFunc(Function<Children, String> func) {
        return function("CHAR_LENGTH", (Class<Children>) getClass(), func);
    }

    /**
     * 连接参数产生的字符串，一个或多个待拼接的内容，任意一个为NULL则返回值为NULL
     *
     * @param func 函数表达式
     * @return 字符串类型
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final String concatFunc(Function<Children, String>... func) {
        return function("CONCAT", (Class<Children>) getClass(), func);
    }

    /**
     * 从左截取
     *
     * @param val    值
     * @param length 截取个数
     * @return 字符串
     */
    public String left(String val, int length) {
        return leftFunc(x -> x.value(val), length);
    }

    /**
     * 从左截取
     *
     * @param func   函数表达式
     * @param length 截取个数
     * @return 字符串
     */
    @SuppressWarnings("unchecked")
    public String leftFunc(Function<Children, String> func, int length) {
        return function("LEFT", (Class<Children>) getClass(), func, x -> x.value(length));
    }

    /**
     * 从右截取
     *
     * @param val    值
     * @param length 截取个数
     * @return 字符串
     */
    public String right(String val, int length) {
        return rightFunc(x -> x.value(val), length);
    }

    /**
     * 从右截取
     *
     * @param func   函数表达式
     * @param length 截取个数
     * @return 字符串
     */
    @SuppressWarnings("unchecked")
    public String rightFunc(Function<Children, String> func, int length) {
        return function("RIGHT", (Class<Children>) getClass(), func, x -> x.value(length));
    }

    /**
     * 字符串截取
     *
     * @param column 列
     * @param delim  分隔符
     * @param count  第几个元素
     * @param <T>    实体类型
     * @return 字符串类型
     */
    public <T> String substringIndex(SFunction<T, String> column, String delim, int count) {
        return substringIndex(column, null, delim, count);
    }

    /**
     * 字符串截取
     *
     * @param column      列
     * @param tableRename 表重命名
     * @param delim       分隔符
     * @param count       第几个元素
     * @param <T>         实体类型
     * @return 字符串类型
     */
    public <T> String substringIndex(SFunction<T, String> column, String tableRename, String delim, int count) {
        return substringIndexFunc(x -> x.column(column, tableRename), delim, count);
    }

    /**
     * 字符串截取
     *
     * @param func   函数表达式
     * @param delim  分隔符
     * @param count  第几个元素
     * @return 字符串类型
     */
    @SuppressWarnings("unchecked")
    public String substringIndexFunc(Function<Children, String> func, String delim, int count) {
        return function("SUBSTRING_INDEX", (Class<Children>) getClass(), func, x -> x.value(delim), x -> x.value(count));
    }

    /**
     * 从字符串中删除不必要的前导和后缀字符
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 字符串类型
     */
    public <T> String trim(SFunction<T, String> column) {
        return trim(column, null);
    }

    /**
     * 从字符串中删除不必要的前导和后缀字符
     *
     * @param column      列
     * @param tableRename 表重命名
     * @param <T>         实体类型
     * @return 字符串类型
     */
    public <T> String trim(SFunction<T, String> column, String tableRename) {
        return trimFunc(x -> x.column(column, tableRename));
    }

    /**
     * 从字符串中删除不必要的前导和后缀字符
     *
     * @param func 函数表达式
     * @return 字符串类型
     */
    @SuppressWarnings("unchecked")
    public String trimFunc(Function<Children, String> func) {
        return function("TRIM", (Class<Children>) getClass(), func);
    }

    /**
     * md5加密
     *
     * @param val 值
     * @return 字符串类型
     */
    public String md5(String val) {
        return md5Func(x -> x.value(val));
    }

    /**
     * md5加密
     *
     * @param func 函数表达式
     * @return 字符串类型
     */
    @SuppressWarnings("unchecked")
    public String md5Func(Function<Children, String> func) {
        return function("MD5", (Class<Children>) getClass(), func);
    }

    /**
     * 转换十六进制值的字符串表示
     *
     * @param val 值
     * @return 字符串
     */
    public String hexStr(String val) {
        return hexStrFunc(x -> x.value(val));
    }

    /**
     * 转换十六进制值的字符串表示
     *
     * @param func 函数表达式
     * @return 字符串
     */
    @SuppressWarnings("unchecked")
    public String hexStrFunc(Function<Children, String> func) {
        return function("HEX", (Class<Children>) getClass(), func);
    }

    /**
     * 转换十六进制值的字符串表示
     *
     * @param val 值
     * @return 字符串
     */
    public String hexNumber(Number val) {
        return hexNumberFunc(x -> x.value(val));
    }

    /**
     * 转换十六进制值的字符串表示
     *
     * @param func 函数表达式
     * @return 字符串
     */
    @SuppressWarnings("unchecked")
    public String hexNumberFunc(Function<Children, Number> func) {
        return function("HEX", (Class<Children>) getClass(), func);
    }

    /**
     * 字符串列表中的第N个元素
     *
     * @param val 值
     * @param str 字符串
     * @return 字符串
     */
    public String elt(Number val, String... str) {
        return eltFunc(x -> x.value(val), str);
    }

    /**
     * 字符串列表中的第N个元素
     *
     * @param func 函数表达式
     * @param str  字符串
     * @return 字符串
     */
    @SuppressWarnings("unchecked")
    public String eltFunc(Function<Children, Number> func, String... str) {
        Function<Children, ?>[] functions = new Function[str.length + 1];
        functions[0] = func;
        for (int i = 0; i < str.length; i++) {
            int index = i;
            functions[i + 1] = x -> x.value(str[index]);
        }
        return function("ELT", (Class<Children>) getClass(), functions);
    }

    /**
     * 查询字段(strlist)中包含(str)的结果，返回结果为null或记录
     *
     * @param str    要查询的字符串
     * @param column 列
     * @param <T>    实体类型
     * @return 数字类型
     */
    public <T> Long findInSet(String str, SFunction<T, String> column) {
        return findInSetFunc(x -> x.value(str), x -> x.column(column));
    }

    /**
     * 查询字段(strlist)中包含(str)的结果，返回结果为null或记录
     *
     * @param func1 函数表达式1
     * @param func2 函数表达式2
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public Long findInSetFunc(Function<Children, String> func1, Function<Children, String> func2) {
        return function("FIND_IN_SET", (Class<Children>) getClass(), func1, func2);
    }

    // 日期函数

    /**
     * 获取当前时间
     *
     * @return 日期类型
     */
    public Date now() {
        return function("NOW");
    }

    /**
     * 日期类型转字符串
     *
     * @param column 列
     * @param format 规定日期/时间的输出格式
     *               <p>%a	缩写星期名</P>
     *               <p>%b	缩写月名</P>
     *               <p>%c	月，数值</P>
     *               <p>%D	带有英文前缀的月中的天</P>
     *               <p>%d	月的天，数值(00-31)</P>
     *               <p>%e	月的天，数值(0-31)</P>
     *               <p>%f	微秒</P>
     *               <p>%H	小时 (00-23)</P>
     *               <p>%h	小时 (01-12)</P>
     *               <p>%I	小时 (01-12)</P>
     *               <p>%i	分钟，数值(00-59)</P>
     *               <p>%j	年的天 (001-366)</P>
     *               <p>%k	小时 (0-23)</P>
     *               <p>%l	小时 (1-12)</P>
     *               <p>%M	月名,%m	月，数值(00-12)</P>
     *               <p>%p	AM 或 PM</P>
     *               <p>%r	时间，12-小时（hh:mm:ss AM 或 PM）</P>
     *               <p>%S	秒(00-59)</P>
     *               <p>%s	秒(00-59)</P>
     *               <p>%T	时间, 24-小时 (hh:mm:ss)</P>
     *               <p>%U	周 (00-53) 星期日是一周的第一天</P>
     *               <p>%u	周 (00-53) 星期一是一周的第一天</P>
     *               <p>%V	周 (01-53) 星期日是一周的第一天，与 %X 使用</P>
     *               <p>%v	周 (01-53) 星期一是一周的第一天，与 %x 使用</P>
     *               <p>%W	星期名,%w	周的天 （0=星期日, 6=星期六）</P>
     *               <p>%X	年，其中的星期日是周的第一天，4 位，与 %V 使用</P>
     *               <p>%x	年，其中的星期一是周的第一天，4 位，与 %v 使用</P>
     *               <p>%Y	年，4 位,%y	年，2 位</P>
     * @param <T>    实体类型
     * @return 日期类型
     */
    public <T> String dateFormat(SFunction<T, Date> column, String format) {
        return dateFormatFunc(x -> x.column(column), format);
    }

    /**
     * 字符串转日期类型
     *
     * @param func   函数表达式
     * @param format 规定日期/时间的输出格式
     *               <p>%a	缩写星期名</P>
     *               <p>%b	缩写月名</P>
     *               <p>%c	月，数值</P>
     *               <p>%D	带有英文前缀的月中的天</P>
     *               <p>%d	月的天，数值(00-31)</P>
     *               <p>%e	月的天，数值(0-31)</P>
     *               <p>%f	微秒</P>
     *               <p>%H	小时 (00-23)</P>
     *               <p>%h	小时 (01-12)</P>
     *               <p>%I	小时 (01-12)</P>
     *               <p>%i	分钟，数值(00-59)</P>
     *               <p>%j	年的天 (001-366)</P>
     *               <p>%k	小时 (0-23)</P>
     *               <p>%l	小时 (1-12)</P>
     *               <p>%M	月名,%m	月，数值(00-12)</P>
     *               <p>%p	AM 或 PM</P>
     *               <p>%r	时间，12-小时（hh:mm:ss AM 或 PM）</P>
     *               <p>%S	秒(00-59)</P>
     *               <p>%s	秒(00-59)</P>
     *               <p>%T	时间, 24-小时 (hh:mm:ss)</P>
     *               <p>%U	周 (00-53) 星期日是一周的第一天</P>
     *               <p>%u	周 (00-53) 星期一是一周的第一天</P>
     *               <p>%V	周 (01-53) 星期日是一周的第一天，与 %X 使用</P>
     *               <p>%v	周 (01-53) 星期一是一周的第一天，与 %x 使用</P>
     *               <p>%W	星期名,%w	周的天 （0=星期日, 6=星期六）</P>
     *               <p>%X	年，其中的星期日是周的第一天，4 位，与 %V 使用</P>
     *               <p>%x	年，其中的星期一是周的第一天，4 位，与 %v 使用</P>
     *               <p>%Y	年，4 位,%y	年，2 位</P>
     * @return 日期类型
     */
    @SuppressWarnings("unchecked")
    public String dateFormatFunc(Function<Children, Date> func, String format) {
        return function("DATE_FORMAT", (Class<Children>) getClass(), func, x -> x.value(format));
    }

    public <T> Date strToDate(SFunction<T, String> column, String format) {
        return strToDateFunc(x -> x.column(column), format);
    }

    @SuppressWarnings("unchecked")
    public Date strToDateFunc(Function<Children, String> func, String format) {
        return function("STR_TO_DATE", (Class<Children>) getClass(), func, x -> x.value(format));
    }


    /*
     * 日期转年
     * @Author: 小明同学
     * @date 2022/5/6 15:29
     * @param column 日期
     * @return java.util.Date
     */
    public <T> Long year(SFunction<T, Date> column) {
        return yearFunc(x -> x.column(column));
    }

    @SuppressWarnings("unchecked")
    public Long yearFunc(Function<Children, Date> func) {
        return function("YEAR", (Class<Children>) getClass(), func);
    }


    /*
     * 日期转季
     * @Author: 小明同学
     * @date 2022/5/6 15:29
     * @param column 日期
     * @return java.util.Date
     */
    public <T> Long quarter(SFunction<T, Date> column) {
        return yearFunc(x -> x.column(column));
    }

    @SuppressWarnings("unchecked")
    public Long quarterFunc(Function<Children, Date> func) {
        return function("QUARTER", (Class<Children>) getClass(), func);
    }


    /*
     * 日期转天数
     * @Author: 小明同学
     * @date 2022/5/6 15:29
     * @param column 日期
     * @return java.util.Date
     */
    public <T> Long toDays(SFunction<T, Date> column) {
        return toDaysFunc(x -> x.column(column));
    }

    @SuppressWarnings("unchecked")
    public Long toDaysFunc(Function<Children, Date> func) {
        return function("TO_DAYS", (Class<Children>) getClass(), func);
    }


    /**
     * 获取该日期在当年的周期
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 数字类型
     */
    public <T> Long weekOfYear(SFunction<T, Date> column) {
        return weekOfYearFunc(x -> x.column(column));
    }

    @SuppressWarnings("unchecked")
    public Long weekOfYearFunc(Function<Children, Date> func) {
        return function("WEEKOFYEAR", (Class<Children>) getClass(), func);
    }


    /**
     * 获取该日期在当年的第多少天
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 数字类型
     */
    public <T> Long dayOfYear(SFunction<T, Date> column) {
        return dayOfYearFunc(x -> x.column(column));
    }

    @SuppressWarnings("unchecked")
    public Long dayOfYearFunc(Function<Children, Date> func) {
        return function("DAYOFYEAR", (Class<Children>) getClass(), func);
    }


    /*
     * 日期转秒
     * @Author: 小明同学
     * @date 2022/5/6 15:29
     * @param column 日期
     * @return java.util.Date
     */
    public <T> Long toSeconds(SFunction<T, Date> column) {
        return toSecondsFun(x -> x.column(column));
    }

    @SuppressWarnings("unchecked")
    public Long toSecondsFun(Function<Children, Date> func) {
        return function("TO_SECONDS", (Class<Children>) getClass(), func);
    }


    // public <T> Date unixTimestamp(SFunction<T, Date> column) {
    //     return unixTimestampFunc(x -> x.column(column), format);
    // }
    //
    // public Date unixTimestampFunc(Function<Children, Date> func, String format) {
    //     return function("UNIX_TIMESTAMP", (Class<Children>) getClass(), func, x -> x.value(format));
    // }

    /**
     * 向日期添加指定的时间间隔
     *
     * @param column 列
     * @param expr   参数是您希望添加的时间间隔
     * @param type   类型,MICROSECOND,SECOND,MINUTE,HOUR,DAY,WEEK,MONTH,QUARTER,YEAR,SECOND_MICROSECOND,MINUTE_MICROSECOND,MINUTE_SECOND,HOUR_MICROSECOND,HOUR_SECOND,HOUR_MINUTE,DAY_MICROSECOND,DAY_SECOND,DAY_MINUTE,DAY_HOUR,YEAR_MONTH
     * @param <T>    实体类型
     * @return 日期类型
     */
    public <T> Date dateAdd(SFunction<T, Date> column, int expr, String type) {
        return dateAdd(column, null, expr, type);
    }

    /**
     * 向日期添加指定的时间间隔
     *
     * @param column      列
     * @param tableRename 表重命名
     * @param expr        参数是您希望添加的时间间隔
     * @param type        类型,MICROSECOND,SECOND,MINUTE,HOUR,DAY,WEEK,MONTH,QUARTER,YEAR,SECOND_MICROSECOND,MINUTE_MICROSECOND,MINUTE_SECOND,HOUR_MICROSECOND,HOUR_SECOND,HOUR_MINUTE,DAY_MICROSECOND,DAY_SECOND,DAY_MINUTE,DAY_HOUR,YEAR_MONTH
     * @param <T>         实体类型
     * @return 日期类型
     */
    public <T> Date dateAdd(SFunction<T, Date> column, String tableRename, int expr, String type) {
        return dateAddFunc(x -> x.column(column, tableRename), x -> x.value(expr), type);
    }

    /**
     * 向日期添加指定的时间间隔
     *
     * @param func     函数表达式
     * @param exprFunc 参数是您希望添加的时间间隔
     * @param type     类型,MICROSECOND,SECOND,MINUTE,HOUR,DAY,WEEK,MONTH,QUARTER,YEAR,SECOND_MICROSECOND,MINUTE_MICROSECOND,MINUTE_SECOND,HOUR_MICROSECOND,HOUR_SECOND,HOUR_MINUTE,DAY_MICROSECOND,DAY_SECOND,DAY_MINUTE,DAY_HOUR,YEAR_MONTH
     * @return 日期类型
     */
    @SuppressWarnings("unchecked")
    public Date dateAddFunc(Function<Children, Date> func, Function<Children, Number> exprFunc, String type) {
        try {
            sqlSegment = "DATE_ADD(" + getSubSqlSegment(func, (Class<Children>) getClass()) + ", INTERVAL " + getSubSqlSegment(exprFunc, (Class<Children>) getClass()) + " " + type + ")";
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return null;
    }

    /**
     * 随机函数
     *
     * @param val 值
     * @param <V> 值类型
     * @return 数字类型
     */
    @SuppressWarnings("unchecked")
    public <V> Double rand(V val) {
        return function("RAND", (Class<Children>) getClass(), x -> x.value(val));
    }

    @SuppressWarnings("unchecked")
    public <V> Double randFunc(Function<Children, V> func) {
        return function("RAND", (Class<Children>) getClass(), func);
    }

    // 条件语句

    protected abstract <C extends AbstractCaseLambdaQueryWrapper<W, Children, V, C>, V> C getCaseLambdaQueryWrapperInstance();

    /**
     * 条件语句
     *
     * @param _case case条件表达式
     */
    public <C extends AbstractCaseLambdaQueryWrapper<W, Children, V, C>, V> V _case0(Consumer<C> _case) {
        C caseLambda = getCaseLambdaQueryWrapperInstance();
        _case.accept(caseLambda);
        sqlSegment = caseLambda.getSqlSegment(getQueryWrapper());
        return null;
    }

    /**
     * 条件判断
     *
     * @param predicate 条件表达式
     * @param val1      条件为真值
     * @param val2      条件为假值
     * @param <V>       值类型
     * @return 值类型
     */
    public <V> V _if(Consumer<W> predicate, V val1, V val2) {
        return ifFunc(predicate, x -> x.value(val1), x -> x.value(val2));
    }

    /**
     * 条件判断
     *
     * @param predicate 条件表达式
     * @param func1     条件为真值表达式
     * @param func2     条件为假值表达式
     * @param <V>       值类型
     * @return 值类型
     */
    @SuppressWarnings("unchecked")
    public <V> V ifFunc(Consumer<W> predicate, Function<Children, V> func1, Function<Children, V> func2) {
        try {
            W whereLambda = wClazz.newInstance();
            predicate.accept(whereLambda);
            sqlSegment = "IF(" + whereLambda.getSqlSegment(getQueryWrapper()) + "," + getSubSqlSegment(func1, (Class<Children>) getClass()) + "," + getSubSqlSegment(func2, (Class<Children>) getClass()) + ")";
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return null;
    }

    // 自定义函数

    /**
     * 空或空字符串函数
     *
     * @param column 判空列
     * @param val    空值
     * @param <T>    实体类型
     * @return 字符串类型
     */
    public <T> String ifEmpty(SFunction<T, String> column, String val) {
        return ifEmpty(column, null, val);
    }

    /**
     * 空或空字符串函数
     *
     * @param column      判空列
     * @param tableRename 表重命名
     * @param val         空值
     * @param <T>         实体类型
     * @return 字符串类型
     */
    public <T> String ifEmpty(SFunction<T, String> column, String tableRename, String val) {
        return ifFunc(x -> x.isEmpty(column, tableRename), x -> x.value(val), x -> x.column(column, tableRename));
    }

    /**
     * 获取拼音首字母
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 字符串类型
     */
    public <T> String firstPinyinChar(SFunction<T, String> column) {
        return firstPinyinChar(column, null);
    }


    /**
     * 获取拼音首字母
     *
     * @param column      列
     * @param tableRename 表重命名
     * @param <T>         实体类型
     * @return 字符串类型
     */
    public <T> String firstPinyinChar(SFunction<T, String> column, String tableRename) {
        return _case0(x -> x
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("吖"), "gbk"), 1)), y -> y.value(""))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("八"), "gbk"), 1)), y -> y.value("A"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("嚓"), "gbk"), 1)), y -> y.value("B"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("咑"), "gbk"), 1)), y -> y.value("C"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("妸"), "gbk"), 1)), y -> y.value("D"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("发"), "gbk"), 1)), y -> y.value("E"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("旮"), "gbk"), 1)), y -> y.value("F"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("铪"), "gbk"), 1)), y -> y.value("G"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("丌"), "gbk"), 1)), y -> y.value("H"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("咔"), "gbk"), 1)), y -> y.value("J"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("垃"), "gbk"), 1)), y -> y.value("K"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("嘸"), "gbk"), 1)), y -> y.value("L"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("拏"), "gbk"), 1)), y -> y.value("M"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("噢"), "gbk"), 1)), y -> y.value("N"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("妑"), "gbk"), 1)), y -> y.value("O"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("七"), "gbk"), 1)), y -> y.value("P"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("呥"), "gbk"), 1)), y -> y.value("Q"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("仨"), "gbk"), 1)), y -> y.value("R"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("他"), "gbk"), 1)), y -> y.value("S"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("屲"), "gbk"), 1)), y -> y.value("T"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("夕"), "gbk"), 1)), y -> y.value("W"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("丫"), "gbk"), 1)), y -> y.value("X"))
                .whenThenFunc(y -> y.ltFunc(z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.column(column, tableRename), "gbk"), 1), z -> z.leftFunc(t -> t.convertCharacterSetFunc(u -> u.value("帀"), "gbk"), 1)), y -> y.value("Y"))
                .elseValue("Z"));
    }

}
