package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 选择表达式包装
 */
public final class SubSelectLambdaQueryWrapper extends AbstractSelectLambdaQueryWrapper<SubSelectLambdaQueryWrapper> {
    public SubSelectLambdaQueryWrapper() {
        this(null);
    }

    public SubSelectLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

    /**
     * 查询实体里所有字段
     *
     * @return 实例本身
     */
    public SubSelectLambdaQueryWrapper selectAll() {
        return selectAll(null);
    }

    /**
     * 查询实体里所有字段
     *
     * @param rename 表重命名
     * @return 实例本身
     */
    public SubSelectLambdaQueryWrapper selectAll(String rename) {
        if (StringUtils.isEmpty(rename)) {
            getQueryWrapper().select("*");
        } else {
            getQueryWrapper().select(rename + ".*");
        }
        return typedThis;
    }

    /**
     * 筛选查询项(子查询使用)
     *
     * @param selectColumn 查询列
     * @param <T>          查询实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T, V> SubSelectLambdaQueryWrapper select(SFunction<T, V> selectColumn) {
        return select(selectColumn, null);
    }

    /**
     * 筛选查询项(子查询使用)
     *
     * @param selectColumn 查询列
     * @param rename       表重命名
     * @param <T>          查询实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T, V> SubSelectLambdaQueryWrapper select(SFunction<T, V> selectColumn, String rename) {
        try {
            getQueryWrapper().addSelect(getFullColumnName(selectColumn, rename), getColumnName(selectColumn));
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
     * @param <T>          查询实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T, V> SubSelectLambdaQueryWrapper selectCase(Consumer<GroupWhereLambdaQueryWrapper> caseWhen, V thenValue, V elseValue, SFunction<T, V> columnRename) {
        return selectCase(x -> x.whenThenValue(caseWhen, thenValue).elseValue(elseValue), columnRename);
    }

    /**
     * 条件查询项
     *
     * @param _case        case条件表达式
     * @param columnRename 重命名列
     * @param <T>          查询实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T, V> SubSelectLambdaQueryWrapper selectCase(Consumer<GroupCaseLambdaQueryWrapper<V>> _case, SFunction<T, V> columnRename) {
        try {
            GroupCaseLambdaQueryWrapper<V> caseLambda = new GroupCaseLambdaQueryWrapper<>();
            _case.accept(caseLambda);
            getQueryWrapper().addSelect(caseLambda.getSqlSegment(getQueryWrapper()), getColumnName(columnRename));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 筛选函数表达式
     *
     * @param func         函数表达式
     * @param columnRename 重命名列
     * @param <T>          查询实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T, V> SubSelectLambdaQueryWrapper selectFunc(Function<GroupFunctionLambdaQueryWrapper, V> func, SFunction<T, V> columnRename) {
        try {
            if (columnRename != null) {
                getQueryWrapper().addSelect(getSubSqlSegment(func, GroupFunctionLambdaQueryWrapper.class), getColumnName(columnRename));
            } else {
                getQueryWrapper().addSelect(getSubSqlSegment(func, GroupFunctionLambdaQueryWrapper.class), "COLUMN_KEY");
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 筛选函数表达式
     *
     * @param func         函数表达式
     * @param columnRename 重命名列
     * @param <T>          查询实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T, V> SubSelectLambdaQueryWrapper selectFunc(Function<GroupFunctionLambdaQueryWrapper, V> func, String columnRename) {
        try {
            if (columnRename != null) {
                getQueryWrapper().addSelect(getSubSqlSegment(func, GroupFunctionLambdaQueryWrapper.class), columnRename);
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
     * @param <T>          查询实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T, V> SubSelectLambdaQueryWrapper selectSubSql(Consumer<SingleValueSubSqlLambdaQueryWrapper<V>> subSql, SFunction<T, V> columnRename) {
        try {
            SingleValueSubSqlLambdaQueryWrapper<V> subSqlLambda = new SingleValueSubSqlLambdaQueryWrapper<>();
            subSql.accept(subSqlLambda);
            if (columnRename != null) {
                getQueryWrapper().addSelect("(" + subSqlLambda.getSqlSegment(getQueryWrapper()) + ")", getColumnName(columnRename));
            } else {
                getQueryWrapper().addSelect("(" + subSqlLambda.getSqlSegment(getQueryWrapper()) + ")", "COLUMN_KEY");
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

}
