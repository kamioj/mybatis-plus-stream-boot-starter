package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractWhereLambdaQueryWrapper<F extends AbstractFunctionLambdaQueryWrapper<Children, F>,
        Children extends AbstractWhereLambdaQueryWrapper<F, Children>>
        extends AbstractSubLambdaQueryWrapper<Children> {
    protected final Class<F> fClazz;

    public AbstractWhereLambdaQueryWrapper() {
        this(null);
    }

    public AbstractWhereLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
        fClazz = ReflectUtils.getGenericClass(getClass().getGenericSuperclass(), AbstractWhereLambdaQueryWrapper.class, 0);
    }

    /**
     * 获取sql，并添加输入参数
     *
     * @param queryWrapper queryWrapper
     * @return sqlSegment
     */
    @Override
    public String getSqlSegment(ExQueryWrapper<?> queryWrapper) {
        String customSqlSegment = getQueryWrapper().getSqlSegment();
        // 去除首尾括号
        if (customSqlSegment.equals("()")) {
            return "";
        } else if (customSqlSegment.startsWith("(") && customSqlSegment.endsWith(")")) {
            customSqlSegment = customSqlSegment.substring(1, customSqlSegment.length() - 1);
        }
        return getSqlSegmentWithParam(customSqlSegment, getQueryWrapper(), queryWrapper);
    }

    /**
     * 等于
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children eq(boolean condition, SFunction<T, V> column, V val) {
        return eq(condition, column, null, val);
    }

    /**
     * 等于
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children eq(boolean condition, SFunction<T, V> column, String rename, V val) {
        return eqFunc(condition, x -> x.column(column, rename), x -> x.value(val));
    }

    /**
     * 等于
     *
     * @param condition 条件
     * @param column1   列1
     * @param column2   列2
     * @param <T1>      实体类型1
     * @param <T2>      实体类型2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T1, T2, V> Children eqColumn(boolean condition, SFunction<T1, V> column1, SFunction<T2, V> column2) {
        return eqColumn(condition, column1, null, column2, null);
    }

    /**
     * 等于
     *
     * @param condition 条件
     * @param column1   列1
     * @param rename1   表重命名1
     * @param column2   列2
     * @param rename2   表重命名2
     * @param <T1>      实体类型1
     * @param <T2>      实体类型2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T1, T2, V> Children eqColumn(boolean condition, SFunction<T1, V> column1, String rename1, SFunction<T2, V> column2, String rename2) {
        return eqFunc(condition, x -> x.column(column1, rename1), x -> x.column(column2, rename2));
    }

    /**
     * 等于
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children eqFunc(boolean condition, Function<F, V> func1, Function<F, V> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + "=" + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 不等于
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children ne(boolean condition, SFunction<T, V> column, V val) {
        return ne(condition, column, null, val);
    }

    /**
     * 不等于
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children ne(boolean condition, SFunction<T, V> column, String rename, V val) {
        return neFunc(condition, x -> x.column(column, rename), x -> x.value(val));
    }

    /**
     * 不等于
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children neFunc(boolean condition, Function<F, V> func1, Function<F, V> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + "<>" + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 大于
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children gt(boolean condition, SFunction<T, V> column, V val) {
        return gt(condition, column, null, val);
    }

    /**
     * 大于
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children gt(boolean condition, SFunction<T, V> column, String rename, V val) {
        return gtFunc(condition, x -> x.column(column, rename), x -> x.value(val));
    }

    /**
     * 大于
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children gtFunc(boolean condition, Function<F, V> func1, Function<F, V> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + ">" + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 大于等于
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children ge(boolean condition, SFunction<T, V> column, V val) {
        return ge(condition, column, null, val);
    }

    /**
     * 大于等于
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children ge(boolean condition, SFunction<T, V> column, String rename, V val) {
        return geFunc(condition, x -> x.column(column, rename), x -> x.value(val));
    }

    /**
     * 大于等于
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children geFunc(boolean condition, Function<F, V> func1, Function<F, V> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + ">=" + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 小于
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children lt(boolean condition, SFunction<T, V> column, V val) {
        return lt(condition, column, null, val);
    }

    /**
     * 小于
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children lt(boolean condition, SFunction<T, V> column, String rename, V val) {
        return ltFunc(condition, x -> x.column(column, rename), x -> x.value(val));
    }

    /**
     * 小于
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children ltFunc(boolean condition, Function<F, V> func1, Function<F, V> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + "<" + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 小于等于
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children le(boolean condition, SFunction<T, V> column, V val) {
        return le(condition, column, null, val);
    }

    /**
     * 小于等于
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children le(boolean condition, SFunction<T, V> column, String rename, V val) {
        return leFunc(condition, x -> x.column(column, rename), x -> x.value(val));
    }

    /**
     * 小于等于
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children leFunc(boolean condition, Function<F, V> func1, Function<F, V> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + "<=" + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 字符串包含（不带通配符）
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children like(boolean condition, SFunction<T, V> column, String val) {
        return like(condition, column, null, val);
    }

    /**
     * 字符串包含（不带通配符）
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children like(boolean condition, SFunction<T, V> column, String rename, String val) {
        return likeFunc(condition, x -> (String) x.column(column, rename), x -> x.value(val));
    }

    /**
     * 字符串包含（不带通配符）
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @return 实例本身
     */
    public Children likeFunc(boolean condition, Function<F, String> func1, Function<F, String> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " LIKE " + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 字符串不包含
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children notLike(boolean condition, SFunction<T, V> column, String val) {
        return notLike(condition, column, null, val);
    }

    /**
     * 字符串不包含
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children notLike(boolean condition, SFunction<T, V> column, String rename, String val) {
        return notLikeFunc(condition, x -> (String) x.column(column, rename), x -> x.value(val));
    }

    /**
     * 字符串不包含
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @return 实例本身
     */
    public Children notLikeFunc(boolean condition, Function<F, String> func1, Function<F, String> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " NOT LIKE " + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 字符串包含
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children likeDefault(boolean condition, SFunction<T, V> column, String val) {
        return likeDefault(condition, column, null, val);
    }

    /**
     * 字符串包含
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children likeDefault(boolean condition, SFunction<T, V> column, String rename, String val) {
        return likeDefaultFunc(condition, x -> (String) x.column(column, rename), x -> x.value(val));
    }

    /**
     * 字符串包含
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @return 实例本身
     */
    public Children likeDefaultFunc(boolean condition, Function<F, String> func1, Function<F, String> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " LIKE CONCAT('%', " + getSubSqlSegment(func2, fClazz) + ", '%')");
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 字符串右包含
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children likeLeft(boolean condition, SFunction<T, V> column, String val) {
        return likeLeft(condition, column, null, val);
    }

    /**
     * 字符串右包含
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children likeLeft(boolean condition, SFunction<T, V> column, String rename, String val) {
        return likeLeftFunc(condition, x -> (String) x.column(column, rename), x -> x.value(val));
    }

    /**
     * 字符串右包含
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @return 实例本身
     */
    public Children likeLeftFunc(boolean condition, Function<F, String> func1, Function<F, String> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " LIKE CONCAT('%', " + getSubSqlSegment(func2, fClazz) + ")");
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 字符串左包含
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children likeRight(boolean condition, SFunction<T, V> column, String val) {
        return likeRight(condition, column, null, val);
    }

    /**
     * 字符串左包含
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children likeRight(boolean condition, SFunction<T, V> column, String rename, String val) {
        return likeRightFunc(condition, x -> (String) x.column(column, rename), x -> x.value(val));
    }

    /**
     * 字符串左包含
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @return 实例本身
     */
    public Children likeRightFunc(boolean condition, Function<F, String> func1, Function<F, String> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " LIKE CONCAT(" + getSubSqlSegment(func2, fClazz) + ", '%')");
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 在...之间
     *
     * @param condition 条件
     * @param column    列
     * @param val1      值1
     * @param val2      值2
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children between(boolean condition, SFunction<T, V> column, V val1, V val2) {
        return between(condition, column, null, val1, val2);
    }

    /**
     * 在...之间
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val1      值1
     * @param val2      值2
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children between(boolean condition, SFunction<T, V> column, String rename, V val1, V val2) {
        return betweenFunc(condition, x -> x.column(column, rename), x -> x.value(val1), x -> x.value(val2));
    }

    /**
     * 在...之间
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param func3     表达式3
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children betweenFunc(boolean condition, Function<F, V> func1, Function<F, V> func2, Function<F, V> func3) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " BETWEEN " + getSubSqlSegment(func2, fClazz) + " AND " + getSubSqlSegment(func3, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 不在...之间
     *
     * @param condition 条件
     * @param column    列
     * @param val1      值1
     * @param val2      值2
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children notBetween(boolean condition, SFunction<T, V> column, V val1, V val2) {
        return notBetween(condition, column, null, val1, val2);
    }

    /**
     * 不在...之间
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val1      值1
     * @param val2      值2
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children notBetween(boolean condition, SFunction<T, V> column, String rename, V val1, V val2) {
        return notBetweenFunc(condition, x -> x.column(column, rename), x -> x.value(val1), x -> x.value(val2));
    }

    /**
     * 不在...之间
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param func3     表达式3
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children notBetweenFunc(boolean condition, Function<F, V> func1, Function<F, V> func2, Function<F, V> func3) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " NOT BETWEEN " + getSubSqlSegment(func2, fClazz) + " AND " + getSubSqlSegment(func3, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 空值
     *
     * @param condition 条件
     * @param column    列
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children isNull(boolean condition, SFunction<T, V> column) {
        return isNull(condition, column, null);
    }

    /**
     * 空值
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children isNull(boolean condition, SFunction<T, V> column, String rename) {
        return isNullFunc(condition, x -> x.column(column, rename));
    }

    /**
     * 空值
     *
     * @param condition 条件
     * @param func      表达式
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children isNullFunc(boolean condition, Function<F, V> func) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func, fClazz) + " IS NULL");
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 非空值
     *
     * @param condition 条件
     * @param column    列
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children isNotNull(boolean condition, SFunction<T, V> column) {
        return isNotNull(condition, column, null);
    }

    /**
     * 非空值
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children isNotNull(boolean condition, SFunction<T, V> column, String rename) {
        return isNotNullFunc(condition, x -> x.column(column, rename));
    }

    /**
     * 非空值
     *
     * @param condition 条件
     * @param func      表达式
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children isNotNullFunc(boolean condition, Function<F, V> func) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func, fClazz) + " IS NOT NULL");
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 在...里
     *
     * @param condition 条件
     * @param column    列
     * @param coll      集合
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children in(boolean condition, SFunction<T, V> column, Collection<V> coll) {
        return in(condition, column, null, coll);
    }

    /**
     * 在...里
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param coll      集合
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children in(boolean condition, SFunction<T, V> column, String rename, Collection<V> coll) {
        try {
            // 去除空元素
            Collection<V> tempColl = coll == null ? new ArrayList<>() : coll.stream().filter(Objects::nonNull).collect(Collectors.toList());
            if (!tempColl.isEmpty()) {
                getQueryWrapper().in(condition, getFullColumnName(column, rename), tempColl);
            } else {
                getQueryWrapper().apply(condition, "{0}={1}", 1, 0);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 在...里
     *
     * @param condition 条件
     * @param column    列
     * @param values    多个值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T, V> Children in(boolean condition, SFunction<T, V> column, V... values) {
        List<V> valueList = ArrayUtils.isEmpty(values) ?
                new ArrayList<>() :
                Arrays.stream(values).collect(Collectors.toList());
        return in(condition, column, valueList);

    }

    /**
     * 在...里
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children inFunc(boolean condition, Function<F, V> func1, Function<F, Collection<V>> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " IN " + getSubSqlSegment(func2, fClazz));
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 在...里
     *
     * @param condition 条件
     * @param func      表达式
     * @param subSql    子查询
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children inSqlFunc(boolean condition, Function<F, V> func, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        try {
            if (condition) {
                SingleValueSubSqlLambdaQueryWrapper<V> subSqlLambda = new SingleValueSubSqlLambdaQueryWrapper<>();
                subSql.accept(subSqlLambda);
                getQueryWrapper().apply(getSubSqlSegment(func, fClazz) + " IN (" + subSqlLambda.getSqlSegment(getQueryWrapper()) + ")");
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 在子查询里
     *
     * @param condition 条件
     * @param column    列
     * @param subSql    子查询
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children inSubSql(boolean condition, SFunction<T, V> column, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return inSubSql(condition, column, null, subSql);
    }

    /**
     * 在子查询里
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param subSql    子查询
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children inSubSql(boolean condition, SFunction<T, V> column, String rename, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        try {
            SingleValueSubSqlLambdaQueryWrapper<V> subSqlLambda = new SingleValueSubSqlLambdaQueryWrapper<>();
            subSql.accept(subSqlLambda);
            getQueryWrapper().inSql(condition, getFullColumnName(column, rename), subSqlLambda.getSqlSegment(getQueryWrapper()));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 不在...里
     *
     * @param condition 条件
     * @param column    列
     * @param coll      集合
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children notIn(boolean condition, SFunction<T, V> column, Collection<V> coll) {
        return notIn(condition, column, null, coll);
    }

    /**
     * 不在...里
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param coll      集合
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children notIn(boolean condition, SFunction<T, V> column, String rename, Collection<V> coll) {
        try {
            // 去除空元素
            Collection<V> tempColl = coll == null ? new ArrayList<>() : coll.stream().filter(Objects::nonNull).collect(Collectors.toList());
            if (!tempColl.isEmpty()) {
                getQueryWrapper().notIn(condition, getFullColumnName(column, rename), tempColl);
            } else {
                getQueryWrapper().apply(condition, "{0}={1}", 1, 1);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 不在...里
     *
     * @param condition 条件
     * @param column    列
     * @param values    多个值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T, V> Children notIn(boolean condition, SFunction<T, V> column, V... values) {
        return notIn(condition, column, Arrays.stream(values).collect(Collectors.toList()));
    }

    /**
     * 不在...里
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children notInFunc(boolean condition, Function<F, V> func1, Function<F, V> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " NOT IN " + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 不在...里
     *
     * @param condition 条件
     * @param func      表达式
     * @param subSql    子查询
     * @param <V>       值类型
     * @return 实例本身
     */
    public <V> Children notInSqlFunc(boolean condition, Function<F, V> func, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        try {
            if (condition) {
                SingleValueSubSqlLambdaQueryWrapper<V> subSqlLambda = new SingleValueSubSqlLambdaQueryWrapper<>();
                subSql.accept(subSqlLambda);
                getQueryWrapper().apply(getSubSqlSegment(func, fClazz) + " NOT IN (" + subSqlLambda.getSqlSegment(getQueryWrapper()) + ")");
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 不在子查询里
     *
     * @param condition 条件
     * @param column    列
     * @param subSql    子查询
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children notInSubSql(boolean condition, SFunction<T, V> column, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return notInSubSql(condition, column, null, subSql);
    }

    /**
     * 不在子查询里
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param subSql    子查询
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children notInSubSql(boolean condition, SFunction<T, V> column, String rename, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        try {
            SingleValueSubSqlLambdaQueryWrapper<V> subSqlLambda = new SingleValueSubSqlLambdaQueryWrapper<>();
            subSql.accept(subSqlLambda);
            getQueryWrapper().notInSql(condition, getFullColumnName(column, rename), subSqlLambda.getSqlSegment(getQueryWrapper()));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 存在
     *
     * @param condition 条件
     * @param subSql    子查询
     * @return 实例本身
     */
    public Children exists(boolean condition, Consumer<NonValueSubSqlLambdaQueryWrapper> subSql) {
        NonValueSubSqlLambdaQueryWrapper subSqlLambda = new NonValueSubSqlLambdaQueryWrapper();
        subSql.accept(subSqlLambda);
        getQueryWrapper().exists(condition, subSqlLambda.getSqlSegment(getQueryWrapper()));
        return typedThis;
    }

    /**
     * 不存在
     *
     * @param condition 条件
     * @param subSql    子查询
     * @return 实例本身
     */
    public Children notExists(boolean condition, Consumer<NonValueSubSqlLambdaQueryWrapper> subSql) {
        NonValueSubSqlLambdaQueryWrapper subSqlLambda = new NonValueSubSqlLambdaQueryWrapper();
        subSql.accept(subSqlLambda);
        getQueryWrapper().notExists(condition, subSqlLambda.getSqlSegment(getQueryWrapper()));
        return typedThis;
    }

    /**
     * 正则
     *
     * @param condition 条件
     * @param column    列
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children regexp(boolean condition, SFunction<T, V> column, String val) {
        return regexp(condition, column, null, val);
    }

    /**
     * 正则
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param val       值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children regexp(boolean condition, SFunction<T, V> column, String rename, String val) {
        return regexpFunc(condition, x -> (String) x.column(column, rename), x -> x.value(val));
    }

    /**
     * 正则
     *
     * @param condition 条件
     * @param func1     表达式1
     * @param func2     表达式2
     * @return 实例本身
     */
    public Children regexpFunc(boolean condition, Function<F, String> func1, Function<F, String> func2) {
        try {
            if (condition) {
                getQueryWrapper().apply(getSubSqlSegment(func1, fClazz) + " REGEXP " + getSubSqlSegment(func2, fClazz));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 全文搜索
     *
     * @param condition 条件
     * @param val       值
     * @param columns   多列
     * @param <T>       实体类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T> Children match(boolean condition, String val, SFunction<T, String>... columns) {
        return match(condition, val, null, columns);
    }

    /**
     * 全文搜索
     *
     * @param condition 条件
     * @param val       值
     * @param rename    表重命名
     * @param columns   多列
     * @param <T>       实体类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T> Children match(boolean condition, String val, String rename, SFunction<T, String>... columns) {
        if (condition) {
            try {
                List<String> columnList = new ArrayList<>();
                for (SFunction<T, String> column : columns) {
                    columnList.add(getFullColumnName(column, rename));
                }
                getQueryWrapper().apply("MATCH (" + String.join(",", columnList) + ") AGAINST ({0})", val);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return typedThis;
    }

    /**
     * 全文搜索
     *
     * @param condition 条件
     * @param val       值
     * @param columns   多列
     * @param <T>       实体类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T> Children matchInBooleanMode(boolean condition, String val, SFunction<T, String>... columns) {
        return matchInBooleanMode(condition, val, null, columns);
    }

    /**
     * 全文搜索
     *
     * @param condition 条件
     * @param val       值
     * @param rename    表重命名
     * @param columns   多列
     * @param <T>       实体类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T> Children matchInBooleanMode(boolean condition, String val, String rename, SFunction<T, String>... columns) {
        if (condition) {
            try {
                List<String> columnList = new ArrayList<>();
                for (SFunction<T, String> column : columns) {
                    columnList.add(getFullColumnName(column, rename));
                }
                getQueryWrapper().apply("MATCH (" + String.join(",", columnList) + ") AGAINST ({0} IN BOOLEAN MODE)", val);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return typedThis;
    }

    /**
     * 与
     *
     * @param condition 条件
     * @param predicate 子条件
     * @return 实例本身
     */
    @SuppressWarnings("unchecked")
    public Children and(boolean condition, Consumer<Children> predicate) {
        try {
            if (condition) {
                String subSqlSegment = this.getSubSqlSegment(predicate, (Class<Children>) this.getClass());
                getQueryWrapper().and(!StringUtils.isEmpty(subSqlSegment), x -> x.apply(subSqlSegment));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 或
     *
     * @param condition 条件
     * @param predicate 子条件
     * @return 实例本身
     */
    @SuppressWarnings("unchecked")
    public Children or(boolean condition, Consumer<Children> predicate) {
        try {
            if (condition) {
                String subSqlSegment = this.getSubSqlSegment(predicate, (Class<Children>) this.getClass());
                getQueryWrapper().or(!StringUtils.isEmpty(subSqlSegment), x -> x.apply(subSqlSegment));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 非
     *
     * @param condition 条件
     * @param predicate 子条件
     * @return 实例本身
     */
    @SuppressWarnings("unchecked")
    public Children not(boolean condition, Consumer<Children> predicate) {
        try {
            if (condition) {
                String subSqlSegment = this.getSubSqlSegment(predicate, (Class<Children>) this.getClass());
                getQueryWrapper().not(!StringUtils.isEmpty(subSqlSegment), x -> x.apply(subSqlSegment));
            }
        } catch (InstantiationException | IllegalAccessException ignored) {
        }
        return typedThis;
    }

    /**
     * 或
     *
     * @param condition 条件
     * @return 实例本身
     */
    public Children or(boolean condition) {
        getQueryWrapper().or(condition);
        return typedThis;
    }

    /**
     * 等于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children eq(SFunction<T, V> column, V val) {
        return eq(true, column, val);
    }

    /**
     * 等于
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children eq(SFunction<T, V> column, String rename, V val) {
        return eq(true, column, rename, val);
    }

    /**
     * 等于
     *
     * @param column1 列1
     * @param column2 列2
     * @param <T1>    实体类型1
     * @param <T2>    实体类型2
     * @param <V>     值类型
     * @return 实例本身
     */
    public <T1, T2, V> Children eqColumn(SFunction<T1, V> column1, SFunction<T2, V> column2) {
        return eqColumn(true, column1, column2);
    }

    /**
     * 等于
     *
     * @param column1 列1
     * @param rename1 表重命名1
     * @param column2 列2
     * @param rename2 表重命名2
     * @param <T1>    实体类型1
     * @param <T2>    实体类型2
     * @param <V>     值类型
     * @return 实例本身
     */
    public <T1, T2, V> Children eqColumn(SFunction<T1, V> column1, String rename1, SFunction<T2, V> column2, String rename2) {
        return eqColumn(true, column1, rename1, column2, rename2);
    }

    /**
     * 等于
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children eqFunc(Function<F, V> func1, Function<F, V> func2) {
        return eqFunc(true, func1, func2);
    }

    /**
     * 不等于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children ne(SFunction<T, V> column, V val) {
        return ne(true, column, val);
    }

    /**
     * 不等于
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children ne(SFunction<T, V> column, String rename, V val) {
        return ne(true, column, rename, val);
    }

    /**
     * 不等于
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children neFunc(Function<F, V> func1, Function<F, V> func2) {
        return neFunc(true, func1, func2);
    }

    /**
     * 大于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children gt(SFunction<T, V> column, V val) {
        return gt(true, column, val);
    }

    /**
     * 大于
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children gt(SFunction<T, V> column, String rename, V val) {
        return gt(true, column, rename, val);
    }

    /**
     * 大于
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children gtFunc(Function<F, V> func1, Function<F, V> func2) {
        return gtFunc(true, func1, func2);
    }

    /**
     * 大于等于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children ge(SFunction<T, V> column, V val) {
        return ge(true, column, val);
    }

    /**
     * 大于等于
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children ge(SFunction<T, V> column, String rename, V val) {
        return ge(true, column, rename, val);
    }

    /**
     * 大于等于
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children geFunc(Function<F, V> func1, Function<F, V> func2) {
        return geFunc(true, func1, func2);
    }

    /**
     * 小于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children lt(SFunction<T, V> column, V val) {
        return lt(true, column, val);
    }

    /**
     * 小于
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children lt(SFunction<T, V> column, String rename, V val) {
        return lt(true, column, rename, val);
    }

    /**
     * 小于
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children ltFunc(Function<F, V> func1, Function<F, V> func2) {
        return ltFunc(true, func1, func2);
    }

    /**
     * 小于等于
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children le(SFunction<T, V> column, V val) {
        return le(true, column, val);
    }

    /**
     * 小于等于
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children le(SFunction<T, V> column, String rename, V val) {
        return le(true, column, rename, val);
    }

    /**
     * 小于等于
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children leFunc(Function<F, V> func1, Function<F, V> func2) {
        return leFunc(true, func1, func2);
    }

    /**
     * 在...之间
     *
     * @param column 列
     * @param val1   值1
     * @param val2   值2
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children between(SFunction<T, V> column, V val1, V val2) {
        return between(true, column, val1, val2);
    }

    /**
     * 在...之间
     *
     * @param column 列
     * @param rename 表重命名
     * @param val1   值1
     * @param val2   值2
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children between(SFunction<T, V> column, String rename, V val1, V val2) {
        return between(true, column, rename, val1, val2);
    }

    /**
     * 在...之间
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param func3 表达式3
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children betweenFunc(Function<F, V> func1, Function<F, V> func2, Function<F, V> func3) {
        return betweenFunc(true, func1, func2, func3);
    }

    /**
     * 不在...之间
     *
     * @param column 列
     * @param val1   值1
     * @param val2   值2
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children notBetween(SFunction<T, V> column, V val1, V val2) {
        return notBetween(true, column, val1, val2);
    }

    /**
     * 不在...之间
     *
     * @param column 列
     * @param rename 表重命名
     * @param val1   值1
     * @param val2   值2
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children notBetween(SFunction<T, V> column, String rename, V val1, V val2) {
        return notBetween(true, column, rename, val1, val2);
    }

    /**
     * 不在...之间
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param func3 表达式3
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children notBetweenFunc(Function<F, V> func1, Function<F, V> func2, Function<F, V> func3) {
        return notBetweenFunc(true, func1, func2, func3);
    }

    /**
     * 字符串包含（不带通配符）
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children like(SFunction<T, V> column, String val) {
        return like(true, column, val);
    }

    /**
     * 字符串包含（不带通配符）
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children like(SFunction<T, V> column, String rename, String val) {
        return like(true, column, rename, val);
    }

    /**
     * 字符串包含（不带通配符）
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @return 实例本身
     */
    public Children likeFunc(Function<F, String> func1, Function<F, String> func2) {
        return likeFunc(true, func1, func2);
    }

    /**
     * 字符串不包含
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children notLike(SFunction<T, V> column, String val) {
        return notLike(true, column, val);
    }

    /**
     * 字符串不包含
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children notLike(SFunction<T, V> column, String rename, String val) {
        return notLike(true, column, rename, val);
    }

    /**
     * 字符串不包含
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @return 实例本身
     */
    public Children notLikeFunc(Function<F, String> func1, Function<F, String> func2) {
        return notLikeFunc(true, func1, func2);
    }

    /**
     * 字符串包含
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children likeDefault(SFunction<T, V> column, String val) {
        return likeDefault(true, column, val);
    }

    /**
     * 字符串包含
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children likeDefault(SFunction<T, V> column, String rename, String val) {
        return likeDefault(true, column, rename, val);
    }

    /**
     * 字符串包含
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @return 实例本身
     */
    public Children likeDefaultFunc(Function<F, String> func1, Function<F, String> func2) {
        return likeDefaultFunc(true, func1, func2);
    }

    /**
     * 字符串右包含
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children likeLeft(SFunction<T, V> column, String val) {
        return likeLeft(true, column, val);
    }

    /**
     * 字符串右包含
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children likeLeft(SFunction<T, V> column, String rename, String val) {
        return likeLeft(true, column, rename, val);
    }

    /**
     * 字符串右包含
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @return 实例本身
     */
    public Children likeLeftFunc(Function<F, String> func1, Function<F, String> func2) {
        return likeLeftFunc(true, func1, func2);
    }

    /**
     * 字符串左包含
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children likeRight(SFunction<T, V> column, String val) {
        return likeRight(true, column, val);
    }

    /**
     * 字符串左包含
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children likeRight(SFunction<T, V> column, String rename, String val) {
        return likeRight(true, column, rename, val);
    }

    /**
     * 字符串左包含
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @return 实例本身
     */
    public Children likeRightFunc(Function<F, String> func1, Function<F, String> func2) {
        return likeRightFunc(true, func1, func2);
    }

    /**
     * 空值
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children isNull(SFunction<T, V> column) {
        return isNull(true, column);
    }

    /**
     * 空值
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children isNull(SFunction<T, V> column, String rename) {
        return isNull(true, column, rename);
    }

    /**
     * 空值
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 实例本身
     */
    public <V> Children isNullFunc(Function<F, V> func) {
        return isNullFunc(true, func);
    }

    /**
     * 非空值
     *
     * @param column 列
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children isNotNull(SFunction<T, V> column) {
        return isNotNull(true, column);
    }

    /**
     * 非空值
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children isNotNull(SFunction<T, V> column, String rename) {
        return isNotNull(true, column, rename);
    }

    /**
     * 非空值
     *
     * @param func 表达式
     * @param <V>  值类型
     * @return 实例本身
     */
    public <V> Children isNotNullFunc(Function<F, V> func) {
        return isNotNullFunc(true, func);
    }

    /**
     * 在...里
     *
     * @param column 列
     * @param coll   集合
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children in(SFunction<T, V> column, Collection<V> coll) {
        return in(true, column, coll);
    }

    /**
     * 在...里
     *
     * @param column 列
     * @param rename 表重命名
     * @param coll   集合
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children in(SFunction<T, V> column, String rename, Collection<V> coll) {
        return in(true, column, rename, coll);
    }

    /**
     * 在...里
     *
     * @param column 列
     * @param values 多个值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T, V> Children in(SFunction<T, V> column, V... values) {
        return in(true, column, values);
    }

    /**
     * 在...里
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children inFunc(Function<F, V> func1, Function<F, Collection<V>> func2) {
        return inFunc(true, func1, func2);
    }

    /**
     * 在...里
     *
     * @param func   表达式
     * @param subSql 子查询
     * @param <V>    值类型
     * @return 实例本身
     */
    public <V> Children inSqlFunc(Function<F, V> func, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return inSqlFunc(true, func, subSql);
    }

    /**
     * 在子查询里
     *
     * @param column 列
     * @param subSql 子查询
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children inSubSql(SFunction<T, V> column, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return inSubSql(true, column, subSql);
    }

    /**
     * 在子查询里
     *
     * @param column 列
     * @param rename 表重命名
     * @param subSql 子查询
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children inSubSql(SFunction<T, V> column, String rename, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return inSubSql(true, column, rename, subSql);
    }

    /**
     * 不在...里
     *
     * @param column 列
     * @param coll   集合
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children notIn(SFunction<T, V> column, Collection<V> coll) {
        return notIn(true, column, coll);
    }

    /**
     * 不在...里
     *
     * @param column 列
     * @param rename 表重命名
     * @param coll   集合
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children notIn(SFunction<T, V> column, String rename, Collection<V> coll) {
        return notIn(true, column, rename, coll);
    }

    /**
     * 不在...里
     *
     * @param column 列
     * @param values 多个值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T, V> Children notIn(SFunction<T, V> column, V... values) {
        return notIn(true, column, values);
    }

    /**
     * 不在...里
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @param <V>   值类型
     * @return 实例本身
     */
    public <V> Children notInFunc(Function<F, V> func1, Function<F, V> func2) {
        return notInFunc(true, func1, func2);
    }

    /**
     * 不在...里
     *
     * @param func   表达式
     * @param subSql 子查询
     * @param <V>    值类型
     * @return 实例本身
     */
    public <V> Children notInSqlFunc(Function<F, V> func, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return notInSqlFunc(true, func, subSql);
    }

    /**
     * 不在子查询里
     *
     * @param column 列
     * @param subSql 子查询
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children notInSubSql(SFunction<T, V> column, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return notInSubSql(true, column, subSql);
    }

    /**
     * 不在子查询里
     *
     * @param column 列
     * @param rename 表重命名
     * @param subSql 子查询
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children notInSubSql(SFunction<T, V> column, String rename, Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql) {
        return notInSubSql(true, column, rename, subSql);
    }

    /**
     * 存在
     *
     * @param subSql 子查询
     * @return 实例本身
     */
    public Children exists(Consumer<NonValueSubSqlLambdaQueryWrapper> subSql) {
        return exists(true, subSql);
    }

    /**
     * 不存在
     *
     * @param subSql 子查询
     * @return 实例本身
     */
    public Children notExists(Consumer<NonValueSubSqlLambdaQueryWrapper> subSql) {
        return notExists(true, subSql);
    }

    /**
     * 正则
     *
     * @param column 列
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children regexp(SFunction<T, V> column, String val) {
        return regexp(true, column, val);
    }

    /**
     * 正则
     *
     * @param column 列
     * @param rename 表重命名
     * @param val    值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children regexp(SFunction<T, V> column, String rename, String val) {
        return regexp(true, column, rename, val);
    }

    /**
     * 正则
     *
     * @param func1 表达式1
     * @param func2 表达式2
     * @return 实例本身
     */
    public Children regexpFunc(Function<F, String> func1, Function<F, String> func2) {
        return regexpFunc(true, func1, func2);
    }

    /**
     * 全文搜索
     *
     * @param val     值
     * @param columns 多列
     * @param <T>     实体类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T> Children match(String val, SFunction<T, String>... columns) {
        return match(true, val, columns);
    }

    /**
     * 全文搜索
     *
     * @param val     值
     * @param rename  表重命名
     * @param columns 多列
     * @param <T>     实体类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T> Children match(String val, String rename, SFunction<T, String>... columns) {
        return match(true, val, rename, columns);
    }

    /**
     * 全文搜索
     *
     * @param val     值
     * @param columns 多列
     * @param <T>     实体类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T> Children matchInBooleanMode(String val, SFunction<T, String>... columns) {
        return matchInBooleanMode(true, val, columns);
    }

    /**
     * 全文搜索
     *
     * @param val     值
     * @param rename  表重命名
     * @param columns 多列
     * @param <T>     实体类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T> Children matchInBooleanMode(String val, String rename, SFunction<T, String>... columns) {
        return matchInBooleanMode(true, val, rename, columns);
    }

    /**
     * 与
     *
     * @param predicate 子条件
     * @return 实例本身
     */
    public Children and(Consumer<Children> predicate) {
        return and(true, predicate);
    }

    /**
     * 或
     *
     * @param predicate 子条件
     * @return 实例本身
     */
    public Children or(Consumer<Children> predicate) {
        return or(true, predicate);
    }

    /**
     * 非
     *
     * @param predicate 子条件
     * @return 实例本身
     */
    public Children not(Consumer<Children> predicate) {
        return not(true, predicate);
    }

    /**
     * 或
     *
     * @return 实例本身
     */
    public Children or() {
        return or(true);
    }

    /**
     * 恒真条件，拼接1=1
     *
     * @return 实例本身
     */
    public Children _true() {
        getQueryWrapper().apply("{0}={1}", 1, 1);
        return typedThis;
    }

    /**
     * 空或空字符串
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 实例本身
     */
    public <T> Children isEmpty(SFunction<T, String> column) {
        return isEmpty(true, column);
    }

    /**
     * 空或空字符串
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 实例本身
     */
    public <T> Children isEmpty(SFunction<T, String> column, String rename) {
        return isEmpty(true, column, rename);
    }

    /**
     * 非空且非空字符串
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 实例本身
     */
    public <T> Children isNotEmpty(SFunction<T, String> column) {
        return isNotEmpty(true, column);
    }

    /**
     * 非空且非空字符串
     *
     * @param column 列
     * @param rename 表重命名
     * @param <T>    实体类型
     * @return 实例本身
     */
    public <T> Children isNotEmpty(SFunction<T, String> column, String rename) {
        return isNotEmpty(true, column, rename);
    }

    /**
     * 空或空字符串
     *
     * @param condition 条件
     * @param column    列
     * @param <T>       实体类型
     * @return 实例本身
     */
    public <T> Children isEmpty(boolean condition, SFunction<T, String> column) {
        return isEmpty(condition, column, null);
    }

    /**
     * 空或空字符串
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param <T>       实体类型
     * @return 实例本身
     */
    public <T> Children isEmpty(boolean condition, SFunction<T, String> column, String rename) {
        if (condition) {
            this.and(x -> x.isNull(column, rename).or().eqFunc(y -> y.charLengthFunc(z -> z.trim(column, rename)), y -> y.value(0)));
        }
        return typedThis;
    }

    /**
     * 非空且非空字符串
     *
     * @param condition 条件
     * @param column    列
     * @param <T>       实体类型
     * @return 实例本身
     */
    public <T> Children isNotEmpty(boolean condition, SFunction<T, String> column) {
        return isNotEmpty(condition, column, null);
    }

    /**
     * 非空且非空字符串
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param <T>       实体类型
     * @return 实例本身
     */
    public <T> Children isNotEmpty(boolean condition, SFunction<T, String> column, String rename) {
        if (condition) {
            this.isNotNull(column, rename).gtFunc(x -> x.charLengthFunc(y -> y.trim(column, rename)), x -> x.value(0));
        }
        return typedThis;
    }

    /**
     * 逗号隔开列包含任意值
     *
     * @param column 列
     * @param coll   集合
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children containAny(SFunction<T, String> column, Collection<V> coll) {
        return containAny(true, column, coll);
    }

    /**
     * 逗号隔开列包含任意值
     *
     * @param column 列
     * @param rename 表重命名
     * @param coll   集合
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    public <T, V> Children containAny(SFunction<T, String> column, String rename, Collection<V> coll) {
        return containAny(true, column, rename, coll);
    }

    /**
     * 逗号隔开列包含任意值
     *
     * @param column 列
     * @param rename 表重命名
     * @param values 多个值
     * @param <T>    实体类型
     * @param <V>    值类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T, V> Children containAny(SFunction<T, String> column, String rename, V... values) {
        return containAny(true, column, rename, values);
    }

    /**
     * 逗号隔开列包含任意值
     *
     * @param condition 条件
     * @param column    列
     * @param coll      集合
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children containAny(boolean condition, SFunction<T, String> column, Collection<V> coll) {
        return containAny(condition, column, null, coll);
    }

    /**
     * 逗号隔开列包含任意值
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param coll      集合
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    public <T, V> Children containAny(boolean condition, SFunction<T, String> column, String rename, Collection<V> coll) {
        if (condition) {
            String regexp = coll.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(",|")) + ",";
            this.regexpFunc(y -> y.concatFunc(z -> z.column(column, rename), z -> z.value(",")), y -> y.value(regexp));
        }
        return typedThis;
    }

    /**
     * 逗号隔开列包含任意值
     *
     * @param condition 条件
     * @param column    列
     * @param rename    表重命名
     * @param values    多个值
     * @param <T>       实体类型
     * @param <V>       值类型
     * @return 实例本身
     */
    @SafeVarargs
    public final <T, V> Children containAny(boolean condition, SFunction<T, String> column, String rename, V... values) {
        return containAny(condition, column, rename, Arrays.stream(values).collect(Collectors.toList()));
    }

    /**
     * 获取包含已删除数据
     *
     * @param condition 条件
     * @return 实例本身
     */
    public final Children withDeleted(boolean condition) {
        if (condition) {
            return withDeleted();
        } else {
            return typedThis;
        }
    }

    /**
     * 获取包含已删除数据
     *
     * @return 实例本身
     */
    public final Children withDeleted() {
        getQueryWrapper().setWithDeleted(true);
        return typedThis;
    }

}
