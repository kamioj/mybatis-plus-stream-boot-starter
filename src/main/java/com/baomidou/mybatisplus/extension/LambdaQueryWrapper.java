package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.toolkit.MybatisUtil;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.lang.invoke.SerializedLambda;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 抽象表达式包装
 *
 * @param <Children> 子类类型
 */
public abstract class LambdaQueryWrapper<Children extends LambdaQueryWrapper<Children>> {
    @SuppressWarnings("unchecked")
    protected final Children typedThis = (Children) this;

    private final ExQueryWrapper<?> queryWrapper;

    public LambdaQueryWrapper() {
        this(null);
    }

    public LambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        if (queryWrapper == null) {
            queryWrapper = new ExQueryWrapper<>();
        }
        this.queryWrapper = queryWrapper;
    }

    public ExQueryWrapper<?> getQueryWrapper() {
        return this.queryWrapper;
    }

    /**
     * 从源queryWrapper传入目标queryWrapper注入参数
     *
     * @param sourceQueryWrapper 源queryWrapper
     * @param targetQueryWrapper 目标queryWrapper
     * @param key                注入参数key
     * @return 新注入参数key
     */
    private String putParam(QueryWrapper<?> sourceQueryWrapper, QueryWrapper<?> targetQueryWrapper, String key) {
        Object val = sourceQueryWrapper.getParamNameValuePairs().get(key);
        int index = 1;
        String newKey = "CUSTOMER_KEY" + index;
        while (targetQueryWrapper.getParamNameValuePairs().containsKey(newKey) || sourceQueryWrapper.getParamNameValuePairs().containsKey(newKey)) {
            index++;
            newKey = "CUSTOMER_KEY" + index;
        }
        targetQueryWrapper.getParamNameValuePairs().put(newKey, val);
        return newKey;
    }

    /**
     * 获取带注入参数sql语句
     *
     * @param sqlSegment         源sql语句
     * @param sourceQueryWrapper 源queryWrapper
     * @return 带注入参数sql语句
     */
    protected String getSqlSegmentWithParam(String sqlSegment, ExQueryWrapper<?> sourceQueryWrapper) {
        return getSqlSegmentWithParam(sqlSegment, sourceQueryWrapper, this.queryWrapper);
    }

    /**
     * 获取带注入参数sql语句
     *
     * @param sqlSegment         源sql语句
     * @param sourceQueryWrapper 源queryWrapper
     * @param targetQueryWrapper 目标queryWrapper
     * @return 带注入参数sql语句
     */
    protected String getSqlSegmentWithParam(String sqlSegment, ExQueryWrapper<?> sourceQueryWrapper, QueryWrapper<?> targetQueryWrapper) {
        if (sourceQueryWrapper == targetQueryWrapper) {
            return sqlSegment;
        }

        if (StringUtils.isEmpty(sqlSegment)) {
            return "";
        }
        String customSqlSegment = sqlSegment;
        String paramKey;
        for (Map.Entry<String, Object> entry : sourceQueryWrapper.getParamNameValuePairs().entrySet()) {
            paramKey = putParam(sourceQueryWrapper, targetQueryWrapper, entry.getKey());
            customSqlSegment = customSqlSegment.replace(String.format(Constants.WRAPPER_PARAM_MIDDLE, Constants.WRAPPER, entry.getKey()), String.format(Constants.WRAPPER_PARAM_MIDDLE, Constants.WRAPPER, paramKey));
        }
        return customSqlSegment;
    }

    protected <S extends AbstractSubLambdaQueryWrapper<?>> String getSubSqlSegment(Consumer<S> predicate, Class<S> clazz) throws InstantiationException, IllegalAccessException {
        S subLambda = clazz.newInstance();
        predicate.accept(subLambda);
        return subLambda.getSqlSegment(this.queryWrapper);
    }

    protected <S extends AbstractSubLambdaQueryWrapper<?>> String getSubSqlSegment(Function<S, ?> predicate, Class<S> clazz) throws InstantiationException, IllegalAccessException {
        S subLambda = clazz.newInstance();
        predicate.apply(subLambda);
        return subLambda.getSqlSegment(this.queryWrapper);
    }

    /**
     * 获取属性名
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 属性名
     * @throws ReflectiveOperationException 表达式异常
     */
    protected <T> String getPropertyName(SFunction<T, ?> column) throws ReflectiveOperationException {
        SerializedLambda lambda = ReflectUtils.getLambda(column);
        if (lambda == null) {
            throw new ReflectiveOperationException();
        }
        return ReflectUtils.getMethodPropertyName(lambda.getImplMethodName());
    }

    /**
     * 获取列名
     *
     * @param column 列表达式
     * @param <T>    实体类型
     * @return 列名
     * @throws ReflectiveOperationException 表达式异常
     */
    protected <T> String getColumnName(SFunction<T, ?> column) throws ReflectiveOperationException {
        String property = getPropertyName(column);
        TableInfo<T> tableInfo = getTable(column);
        if (tableInfo != null) {
            Optional<ColumnInfo> columnInfo = tableInfo.getColumns().stream().filter(x -> x.getPropertyName().equalsIgnoreCase(property)).findFirst();
            if (columnInfo.isPresent()) {
                return StringPool.BACKTICK + columnInfo.get().getColumnName() + StringPool.BACKTICK;
            }
        }
        throw new ReflectiveOperationException();
    }

    /**
     * 获取列名
     *
     * @param column      列表达式
     * @param tableRename 表重命名
     * @param <T>         实体类型
     * @return 列名
     * @throws ReflectiveOperationException 表达式异常
     */
    protected <T> String getFullColumnName(SFunction<T, ?> column, String tableRename) throws ReflectiveOperationException {
        String property = getPropertyName(column);
        TableInfo<T> tableInfo = getTable(column);
        if (tableInfo != null) {
            String tableName = StringPool.BACKTICK + tableRename + StringPool.BACKTICK;
            if (StringUtils.isEmpty(tableRename)) {
                tableName = StringPool.BACKTICK + tableInfo.getTableName() + StringPool.BACKTICK;
            }
            tableName = tableName.replace(StringPool.BACKTICK + StringPool.BACKTICK, StringPool.BACKTICK);
            Optional<ColumnInfo> columnInfo = tableInfo.getColumns().stream().filter(x -> x.getPropertyName().equalsIgnoreCase(property)).findFirst();
            if (columnInfo.isPresent()) {
                return tableName + StringPool.DOT + StringPool.BACKTICK + columnInfo.get().getColumnName() + StringPool.BACKTICK;
            }
        }
        throw new ReflectiveOperationException();
    }

    protected <T> String getFullColumnName(Class<T> entityClass, String propertyName, String rename) throws ReflectiveOperationException {
        TableInfo<T> tableInfo = getTable(entityClass);
        if (tableInfo != null) {
            String tableName = StringPool.BACKTICK + rename + StringPool.BACKTICK;
            if (StringUtils.isEmpty(rename)) {
                tableName = StringPool.BACKTICK + tableInfo.getTableName() + StringPool.BACKTICK;
            }
            tableName = tableName.replace(StringPool.BACKTICK + StringPool.BACKTICK, StringPool.BACKTICK);
            Optional<ColumnInfo> columnInfo = tableInfo.getColumns().stream().filter(x -> x.getPropertyName().equalsIgnoreCase(propertyName)).findFirst();
            if (columnInfo.isPresent()) {
                return tableName + StringPool.DOT + StringPool.BACKTICK + columnInfo.get().getColumnName() + StringPool.BACKTICK;
            }
        }
        throw new ReflectiveOperationException();
    }

//    /**
//     * 获取实例类里所有列全名和属性名
//     *
//     * @param entityClass 实体类型
//     * @param tableRename 表重命名
//     * @return [列全名, 属性名]集合
//     * @throws ReflectiveOperationException 反射错误
//     */
//    protected List<String[]> getFullColumnNameAndPropertyNames(Class<?> entityClass, String tableRename) throws ReflectiveOperationException {
//        TableInfo<?> tableInfo = MybatisUtil.getTableInfo(entityClass);
//        if (tableInfo != null) {
//            String tableName = StringPool.BACKTICK + tableRename + StringPool.BACKTICK;
//            if (StringUtils.isEmpty(tableRename)) {
//                tableName = StringPool.BACKTICK + tableInfo.getTableName() + StringPool.BACKTICK;
//            }
//            tableName = tableName.replace("``", StringPool.BACKTICK);
//            List<String[]> columnNames = new ArrayList<>();
//            for (ColumnInfo columnInfo : tableInfo.getColumns()) {
//                columnNames.add(new String[]{
//                        tableName + ".`" + columnInfo.getColumnName() + StringPool.BACKTICK,
//                        StringPool.BACKTICK + entityClass.getName().replace(".", "/") + "/" + columnInfo.getPropertyName() + StringPool.BACKTICK
//                });
//            }
//            return columnNames;
//        }
//        throw new ReflectiveOperationException();
//    }

    /**
     * 获取列重命名
     *
     * @param column 列
     * @param seqKey 索引键
     * @param <T>    实体类型
     * @return 列重命名
     * @throws ReflectiveOperationException 反射错误
     */
    protected <T> String getColumnRename(SFunction<T, ?> column, String seqKey) throws ReflectiveOperationException {
        SerializedLambda lambda = ReflectUtils.getLambda(column);
        if (lambda == null) {
            throw new ReflectiveOperationException();
        }
//        String className = Objects.requireNonNull(StringUtils.regexMatcher("\\(L(.*);\\)", lambda.getInstantiatedMethodType()));
//        return StringPool.BACKTICK + className + "/" + Property.getMethodPropertyName(lambda.getImplMethodName()) + StringPool.BACKTICK;
        return StringPool.BACKTICK + ReflectUtils.getMethodPropertyName(lambda.getImplMethodName()) + StringPool.SLASH + seqKey + StringPool.BACKTICK;
    }

    /**
     * 获取列重命名
     *
     * @param fieldName 字段名
     * @param seqKey    索引键
     * @return 列重命名
     * @throws ReflectiveOperationException 反射错误
     */
    protected String getColumnRename(String fieldName, String seqKey) throws ReflectiveOperationException {
        return StringPool.BACKTICK + fieldName + StringPool.SLASH + seqKey + StringPool.BACKTICK;
    }

    /**
     * 获取表名
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 表名
     * @throws ReflectiveOperationException 表达式异常
     */
    protected <T> String getTableName(SFunction<T, ?> column) throws ReflectiveOperationException {
        TableInfo<T> tableInfo = getTable(column);
        if (tableInfo != null) {
            return StringPool.BACKTICK + tableInfo.getTableName() + StringPool.BACKTICK;
        }
        throw new ReflectiveOperationException();
    }

    /**
     * 获取表名
     *
     * @param entityClass 实体类型
     * @return 表名
     * @throws ReflectiveOperationException 表达式异常
     */
    protected <T> String getTableName(Class<T> entityClass) throws ReflectiveOperationException {
        TableInfo<T> tableInfo = getTable(entityClass);
        if (tableInfo != null) {
            return StringPool.BACKTICK + tableInfo.getTableName() + StringPool.BACKTICK;
        }
        throw new ReflectiveOperationException();
    }

    /**
     * 获取表
     *
     * @param column 列
     * @param <T>    实体类型
     * @return 表
     * @throws ReflectiveOperationException 表达式异常
     */
    @SuppressWarnings("unchecked")
    private <T> TableInfo<T> getTable(SFunction<T, ?> column) throws ReflectiveOperationException {
        SerializedLambda lambda = ReflectUtils.getLambda(column);
        if (lambda == null) {
            throw new ReflectiveOperationException();
        }
        String className = Objects.requireNonNull(StringUtils.regexMatcher("\\(L(.*);\\)", lambda.getInstantiatedMethodType())).replace("/", ".");
        Class<T> entityClass = (Class<T>) Class.forName(className);
        return getTable(entityClass);
    }

    /**
     * 获取表
     *
     * @param entityClass 实体类型
     * @return 表
     */
    private <T> TableInfo<T> getTable(Class<T> entityClass) {
        return MybatisUtil.getTableInfo(entityClass);
    }

}
