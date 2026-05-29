package com.baomidou.mybatisplus.extension.stream;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.mapper.StreamBaseMapper;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper;
import com.baomidou.mybatisplus.extension.metadata.ColumnInfo;
import com.baomidou.mybatisplus.extension.support.StringUtils;
import com.baomidou.mybatisplus.extension.wrapper.DuplicateSetLambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.wrapper.NormalFunctionLambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.dialect.SetterClause;
import com.baomidou.mybatisplus.extension.wrapper.NormalSetLambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.wrapper.OrderLambdaQueryWrapper;

public class MybatisExecutableStream<T> extends MybatisStream<T, T, ExecutableQueryWrapper<T>, MybatisExecutableStream<T>> {

    private static final int MAX_INSERT_COUNT = 500;

    public MybatisExecutableStream(Class<T> entityClass, StreamBaseMapper<T> baseMapper) {
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
        // 4.0.2: 标记本次写入为 DUPLICATE 模式，让 dialect 生成 ON DUPLICATE KEY UPDATE / ON CONFLICT DO UPDATE
        queryWrapper.setWriteMode(com.baomidou.mybatisplus.extension.dialect.WriteMode.DUPLICATE);
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
        return doBatchWrite(entities);
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
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        // 4.0.2: 标记本次写入为 IGNORE 模式，让 dialect 生成 INSERT IGNORE / ON CONFLICT DO NOTHING
        queryWrapper.setWriteMode(com.baomidou.mybatisplus.extension.dialect.WriteMode.IGNORE);
        return doBatchWrite(entities);
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
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        // 4.0.2: 标记本次写入为 REPLACE 模式，让 dialect 生成 REPLACE INTO / ON CONFLICT DO UPDATE 全列
        queryWrapper.setWriteMode(com.baomidou.mybatisplus.extension.dialect.WriteMode.REPLACE);
        return doBatchWrite(entities);
    }

    /**
     * 批量写入共享实现：executeInsert / executeIgnore / executeReplace 仅写入模式不同，
     * 主体逻辑（构建列映射 + 分批 + 派发）抽到这里，避免三处复制粘贴（历史上重复代码已导致末批漏提交 bug）。
     *
     * <p>分批触发改用 {@code itemList.size()} 计数，并在循环结束后无条件提交剩余批次，
     * 杜绝集合中含 null 元素时「以原始集合长度判定末批」导致末批数据被静默丢弃的问题。
     */
    private int doBatchWrite(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        Map<String, String> columnMap = buildInsertColumnMap();
        String[] columns = columnMap.values().toArray(new String[0]);
        List<Object[]> itemList = new ArrayList<>();
        int result = 0;
        for (T entity : entities) {
            if (entity == null) {
                continue;
            }
            List<Object> valueList = new ArrayList<>();
            for (Map.Entry<String, String> column : columnMap.entrySet()) {
                try {
                    valueList.add(ReflectUtils.getPropertyValue(entity, column.getKey()));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException ignored) {
                }
            }
            itemList.add(valueList.toArray(new Object[0]));

            if (itemList.size() >= MAX_INSERT_COUNT) {
                result += dispatchBatchWrite(columns, itemList.toArray(new Object[0][]));
                itemList.clear();
            }
        }
        // 提交剩余不足一批的数据（含集合末尾元素为 null 被跳过的场景）
        if (!itemList.isEmpty()) {
            result += dispatchBatchWrite(columns, itemList.toArray(new Object[0][]));
            itemList.clear();
        }
        return result;
    }

    /**
     * 构建插入列映射：propertyName -> 方言引号包裹的列名。
     * 未指定 effectColumns 时取表全部列（跳过 insertStrategy=NEVER），否则只取指定列。
     */
    private Map<String, String> buildInsertColumnMap() {
        Map<String, String> columnMap = new LinkedHashMap<>();
        if (queryWrapper.getEffectColumns().size() == 0) {
            // 没配置更新列，直接按实体插入
            for (ColumnInfo columnInfo : queryWrapper.getFromTableInfo().getColumns()) {
                if (columnInfo.getTableField() != null && columnInfo.getTableField().insertStrategy() != null && columnInfo.getTableField().insertStrategy().equals(FieldStrategy.NEVER)) {
                    continue;
                }
                columnMap.put(columnInfo.getPropertyName(), com.baomidou.mybatisplus.extension.dialect.DialectRegistry.current().quoteIdentifier(columnInfo.getColumnName()));
            }
        } else {
            // 配置了更新列，只插入更新列
            for (ColumnInfo columnInfo : queryWrapper.getEffectColumns()) {
                columnMap.put(columnInfo.getPropertyName(), com.baomidou.mybatisplus.extension.dialect.DialectRegistry.current().quoteIdentifier(columnInfo.getColumnName()));
            }
        }
        return columnMap;
    }

    /**
     * 执行删除
     *
     * @return 成功条数
     */
    public int executeDelete() {
        // 安全护栏：禁止无条件全表删除。原实现用 assert，生产 JVM 默认不带 -ea 会被跳过，
        // 改为显式 if + throw，确保任何启动参数下都生效。
        if (queryWrapper.getExpression() == null || queryWrapper.getExpression().getNormal().isEmpty()) {
            throw new IllegalStateException("批量删除需要带上删除条件，不可全表删除");
        }

        if (queryWrapper.getFromTableInfo() != null && queryWrapper.getFromTableInfo().isWithLogicDelete()) {
            ColumnInfo logicDeleteColumn = queryWrapper.getFromTableInfo().getLogicDeleteColumn();
            NormalFunctionLambdaQueryWrapper funcLambda = new NormalFunctionLambdaQueryWrapper();
            funcLambda.value(queryWrapper.getFromTableInfo().getLogicDeleteValue());
            // Phase 4: 结构化 setter（裸表名 + 裸列名 + valueExpr）
            queryWrapper.addSetter(new SetterClause(
                queryWrapper.getFromTableInfo().getTableName(),
                logicDeleteColumn.getColumnName(),
                funcLambda.getSqlSegment(queryWrapper)));
            return baseMapper.updateBatch(queryWrapper);
        }
        return baseMapper.delete(queryWrapper);
    }

    /**
     * 执行更新
     *
     * @return 成功条数
     */
    public int executeUpdate() {
        // 安全护栏：禁止无条件全表更新（assert 在生产被跳过，改 if + throw）。
        if (queryWrapper.getExpression() == null || queryWrapper.getExpression().getNormal().isEmpty()) {
            throw new IllegalStateException("批量更新需要带上更新条件，不可全表更新");
        }

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
        // 安全护栏：禁止无条件全表更新（assert 在生产被跳过，改 if + throw）。
        if (queryWrapper.getExpression() == null || queryWrapper.getExpression().getNormal().isEmpty()) {
            throw new IllegalStateException("批量更新需要带上更新条件，不可全表更新");
        }

        // 临时保存setter防止被篡改
        List<SetterClause> tempSetters = new ArrayList<>(queryWrapper.getSetters());
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
                    this.queryWrapper.addSetter(new SetterClause(this.queryWrapper.getFromTableInfo().getTableName(), columnInfo.getColumnName(), funcLambda.getSqlSegment(this.queryWrapper)));
                }
            } else {
                // 配置了更新列，只插入更新列
                for (ColumnInfo columnInfo : this.queryWrapper.getEffectColumns()) {
                    value = ReflectUtils.getPropertyValue(entity, columnInfo.getPropertyName());
                    funcLambda = new NormalFunctionLambdaQueryWrapper();
                    funcLambda.value(value);
                    this.queryWrapper.addSetter(new SetterClause(this.queryWrapper.getFromTableInfo().getTableName(), columnInfo.getColumnName(), funcLambda.getSqlSegment(this.queryWrapper)));
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

    /**
     * 4.0.3：根据当前方言选择走标准 INSERT 路径还是 MERGE INTO 路径。
     * MySQL / PG 走 {@code insertDuplicate}（三个 INSERT mapper 方法 SQL 等价、由 dialect 占位驱动）；
     * DM 在 DUPLICATE/IGNORE/REPLACE 模式走 {@code mergeInto}（@InsertProvider 调 dialect 生成完整 SQL）。
     */
    private int dispatchBatchWrite(String[] columns, Object[][] values) {
        com.baomidou.mybatisplus.extension.dialect.SqlDialect d =
            com.baomidou.mybatisplus.extension.dialect.DialectRegistry.current();
        if (d.useMergeInto(queryWrapper.getWriteMode())) {
            Object[] flatValues = java.util.Arrays.stream(values)
                .flatMap(java.util.Arrays::stream)
                .toArray();
            return baseMapper.mergeInto(columns, values, flatValues, queryWrapper);
        }
        return baseMapper.insertDuplicate(columns, values, queryWrapper);
    }
}
