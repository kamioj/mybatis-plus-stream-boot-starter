package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.toolkit.MybatisUtil;

import java.util.function.Consumer;

/**
 * 连表表达式包装
 */
public final class JoinLambdaQueryWrapper<T> extends LambdaQueryWrapper<JoinLambdaQueryWrapper<T>> {

    public JoinLambdaQueryWrapper(Class<T> clazz, String rename) {
        this(null, clazz, rename);
    }

    public JoinLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper, Class<T> clazz, String rename) {
        super(queryWrapper);
        try {
            getQueryWrapper().setRename(getTableName(clazz), rename);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    /**
     * 左连
     *
     * @param joinClass    连表实体类型
     * @param sourceColumn 源列
     * @param joinColumn   连表列
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> leftJoin(Class<T2> joinClass, SFunction<T1, V> sourceColumn, SFunction<T2, V> joinColumn) {
        return leftJoin(joinClass, sourceColumn, null, joinColumn, null);
    }

    /**
     * 左连
     *
     * @param joinClass    连表实体类型
     * @param joinRename   连表重命名
     * @param sourceColumn 源列
     * @param joinColumn   连表列
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> leftJoin(Class<T2> joinClass, String joinRename, SFunction<T1, V> sourceColumn, SFunction<T2, V> joinColumn) {
        return leftJoin(joinClass, sourceColumn, null, joinColumn, joinRename);
    }

    /**
     * 左连
     *
     * @param joinClass    连表实体类型
     * @param sourceColumn 源列
     * @param sourceRename 源表重命名
     * @param joinColumn   连表列
     * @param joinRename   连表重命名
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> leftJoin(Class<T2> joinClass, SFunction<T1, V> sourceColumn, String sourceRename, SFunction<T2, V> joinColumn, String joinRename) {
        try {
            String finalSourceRename = getQueryWrapper().getRename(getTableName(sourceColumn), sourceRename);
            return leftJoin(joinClass, joinRename, x -> x.eqColumn(joinColumn, joinRename, sourceColumn, finalSourceRename));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 左连
     *
     * @param joinClass 连表实体类型
     * @param predicate 连表条件
     * @param <T2>      连表实体类型
     * @return 实例本身
     */
    public <T2> JoinLambdaQueryWrapper<T> leftJoin(Class<T2> joinClass, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return leftJoin(joinClass, null, predicate);
    }

    /**
     * 左连
     *
     * @param joinClass  连表实体类型
     * @param joinRename 连表重命名
     * @param predicate  连表条件
     * @param <T2>       连表实体类型
     * @return 实例本身
     */
    public <T2> JoinLambdaQueryWrapper<T> leftJoin(Class<T2> joinClass, String joinRename, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return join(joinClass, joinRename, "LEFT", predicate);
    }

    /**
     * 左连
     *
     * @param subSql     子查询
     * @param joinRename 连表重命名
     * @param predicate  连表条件
     * @return 实例本身
     */
    public JoinLambdaQueryWrapper<T> leftJoin(Consumer<SubSqlLambdaQueryWrapper> subSql, String joinRename, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return join(subSql, joinRename, "LEFT", predicate);
    }

    /**
     * 右连
     *
     * @param joinClass    连表实体类型
     * @param sourceColumn 源列
     * @param joinColumn   连表列
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> rightJoin(Class<T2> joinClass, SFunction<T1, V> sourceColumn, SFunction<T2, V> joinColumn) {
        return rightJoin(joinClass, sourceColumn, null, joinColumn, null);
    }

    /**
     * 右连
     *
     * @param joinClass    连表实体类型
     * @param joinRename   连表重命名
     * @param sourceColumn 源列
     * @param joinColumn   连表列
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> rightJoin(Class<T2> joinClass, String joinRename, SFunction<T1, V> sourceColumn, SFunction<T2, V> joinColumn) {
        return rightJoin(joinClass, sourceColumn, null, joinColumn, joinRename);
    }

    /**
     * 右连
     *
     * @param joinClass    连表实体类型
     * @param sourceColumn 源列
     * @param sourceRename 源表重命名
     * @param joinColumn   连表列
     * @param joinRename   连表重命名
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> rightJoin(Class<T2> joinClass, SFunction<T1, V> sourceColumn, String sourceRename, SFunction<T2, V> joinColumn, String joinRename) {
        try {
            String finalSourceRename = getQueryWrapper().getRename(getTableName(sourceColumn), sourceRename);
            return rightJoin(joinClass, joinRename, x -> x.eqColumn(joinColumn, joinRename, sourceColumn, finalSourceRename));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 右连
     *
     * @param joinClass 连表实体类型
     * @param predicate 连表条件
     * @param <T2>      连表实体类型
     * @return 实例本身
     */
    public <T2> JoinLambdaQueryWrapper<T> rightJoin(Class<T2> joinClass, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return rightJoin(joinClass, null, predicate);
    }

    /**
     * 右连
     *
     * @param joinClass  连表实体类型
     * @param joinRename 连表重命名
     * @param predicate  连表条件
     * @param <T2>       连表实体类型
     * @return 实例本身
     */
    public <T2> JoinLambdaQueryWrapper<T> rightJoin(Class<T2> joinClass, String joinRename, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return join(joinClass, joinRename, "RIGHT", predicate);
    }

    /**
     * 右连
     *
     * @param subSql     子查询
     * @param joinRename 连表重命名
     * @param predicate  连表条件
     * @return 实例本身
     */
    public JoinLambdaQueryWrapper<T> rightJoin(Consumer<SubSqlLambdaQueryWrapper> subSql, String joinRename, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return join(subSql, joinRename, "RIGHT", predicate);
    }

    /**
     * 内连
     *
     * @param joinClass    连表实体类型
     * @param sourceColumn 源列
     * @param joinColumn   连表列
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> innerJoin(Class<T2> joinClass, SFunction<T1, V> sourceColumn, SFunction<T2, V> joinColumn) {
        return innerJoin(joinClass, sourceColumn, null, joinColumn, null);
    }

    /**
     * 内连
     *
     * @param joinClass    连表实体类型
     * @param joinRename   连表重命名
     * @param sourceColumn 源列
     * @param joinColumn   连表列
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> innerJoin(Class<T2> joinClass, String joinRename, SFunction<T1, V> sourceColumn, SFunction<T2, V> joinColumn) {
        return innerJoin(joinClass, sourceColumn, null, joinColumn, joinRename);
    }

    /**
     * 内连
     *
     * @param joinClass    连表实体类型
     * @param sourceColumn 源列
     * @param sourceRename 源表重命名
     * @param joinColumn   连表列
     * @param joinRename   连表重命名
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> innerJoin(Class<T2> joinClass, SFunction<T1, V> sourceColumn, String sourceRename, SFunction<T2, V> joinColumn, String joinRename) {
        try {
            String finalSourceRename = getQueryWrapper().getRename(getTableName(sourceColumn), sourceRename);
            return innerJoin(joinClass, joinRename, x -> x.eqColumn(joinColumn, joinRename, sourceColumn, finalSourceRename));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 内连
     *
     * @param joinClass 连表实体类型
     * @param predicate 连表条件
     * @param <T2>      连表实体类型
     * @return 实例本身
     */
    public <T2> JoinLambdaQueryWrapper<T> innerJoin(Class<T2> joinClass, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return innerJoin(joinClass, null, predicate);
    }

    /**
     * 内连
     *
     * @param joinClass  连表实体类型
     * @param joinRename 连表重命名
     * @param predicate  连表条件
     * @param <T2>       连表实体类型
     * @return 实例本身
     */
    public <T2> JoinLambdaQueryWrapper<T> innerJoin(Class<T2> joinClass, String joinRename, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return join(joinClass, joinRename, "INNER", predicate);
    }

    /**
     * 右连
     *
     * @param subSql     子查询
     * @param joinRename 连表重命名
     * @param predicate  连表条件
     * @return 实例本身
     */
    public JoinLambdaQueryWrapper<T> innerJoin(Consumer<SubSqlLambdaQueryWrapper> subSql, String joinRename, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return join(subSql, joinRename, "INNER", predicate);
    }

    /**
     * 笛卡尔积连
     *
     * @param joinClass    连表实体类型
     * @param sourceColumn 源列
     * @param joinColumn   连表列
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> crossJoin(Class<T2> joinClass, SFunction<T1, V> sourceColumn, SFunction<T2, V> joinColumn) {
        return crossJoin(joinClass, sourceColumn, null, joinColumn, null);
    }

    /**
     * 笛卡尔积连
     *
     * @param joinClass    连表实体类型
     * @param joinRename   连表重命名
     * @param sourceColumn 源列
     * @param joinColumn   连表列
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> crossJoin(Class<T2> joinClass, String joinRename, SFunction<T1, V> sourceColumn, SFunction<T2, V> joinColumn) {
        return crossJoin(joinClass, sourceColumn, null, joinColumn, joinRename);
    }

    /**
     * 笛卡尔积连
     *
     * @param joinClass    连表实体类型
     * @param sourceColumn 源列
     * @param sourceRename 源表重命名
     * @param joinColumn   连表列
     * @param joinRename   连表重命名
     * @param <T1>         源实体类型
     * @param <T2>         连表实体类型
     * @param <V>          值类型
     * @return 实例本身
     */
    public <T1, T2, V> JoinLambdaQueryWrapper<T> crossJoin(Class<T2> joinClass, SFunction<T1, V> sourceColumn, String sourceRename, SFunction<T2, V> joinColumn, String joinRename) {
        try {
            String finalSourceRename = getQueryWrapper().getRename(getTableName(sourceColumn), sourceRename);
            return crossJoin(joinClass, joinRename, x -> x.eqColumn(joinColumn, joinRename, sourceColumn, finalSourceRename));
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 笛卡尔积连
     *
     * @param joinClass 连表实体类型
     * @param predicate 连表条件
     * @param <T2>      连表实体类型
     * @return 实例本身
     */
    public <T2> JoinLambdaQueryWrapper<T> crossJoin(Class<T2> joinClass, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return crossJoin(joinClass, null, predicate);
    }

    /**
     * 笛卡尔积连
     *
     * @param joinClass  连表实体类型
     * @param joinRename 连表重命名
     * @param predicate  连表条件
     * @param <T2>       连表实体类型
     * @return 实例本身
     */
    public <T2> JoinLambdaQueryWrapper<T> crossJoin(Class<T2> joinClass, String joinRename, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return join(joinClass, joinRename, "CROSS", predicate);
    }

    /**
     * 笛卡尔积连
     *
     * @param subSql     子查询
     * @param joinRename 连表重命名
     * @param predicate  连表条件
     * @return 实例本身
     */
    public JoinLambdaQueryWrapper<T> crossJoin(Consumer<SubSqlLambdaQueryWrapper> subSql, String joinRename, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return join(subSql, joinRename, "CROSS", predicate);
    }

    /**
     * 连表
     *
     * @param joinClass  连表实体类型
     * @param joinRename 连表重命名
     * @param joinType   连表类型
     * @param predicate  连表条件
     * @return 实例本身
     */
    @SuppressWarnings("unchecked")
    private <JT> JoinLambdaQueryWrapper<T> join(Class<JT> joinClass, String joinRename, String joinType, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(joinType).append(" JOIN \n ").append(getTableName(joinClass));

            getQueryWrapper().setRename(getTableName(joinClass), joinRename);
            if (!StringUtils.isEmpty(joinRename)) {
                sb.append(" AS ").append(getQueryWrapper().getRename(getTableName(joinClass), joinRename));
            }
            sb.append(" ON ");
            NormalWhereLambdaQueryWrapper whereLambda = new NormalWhereLambdaQueryWrapper();
            predicate.accept(whereLambda);
            ((ExQueryWrapper<JT>) whereLambda.getQueryWrapper()).setFromTable(MybatisUtil.getTableInfo(joinClass), joinRename);
            sb.append(whereLambda.getSqlSegment(getQueryWrapper()));
            getQueryWrapper().addJoinSql(sb.toString());
        } catch (ReflectiveOperationException ignored) {
        }
        return typedThis;
    }

    /**
     * 左连
     *
     * @param subSql     子查询
     * @param joinRename 连表重命名
     * @param joinType   连表类型
     * @param predicate  连表条件
     * @return 实例本身
     */
    private JoinLambdaQueryWrapper<T> join(Consumer<SubSqlLambdaQueryWrapper> subSql, String joinRename, String joinType, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        StringBuilder sb = new StringBuilder();
        sb.append(joinType).append(" JOIN \n ");
        SubSqlLambdaQueryWrapper subSqlLambda = new SubSqlLambdaQueryWrapper();
        subSql.accept(subSqlLambda);
        sb.append("(").append(subSqlLambda.getSqlSegment(getQueryWrapper())).append(") AS ").append(joinRename);
        sb.append(" ON ");
        NormalWhereLambdaQueryWrapper whereLambda = new NormalWhereLambdaQueryWrapper();
        predicate.accept(whereLambda);
        sb.append(whereLambda.getSqlSegment(getQueryWrapper()));
        getQueryWrapper().addJoinSql(sb.toString());
        return typedThis;
    }

}
