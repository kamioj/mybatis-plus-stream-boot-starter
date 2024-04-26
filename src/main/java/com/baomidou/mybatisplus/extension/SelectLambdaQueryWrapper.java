package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 选择表达式包装
 *
 * @param <R> 输出类型
 */
public final class SelectLambdaQueryWrapper<R> extends AbstractSelectLambdaQueryWrapper<SelectLambdaQueryWrapper<R>> {
    private final AtomicInteger advColumnNameKeySeq = new AtomicInteger(0);

    private final String seqKey;

    public SelectLambdaQueryWrapper() {
        this(null);
    }

    public SelectLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        this(queryWrapper, "");
    }

    public SelectLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper, String seqKey) {
        super(queryWrapper);
        this.seqKey = seqKey;
    }

    /**
     * 查询实体里所有字段
     *
     * @param entityClass 实体类型
     * @return 实例本身
     */
    public SelectLambdaQueryWrapper<R> selectAll(Class<R> entityClass) {
        return selectAll(entityClass, null);
    }

    /**
     * 查询实体里所有字段
     *
     * @param entityClass 实体类型
     * @param tableRename 表重命名
     * @return 实例本身
     */
    public SelectLambdaQueryWrapper<R> selectAll(Class<R> entityClass, String tableRename) {
        Field[] renameFields = ReflectUtils.getDeclaredFields(entityClass, Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
        for (Field renameField : renameFields) {
            try {
                getQueryWrapper().addSelect(getFullColumnName(entityClass, renameField.getName(), tableRename), getColumnRename(renameField.getName(), seqKey));
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return typedThis;
    }

    /**
     * 筛选查询项
     *
     * @param selectColumn 查询列
     * @param <V>          值类型
     * @return 实例本身
     */
    public <V> SelectLambdaQueryWrapper<R> select(SFunction<R, V> selectColumn) {
        return select(selectColumn, "");
    }

    /**
     * 筛选查询项
     *
     * @param selectColumn 查询列
     * @param tableRename  表重命名
     * @param <V>          值类型
     * @return 实例本身
     */
    public <V> SelectLambdaQueryWrapper<R> select(SFunction<R, V> selectColumn, String tableRename) {
        return select(selectColumn, tableRename, null);
    }

    /**
     * 筛选查询项
     *
     * @param selectColumn 查询列
     * @param voColumn     输出列
     * @param <T>          查询实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T, V> SelectLambdaQueryWrapper<R> select(SFunction<T, V> selectColumn, SFunction<R, V> voColumn) {
        return select(selectColumn, null, voColumn);
    }

    /**
     * 筛选查询项
     *
     * @param selectColumn 查询列
     * @param tableRename  表重命名
     * @param voColumn     输出列
     * @param <T>          查询实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T, V> SelectLambdaQueryWrapper<R> select(SFunction<T, V> selectColumn, String tableRename, SFunction<R, V> voColumn) {
        try {
            if (voColumn != null) {
                getQueryWrapper().addSelect(getFullColumnName(selectColumn, tableRename), getColumnRename(voColumn, seqKey));
            } else {
                getQueryWrapper().addSelect(getFullColumnName(selectColumn, tableRename), getColumnRename(selectColumn, seqKey));
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 条件查询项
     *
     * @param caseWhen     满足条件表达式
     * @param thenValue    满足条件值
     * @param elseValue    不满足条件值
     * @param columnRename 重命名列
     * @param <V>          值类型
     * @return 实例本身
     */
    public <V> SelectLambdaQueryWrapper<R> selectCase(Consumer<GroupWhereLambdaQueryWrapper> caseWhen, V thenValue, V elseValue, SFunction<R, V> columnRename) {
        return selectCase(x -> x.whenThenValue(caseWhen, thenValue).elseValue(elseValue), columnRename);
    }

    /**
     * 条件查询项
     *
     * @param _case        case条件表达式
     * @param columnRename 重命名列
     * @param <V>          值类型
     * @return 实例本身
     */
    public <V> SelectLambdaQueryWrapper<R> selectCase(Consumer<GroupCaseLambdaQueryWrapper<V>> _case, SFunction<R, V> columnRename) {
        return selectFunc(x -> x._case(_case), columnRename);
    }

    /**
     * 筛选函数表达式
     *
     * @param func         函数表达式
     * @param columnRename 重命名列
     * @param <V>          值类型
     * @return 实例本身
     */
    public <V> SelectLambdaQueryWrapper<R> selectFunc(Function<GroupFunctionLambdaQueryWrapper, V> func, SFunction<R, V> columnRename) {
        try {
            if (columnRename != null) {
                getQueryWrapper().addSelect(getSubSqlSegment(func, GroupFunctionLambdaQueryWrapper.class), getColumnRename(columnRename, seqKey));
            } else {
                getQueryWrapper().addSelect(getSubSqlSegment(func, GroupFunctionLambdaQueryWrapper.class), "COLUMN_KEY");
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 查询子查询
     *
     * @param subSql       子查询
     * @param columnRename 重命名列
     * @param <V>          值类型
     * @return 实例本身
     */
    public <V> SelectLambdaQueryWrapper<R> selectSubSql(Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql, SFunction<R, V> columnRename) {
        try {
            SingleValueSubSqlLambdaQueryWrapper<V> subSqlLambda = new SingleValueSubSqlLambdaQueryWrapper<>();
            subSql.accept(subSqlLambda);
            getQueryWrapper().addSelect("(" + subSqlLambda.getSqlSegment(getQueryWrapper()) + ")", getColumnRename(columnRename, seqKey));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 按字段名称自动匹配所有字段
     *
     * @param renameClass 输出类型
     * @param tClass      匹配表，字段一样取第一个匹配表
     */
    @SafeVarargs
    @Deprecated
    public final void selectAuto(Class<R> renameClass, Class<?>... tClass) {
        Field[] renameFields = ReflectUtils.getDeclaredFields(renameClass, Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
        for (Field renameField : renameFields) {
            for (Class<?> c : tClass) {
                try {
                    if (Arrays.stream(ReflectUtils.getDeclaredFields(c, Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)).anyMatch(x -> x.getName().equals(renameField.getName()))) {
                        getQueryWrapper().addSelect(getFullColumnName(c, renameField.getName(), getQueryWrapper().getRename(getTableName(c), null)), getColumnRename(renameField.getName(), seqKey));
                        break;
                    }
                } catch (ReflectiveOperationException ignored) {
                }

            }
        }
    }

}
