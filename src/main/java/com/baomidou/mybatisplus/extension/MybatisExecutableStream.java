package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.mapper.MysqlBaseMapper;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MybatisExecutableStream<T> extends MybatisStream<T, T, ExecutableQueryWrapper<T>, MybatisExecutableStream<T>> {

    private static final int MAX_INSERT_COUNT = 500;

    public MybatisExecutableStream(Class<T> entityClass, MysqlBaseMapper<T> baseMapper) {
        super(new ExecutableQueryWrapper<>(entityClass), entityClass, baseMapper);
    }

    @Override
    public MybatisExecutableStream<T> distinct() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MybatisExecutableStream<T> sorted(Consumer<OrderLambdaQueryWrapper> order) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MybatisExecutableStream<T> limit(long maxSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MybatisExecutableStream<T> skip(long n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exist() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Stream<T> toStream() {
        throw new UnsupportedOperationException();
    }

    public MybatisExecutableStream<T> set(Consumer<NormalSetLambdaQueryWrapper> setter) {
        NormalSetLambdaQueryWrapper setLambda = new NormalSetLambdaQueryWrapper(queryWrapper);
        setter.accept(setLambda);
        return typedThis;
    }

    public MybatisExecutableStream<T> duplicate(Consumer<DuplicateSetLambdaQueryWrapper<T>> duplicateSetter) {
        DuplicateSetLambdaQueryWrapper<T> setLambda = new DuplicateSetLambdaQueryWrapper<>(queryWrapper, entityClass);
        duplicateSetter.accept(setLambda);
        return typedThis;
    }

    @SafeVarargs
    public final MybatisExecutableStream<T> effects(SFunction<T, ?>... columns) {
        NormalFunctionLambdaQueryWrapper funcLambda;
        String columnName;
        List<String> columnNames = new ArrayList<>();
        for (SFunction<T, ?> column : columns) {
            funcLambda = new NormalFunctionLambdaQueryWrapper();
            funcLambda.column(column);
            columnName = funcLambda.getSqlSegment(funcLambda.getQueryWrapper());
            if (columnName.contains(".")) {
                columnName = columnName.split("\\.")[1];
            }
            columnName = columnName.replaceAll(StringPool.BACKTICK, "");
            columnNames.add(columnName);
        }
        queryWrapper.getEffectColumns().addAll(queryWrapper.getFromTableInfo().getColumns().stream().filter(x -> columnNames.contains(x.getColumnName())).collect(Collectors.toSet()));
        return typedThis;
    }

    /**
     * 执行插入
     *
     * @param entity 实体
     * @return 成功条数
     */
    public int executeInsert(T entity) {
        return executeInsert(new ArrayList<T>() {{
            add(entity);
        }});
    }

    /**
     * 执行插入
     *
     * @param entities 实体数组
     * @return 成功条数
     */
    public int executeInsert(Collection<T> entities) {
        if (entities == null || entities.size() == 0) {
            return 0;
        }

        // 获取表信息
        Map<String, String> columnMap = new LinkedHashMap<>();
        if (queryWrapper.getEffectColumns().size() == 0) {
            // 没配置更新列，直接按实体插入
            for (ColumnInfo columnInfo : queryWrapper.getFromTableInfo().getColumns()) {
                if (columnInfo.getTableField() != null && columnInfo.getTableField().insertStrategy() != null && columnInfo.getTableField().insertStrategy().equals(FieldStrategy.NEVER)) {
                    continue;
                }
                columnMap.put(columnInfo.getPropertyName(), StringPool.BACKTICK + columnInfo.getColumnName() + StringPool.BACKTICK);
            }
        } else {
            // 配置了更新列，只插入更新列
            for (ColumnInfo columnInfo : queryWrapper.getEffectColumns()) {
                columnMap.put(columnInfo.getPropertyName(), StringPool.BACKTICK + columnInfo.getColumnName() + StringPool.BACKTICK);
            }
        }

        int result = 0;
//      g  int addOrUnchangedCount = 0;
//      g  int updateCount = 0;
        int i = 0;
        String[] columns = columnMap.values().toArray(new String[0]);
        Object[][] values;
        List<Object> valueList;
        List<Object[]> itemList = new ArrayList<>();
        for (T entity : entities) {
            if (entity == null) {
                continue;
            }
            valueList = new ArrayList<>();
            for (Map.Entry<String, String> column : columnMap.entrySet()) {
                try {
                    valueList.add(ReflectUtils.getPropertyValue(entity, column.getKey()));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException ignored) {
                }
            }
            itemList.add(valueList.toArray(new Object[0]));

            i++;
            if (i % MAX_INSERT_COUNT == 0 || i == entities.size()) {
                // 提交更新
                values = itemList.toArray(new Object[0][]);
                itemList.clear();

                result += baseMapper.insertDuplicate(columns, values, queryWrapper);
//              g   int count = values.length;
//              g  addOrUnchangedCount += 2 * count - result;
//              g  updateCount += result - count;
            }
        }
        return result;
    }

    public int executeIgnore(T entity) {
        return executeIgnore(new ArrayList<T>() {{
            add(entity);
        }});
    }

    /**
     * 执行忽略插入
     *
     * @param entities 实体数组
     * @return 成功条数
     */
    public int executeIgnore(Collection<T> entities) {
        if (entities == null || entities.size() == 0) {
            return 0;
        }

        // 获取表信息
        Map<String, String> columnMap = new LinkedHashMap<>();
        if (queryWrapper.getEffectColumns().size() == 0) {
            // 没配置更新列，直接按实体插入
            for (ColumnInfo columnInfo : queryWrapper.getFromTableInfo().getColumns()) {
                if (columnInfo.getTableField() != null && columnInfo.getTableField().insertStrategy() != null && columnInfo.getTableField().insertStrategy().equals(FieldStrategy.NEVER)) {
                    continue;
                }
                columnMap.put(columnInfo.getPropertyName(), StringPool.BACKTICK + columnInfo.getColumnName() + StringPool.BACKTICK);
            }
        } else {
            // 配置了更新列，只插入更新列
            for (ColumnInfo columnInfo : queryWrapper.getEffectColumns()) {
                columnMap.put(columnInfo.getPropertyName(), StringPool.BACKTICK + columnInfo.getColumnName() + StringPool.BACKTICK);
            }
        }

        int result = 0;
//      g  int addOrUnchangedCount = 0;
//      g  int updateCount = 0;
        int i = 0;
        String[] columns = columnMap.values().toArray(new String[0]);
        Object[][] values;
        List<Object> valueList;
        List<Object[]> itemList = new ArrayList<>();
        for (T entity : entities) {
            if (entity == null) {
                continue;
            }
            valueList = new ArrayList<>();
            for (Map.Entry<String, String> column : columnMap.entrySet()) {
                try {
                    valueList.add(ReflectUtils.getPropertyValue(entity, column.getKey()));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException ignored) {
                }
            }
            itemList.add(valueList.toArray(new Object[0]));

            i++;
            if (i % MAX_INSERT_COUNT == 0 || i == entities.size()) {
                // 提交更新
                values = itemList.toArray(new Object[0][]);
                itemList.clear();

                result += baseMapper.insertIgnore(columns, values, queryWrapper);
//              g   int count = values.length;
//              g  addOrUnchangedCount += 2 * count - result;
//              g  updateCount += result - count;
            }
        }
        return result;
    }

    public int executeReplace(T entity) {
        return executeReplace(new ArrayList<T>() {{
            add(entity);
        }});
    }

    /**
     * 执行替换插入
     *
     * @param entities 实体数组
     * @return 成功条数
     */
    public int executeReplace(Collection<T> entities) {
        if (entities == null || entities.size() == 0) {
            return 0;
        }

        // 获取表信息
        Map<String, String> columnMap = new LinkedHashMap<>();
        if (queryWrapper.getEffectColumns().size() == 0) {
            // 没配置更新列，直接按实体插入
            for (ColumnInfo columnInfo : queryWrapper.getFromTableInfo().getColumns()) {
                if (columnInfo.getTableField() != null && columnInfo.getTableField().insertStrategy() != null && columnInfo.getTableField().insertStrategy().equals(FieldStrategy.NEVER)) {
                    continue;
                }
                columnMap.put(columnInfo.getPropertyName(), StringPool.BACKTICK + columnInfo.getColumnName() + StringPool.BACKTICK);
            }
        } else {
            // 配置了更新列，只插入更新列
            for (ColumnInfo columnInfo : queryWrapper.getEffectColumns()) {
                columnMap.put(columnInfo.getPropertyName(), StringPool.BACKTICK + columnInfo.getColumnName() + StringPool.BACKTICK);
            }
        }

        int result = 0;
//      g  int addOrUnchangedCount = 0;
//      g  int updateCount = 0;
        int i = 0;
        String[] columns = columnMap.values().toArray(new String[0]);
        Object[][] values;
        List<Object> valueList;
        List<Object[]> itemList = new ArrayList<>();
        for (T entity : entities) {
            if (entity == null) {
                continue;
            }
            valueList = new ArrayList<>();
            for (Map.Entry<String, String> column : columnMap.entrySet()) {
                try {
                    valueList.add(ReflectUtils.getPropertyValue(entity, column.getKey()));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException ignored) {
                }
            }
            itemList.add(valueList.toArray(new Object[0]));

            i++;
            if (i % MAX_INSERT_COUNT == 0 || i == entities.size()) {
                // 提交更新
                values = itemList.toArray(new Object[0][]);
                itemList.clear();

                result += baseMapper.insertReplace(columns, values, queryWrapper);
//              g   int count = values.length;
//              g  addOrUnchangedCount += 2 * count - result;
//              g  updateCount += result - count;
            }
        }
        return result;
    }

    /**
     * 执行删除
     *
     * @return 成功条数
     */
    public int executeDelete() {
        assert queryWrapper.getExpression() != null && !queryWrapper.getExpression().getNormal().isEmpty() : "批量删除需要带上删除条件，不可全表更新";

        return baseMapper.delete(queryWrapper);
    }

    /**
     * 执行更新
     *
     * @return 成功条数
     */
    public int executeUpdate() {
        assert queryWrapper.getExpression() != null && !queryWrapper.getExpression().getNormal().isEmpty() : "批量更新需要带上更新条件，不可全表更新";

        if (queryWrapper.getSetters().size() == 0) {
            return 0;
        }
        return baseMapper.updateBatch(queryWrapper);
    }

    /**
     * 执行更新
     *
     * @param entity 实体
     * @return 成功条数
     */
    public int executeUpdate(T entity) {
        assert queryWrapper.getExpression() != null && !queryWrapper.getExpression().getNormal().isEmpty() : "批量更新需要带上更新条件，不可全表更新";

        // 临时保存setter防止被篡改
        List<String> tempSetters = new ArrayList<>(queryWrapper.getSetters());
        try {
            // 获取表信息
            Object value;
            NormalFunctionLambdaQueryWrapper funcLambda;
            if (this.queryWrapper.getEffectColumns().size() == 0) {
                // 没配置更新列，直接按实体插入
                for (ColumnInfo columnInfo : this.queryWrapper.getFromTableInfo().getColumns()) {
                    if (columnInfo.getColumnName().equals(this.queryWrapper.getFromTableInfo().getKeyColumn().getColumnName())) {
                        // 过滤主键
                        continue;
                    }
                    value = ReflectUtils.getPropertyValue(entity, columnInfo.getPropertyName());
                    FieldStrategy updateStrategy = columnInfo.getTableField() != null && columnInfo.getTableField().updateStrategy() != null ? columnInfo.getTableField().updateStrategy() : null;
                    if (updateStrategy != null && updateStrategy.equals(FieldStrategy.NEVER)) {
                        // 过滤主键
                        continue;
                    }
                    if (updateStrategy != null && updateStrategy.equals(FieldStrategy.NOT_NULL) && value == null) {
                        continue;
                    }
                    if (updateStrategy != null && updateStrategy.equals(FieldStrategy.NOT_EMPTY) && StringUtils.isEmpty((String) value)) {
                        continue;
                    }
                    funcLambda = new NormalFunctionLambdaQueryWrapper();
                    funcLambda.value(value);
                    this.queryWrapper.addSetter(this.queryWrapper.getFromTableRename() + StringPool.DOT + StringPool.BACKTICK + columnInfo.getColumnName() + StringPool.BACKTICK + " = " + funcLambda.getSqlSegment(this.queryWrapper));
                }
            } else {
                // 配置了更新列，只插入更新列
                for (ColumnInfo columnInfo : this.queryWrapper.getEffectColumns()) {
                    value = ReflectUtils.getPropertyValue(entity, columnInfo.getPropertyName());
                    funcLambda = new NormalFunctionLambdaQueryWrapper();
                    funcLambda.value(value);
                    this.queryWrapper.addSetter(this.queryWrapper.getFromTableRename() + StringPool.DOT + StringPool.BACKTICK + columnInfo.getColumnName() + StringPool.BACKTICK + " = " + funcLambda.getSqlSegment(this.queryWrapper));
                }
            }
            return baseMapper.updateBatch(this.queryWrapper);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            // 还原setter
            this.queryWrapper.getSetters().clear();
            this.queryWrapper.getSetters().addAll(tempSetters);
        }
    }

}
