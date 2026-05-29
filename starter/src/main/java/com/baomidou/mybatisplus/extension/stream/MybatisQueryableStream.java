package com.baomidou.mybatisplus.extension.stream;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.mapper.StreamBaseMapper;
import com.baomidou.mybatisplus.toolkit.MybatisUtil;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;
import org.apache.ibatis.type.TypeReference;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.baomidou.mybatisplus.extension.core.ExQueryWrapper;
import com.baomidou.mybatisplus.extension.dialect.DialectRegistry;
import com.baomidou.mybatisplus.extension.dialect.LockMode;
import com.baomidou.mybatisplus.extension.dialect.SqlDialect;
import com.baomidou.mybatisplus.extension.support.LambdaOrderItem;
import com.baomidou.mybatisplus.extension.value.SingleValue;
import com.baomidou.mybatisplus.extension.wrapper.GroupLambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.wrapper.OrderLambdaQueryWrapper;

public abstract class MybatisQueryableStream<T, R, Children extends MybatisQueryableStream<T, R, Children>> extends MybatisStream<T, R, ExQueryWrapper<T>, Children> {
    protected final Type[] renameClass;

    abstract Function<Object[], R> getReturnMapper();

    public MybatisQueryableStream(Class<T> entityClass, StreamBaseMapper<T> baseMapper, Type... renameClass) {
        this(null, entityClass, baseMapper, renameClass);
    }

    public MybatisQueryableStream(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, StreamBaseMapper<T> baseMapper, Type... renameClass) {
        super(queryWrapper == null ? new ExQueryWrapper<T>() {{
            setFromTable(MybatisUtil.getTableInfo(entityClass), null);
        }} : queryWrapper, entityClass, baseMapper);
        this.renameClass = renameClass;
    }

    @SafeVarargs
    public MybatisQueryableStream(Class<T> entityClass, StreamBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        this(null, entityClass, baseMapper, renameType);
    }

    @SafeVarargs
    public MybatisQueryableStream(ExQueryWrapper<T> queryWrapper, Class<T> entityClass, StreamBaseMapper<T> baseMapper, TypeReference<?>... renameType) {
        super(queryWrapper == null ? new ExQueryWrapper<T>() {{
            setFromTable(MybatisUtil.getTableInfo(entityClass), null);
        }} : queryWrapper, entityClass, baseMapper);
        // 用 MyBatis TypeReference 的公开零参 getRawType()。注意 getSuperclassTypeParameter(Class) 是带参的包私方法，
        // 经无参反射 invokeMethod 调用必抛 NoSuchMethodException 而静默返回 null（会把 null 存进 renameClass 致后续 NPE）。
        this.renameClass = Arrays.stream(renameType).map(TypeReference::getRawType).toArray(Type[]::new);
    }

    /**
     * 基于当前 renameClass 复制一份并替换最后一个元素，返回新数组。
     * map* 系列必须用本方法而非原地改 {@code this.renameClass[len-1]}——
     * 后者会让旧 stream 实例与新实例共享同一数组，旧实例被复用时类型映射被污染。
     */
    protected Type[] copyRenameWithLast(Type last) {
        Type[] copy = Arrays.copyOf(this.renameClass, this.renameClass.length);
        copy[copy.length - 1] = last;
        return copy;
    }

//    /**
//     * 获取子查询sql并传入注入参数
//     *
//     * @param queryWrapper 待传入注入参数的queryWrapper
//     * @return 子查询sql
//     */
//    @Override
//    public String getSqlSegment(ExQueryWrapper<?> queryWrapper) {
//        String sqlSelect = getQueryWrapper().getSqlSelect();
//        if (StringUtils.isEmpty(sqlSelect)) {
//            sqlSelect = "*";
//        }
//
//        String customSqlSegment = getSqlSegmentWithParam(getQueryWrapper().getCustomSqlSegment(), getQueryWrapper(), queryWrapper);
//        return "SELECT " + sqlSelect + " FROM " + getQueryWrapper().getSqlFrom() + " " + customSqlSegment;
//    }


    /**
     * 分组
     *
     * @param group 分组表达式
     * @return 实例本身
     */
    public Children group(Consumer<GroupLambdaQueryWrapper> group) {
        GroupLambdaQueryWrapper groupLambda = new GroupLambdaQueryWrapper(queryWrapper);
        group.accept(groupLambda);
        return typedThis;
    }

    /**
     * 去重
     *
     * @return 实例本身
     */
    @Override
    public Children distinct() {
        this.queryWrapper.setDistinct(true);
        return typedThis;
    }

    /**
     * 排序
     *
     * @param order 排序表达式
     * @return 实例本身
     */
    @Override
    public Children sorted(Consumer<OrderLambdaQueryWrapper> order) {
        OrderLambdaQueryWrapper orderLambda = new OrderLambdaQueryWrapper(queryWrapper);
        if (order != null) {
            order.accept(orderLambda);
        }
        return typedThis;
    }

    /**
     * 限制条数
     *
     * @param maxSize 限制条数
     * @return 实例本身
     */
    @Override
    public Children limit(long maxSize) {
        queryWrapper.setLimit(maxSize);
        queryWrapper.last(buildTailClause(queryWrapper.getSkip(), queryWrapper.getLimit(), queryWrapper.isForUpdate()));
        return typedThis;
    }

    /**
     * 跳过条数
     *
     * @param n 跳过条数
     * @return 实例本身
     */
    @Override
    public Children skip(long n) {
        queryWrapper.setSkip(n);
        queryWrapper.last(buildTailClause(queryWrapper.getSkip(), queryWrapper.getLimit(), queryWrapper.isForUpdate()));
        return typedThis;
    }

    @Override
    public boolean exist() {
        ExQueryWrapper<T> existQueryWrapper = this.queryWrapper.toExistWrapper();
        boolean exist = baseMapper.list(existQueryWrapper).size() > 0;
        this.queryWrapper.reset();
        return exist;
    }

    @Override
    public long count() {
        ExQueryWrapper<T> countQueryWrapper = this.queryWrapper.toCountQueryWrapper("C");
        Long count = (Long) baseMapper.list(countQueryWrapper).get(0).get("C");
        this.queryWrapper.reset();
        return count;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Stream<R> toStream() {
        String sqlSelect = queryWrapper.getSqlSelect();
        if (renameClass.length == 1 && renameClass[0].equals(Object.class)) {
            sqlSelect = sqlSelect.split(",(?![^()]*+\\))")[0].split("(?i) as ")[0] + " AS mps_v";
        }
        if (queryWrapper.isDistinct()) {
            sqlSelect = "DISTINCT " + sqlSelect.trim().replaceAll("^(?i)distinct\\s+", "");
        }
        queryWrapper.select(sqlSelect);
        List<Map<String, Object>> result = baseMapper.list(queryWrapper);
        this.queryWrapper.reset();
        if (renameClass.length == 1 && renameClass[0].equals(Object.class)) {
            // 单值类型
            return result.stream().filter(Objects::nonNull).map(x -> (R) valueOfAlias(x, "mps_v", "mpsV", "V"))
                .filter(Objects::nonNull);
        } else {
            // 实体类型
            return MybatisUtil.mapStream(result, renameClass).map(getReturnMapper());
        }
    }

    private static Object valueOfAlias(Map<String, Object> row, String... aliases) {
        for (String alias : aliases) {
            if (row.containsKey(alias)) {
                return row.get(alias);
            }
            String upper = alias.toUpperCase();
            if (row.containsKey(upper)) {
                return row.get(upper);
            }
        }
        return row.values().stream().findFirst().orElse(null);
    }

    public Children forUpdate() {
        queryWrapper.setForUpdate(true);
        queryWrapper.last(buildTailClause(queryWrapper.getSkip(), queryWrapper.getLimit(), queryWrapper.isForUpdate()));
        return typedThis;
    }

    /**
     * 分页查询
     *
     * @param page 分页参数
     * @param <P>  分页实体类
     * @return 分页
     */
    public <P extends IPage<R>> P page(P page) {
        return page(page, Function.identity());
    }

    /**
     * 分页查询
     *
     * @param page   分页参数
     * @param mapper 转换方法
     * @param <V>    值类型
     * @param <P>    分页实体类
     * @return 分页
     */
    @SuppressWarnings("unchecked")
    public <V, P extends IPage<V>> P page(P page, Function<R, V> mapper) {
//        Page<Map<String, Object>> p = new Page<>(page.getCurrent(), page.getSize());
//        String sqlSelect = queryWrapper.getSqlSelect();
//        if (renameClass == null) {
//            sqlSelect = sqlSelect.split(",")[0].split("(?i) as ")[0] + " AS mps_v";
//        }
//        if (queryWrapper.isDistinct()) {
//            sqlSelect = "DISTINCT " + sqlSelect.trim().replaceAll("^(?i)distinct\\s+", "");
//        }
//        queryWrapper.select(sqlSelect);
//        if (!CollectionUtils.isEmpty(page.orders())) {
//            for (OrderItem orderItem : page.orders()) {
//                queryWrapper.orderBy(true, orderItem.isAsc(), orderItem.getColumn());
//            }
//        }
//        IPage<Map<String, Object>> page1 = baseMapper.page(new Page<>(page.getCurrent(), page.getSize()), queryWrapper);
        ExQueryWrapper<T> countQueryWrapper = queryWrapper.toCountQueryWrapper("C");
        Long total = (Long) baseMapper.list(countQueryWrapper).get(0).get("C");
        if (total > 0) {
            List<ReflectUtils.Property[]> propertiesArr = Arrays.stream(this.renameClass).map(ReflectUtils::getDeclaredProperties).collect(Collectors.toList());
            List<OrderItem> orders = page.orders().stream().map(
                    x -> {
                        OrderItem orderItem = new OrderItem();
                        String column = x.getColumn().replaceAll(StringPool.BACKTICK, "");
                        if (x instanceof LambdaOrderItem) {
                            for (int i = 0; i < this.renameClass.length; i++) {
                                if (ReflectUtils.getGenericClass(this.renameClass[i]).equals(((LambdaOrderItem<?>) x).getClazz())) {
                                    column = StringPool.BACKTICK + column + StringPool.SLASH + (i + 1) + StringPool.BACKTICK;
                                    break;
                                }
                            }
                        } else {
                            boolean b = false;
                            for (int i = 0; i < this.renameClass.length; i++) {
                                if (!Object.class.equals(this.renameClass[i]) && !ReflectUtils.isPrimitive(ReflectUtils.getGenericClass(this.renameClass[i]))) {
                                    for (ReflectUtils.Property property : propertiesArr.get(i)) {
                                        if (property.getName().equals(column)) {
                                            column = StringPool.BACKTICK + column + StringPool.SLASH + (i + 1) + StringPool.BACKTICK;
                                            b = true;
                                            break;
                                        }
                                    }
                                    if (b) {
                                        break;
                                    }
                                } else {
                                    column = StringPool.BACKTICK + column + StringPool.SLASH + (i + 1) + StringPool.BACKTICK;
                                }
                            }
                        }
                        orderItem.setColumn(column);
                        orderItem.setAsc(x.isAsc());
                        return orderItem;
                    }
            ).collect(Collectors.toList());
            ExQueryWrapper<T> pageQueryWrapper = queryWrapper.toPageQueryWrapper(page.getCurrent(), page.getSize(), orders);
            List<Map<String, Object>> pageResult = baseMapper.list(pageQueryWrapper);
            List<V> result;
            if (renameClass.length == 1 && renameClass[0].equals(SingleValue.class)) {
                // 单值类型
                result = pageResult.stream().filter(Objects::nonNull).map(x -> (V) x.entrySet().iterator().next().getValue()).filter(Objects::nonNull).collect(Collectors.toList());
            } else {
                // 实体类型
                result = MybatisUtil.mapStream(pageResult, renameClass).map(getReturnMapper()).map(mapper).collect(Collectors.toList());
            }
            page.setRecords(result);
        }
        page.setTotal(total);
        queryWrapper.reset();
        return page;
    }

    /** 拼装查询尾部子句（LIMIT + 可选 FOR UPDATE [模式]），交给当前方言渲染。*/
    private String buildTailClause(long skip, long limit, boolean forUpdate) {
        SqlDialect d = DialectRegistry.current();
        String tail = d.paginate("", skip, limit).trim();
        if (forUpdate) {
            LockMode mode = queryWrapper.getLockMode();
            if (mode == null) {
                mode = LockMode.FOR_UPDATE;
            }
            tail += d.forUpdate(mode, queryWrapper.getLockWaitSeconds());
        }
        return tail;
    }

    /* ============== 4.0：行锁细粒度 API ============== */

    /** {@code FOR UPDATE NOWAIT} —— 行被占用时立即抛错，不等待 */
    public Children forUpdateNoWait() {
        return forUpdateWith(LockMode.NOWAIT, 0);
    }

    /** {@code FOR UPDATE SKIP LOCKED} —— 行被占用时跳过该行 */
    public Children forUpdateSkipLocked() {
        return forUpdateWith(LockMode.SKIP_LOCKED, 0);
    }

    /** {@code FOR UPDATE WAIT n} —— 等待 n 秒后超时抛错（MySQL 不支持，DM 支持）*/
    public Children forUpdateWait(int seconds) {
        return forUpdateWith(LockMode.WAIT, seconds);
    }

    @SuppressWarnings("unchecked")
    private Children forUpdateWith(LockMode mode, int waitSeconds) {
        queryWrapper.setLockMode(mode);
        queryWrapper.setLockWaitSeconds(waitSeconds);
        queryWrapper.last(buildTailClause(queryWrapper.getSkip(), queryWrapper.getLimit(), true));
        return (Children) this;
    }

    /* ============================================================
     * 4.0：SQL-aware Terminal Operations
     * 让用户直接在 starter 上调用 toMap/toSet/groupingBy 等，避免
     * "service.list().stream().collect(JDK Collector)" 的全量加载模式。
     * 命名与 JDK Collectors 保持接近，降低学习成本。
     * ============================================================ */

    /**
     * 取列值集合（去重）。
     * <p>SQL: {@code SELECT col FROM ... WHERE ...}（应用层 LinkedHashSet 去重）
     */
    public <K> java.util.Set<K> toSet(SFunction<T, K> col) {
        Class<?> colType = MybatisUtil.valueTypeOf(col);
        java.util.List<Map<String, Object>> rows = executeSelectOneColumn(col);
        java.util.LinkedHashSet<K> result = new java.util.LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            // 4.1.1: 通过 alias 取值（Map iterator 顺序在 PG 等 JDBC 上不可靠）
            // 4.x: 按列声明类型归一驱动返回值（达梦 TINYINT→Byte 转回 Boolean）
            @SuppressWarnings("unchecked")
            K v = (K) MybatisUtil.coerceValue(valueOfAlias(row, "mps_k", "mpsK", "K"), colType);
            if (v != null) result.add(v);
        }
        return result;
    }

    /**
     * 取键-值 Map。Key 冲突时<b>后者覆盖前者</b>（与 JDK {@code Collectors.toMap} 默认抛异常不同；
     * 业务里通常是按 key 索引最新一条，覆盖比抛错更实用）。
     * <p>SQL: {@code SELECT keyCol, valCol FROM ... WHERE ...}
     */
    public <K, V> Map<K, V> toMap(SFunction<T, K> keyCol, SFunction<T, V> valCol) {
        return toMap(keyCol, valCol, (a, b) -> b);
    }

    /**
     * 取键-值 Map，Key 冲突时用 {@code merger} 合并。
     * <p>SQL 同上；合并完全在应用层进行。
     */
    public <K, V> Map<K, V> toMap(SFunction<T, K> keyCol, SFunction<T, V> valCol,
                                  java.util.function.BinaryOperator<V> merger) {
        Class<?> keyType = MybatisUtil.valueTypeOf(keyCol);
        Class<?> valType = MybatisUtil.valueTypeOf(valCol);
        java.util.List<Map<String, Object>> rows = executeSelectTwoColumns(keyCol, valCol);
        java.util.LinkedHashMap<K, V> result = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            // 4.x: 按列声明类型归一驱动返回值（达梦 TINYINT→Byte 转回 Boolean）
            @SuppressWarnings("unchecked")
            K k = (K) MybatisUtil.coerceValue(valueOfAlias(row, "mps_k", "mpsK", "K"), keyType);
            @SuppressWarnings("unchecked")
            V v = (V) MybatisUtil.coerceValue(valueOfAlias(row, "mps_v", "mpsV", "V"), valType);
            if (k != null) result.merge(k, v, merger);
        }
        return result;
    }

    /**
     * 按 keyCol 分组（应用层），<b>保留所有列</b>。SQL 不下推 GROUP BY。
     *
     * <p><b>性能/内存警告</b>：本方法会把所有匹配行全量加载进内存再分组，<b>不限制行数</b>。
     * 大数据量下可能 OOM。仅适用于小结果集；如只需 key + 聚合，请用 {@link #toMapCount} /
     * {@link #toMapSum} 等真正下推到 SQL 的方法（性能更好且不占内存）。
     */
    public <K> Map<K, java.util.List<T>> groupingBy(SFunction<T, K> keyCol) {
        // 不改 SELECT，让 wrapper 保留完整投影；让基类的 toStream 走原有 entity mapping
        // 简化实现：直接调用 toStream() 拿到 R 流，但 R 不一定是 T...
        // 这里通过一次 baseMapper.list 拿原始 row，再用 keyCol 取属性名分组。
        String keyColumnName = MybatisUtil.propertyOf(keyCol);
        // 通过 mapStream 的 entity-style 还原回 T 实例
        @SuppressWarnings("unchecked")
        Class<T> tClass = (Class<T>) entityClass;
        java.util.List<Map<String, Object>> rows = baseMapper.list(queryWrapper);
        queryWrapper.reset();
        java.util.LinkedHashMap<K, java.util.List<T>> result = new java.util.LinkedHashMap<>();
        MybatisUtil.mapStream(rows, tClass).forEach(arr -> {
            @SuppressWarnings("unchecked")
            T entity = (T) arr[0];
            K k = MybatisUtil.readProperty(entity, keyColumnName);
            result.computeIfAbsent(k, x -> new java.util.ArrayList<>()).add(entity);
        });
        return result;
    }

    /**
     * 按 keyCol 分组计数。<b>SQL 真下推</b>。
     * <p>SQL: {@code SELECT keyCol, COUNT(*) FROM ... WHERE ... GROUP BY keyCol}
     */
    public <K> Map<K, Long> toMapCount(SFunction<T, K> keyCol) {
        return executeGroupBy(keyCol, "COUNT(*)", Number.class, o -> ((Number) o).longValue());
    }

    /**
     * 按 keyCol 分组求和。<b>SQL 真下推</b>。
     * <p>SQL: {@code SELECT keyCol, SUM(sumCol) FROM ... WHERE ... GROUP BY keyCol}
     */
    @SuppressWarnings("unchecked")
    public <K, V extends Number> Map<K, V> toMapSum(SFunction<T, K> keyCol, SFunction<T, V> sumCol) {
        return executeGroupBy(keyCol, "SUM(" + MybatisUtil.columnOf(sumCol) + ")",
                              Number.class, n -> (V) n);
    }

    /**
     * 按 keyCol 分组求均值。<b>SQL 真下推</b>。返回 {@code Double}（SQL AVG 默认浮点）。
     * <p>SQL: {@code SELECT keyCol, AVG(avgCol) FROM ... WHERE ... GROUP BY keyCol}
     */
    public <K> Map<K, Double> toMapAvg(SFunction<T, K> keyCol, SFunction<T, ? extends Number> avgCol) {
        return executeGroupBy(keyCol, "AVG(" + MybatisUtil.columnOf(avgCol) + ")",
                              Number.class, o -> ((Number) o).doubleValue());
    }

    /**
     * 按 keyCol 分组取最大值。<b>SQL 真下推</b>。
     * <p>SQL: {@code SELECT keyCol, MAX(maxCol) FROM ... WHERE ... GROUP BY keyCol}
     */
    @SuppressWarnings("unchecked")
    public <K, V extends Comparable<V>> Map<K, V> toMapMax(SFunction<T, K> keyCol, SFunction<T, V> maxCol) {
        return executeGroupBy(keyCol, "MAX(" + MybatisUtil.columnOf(maxCol) + ")",
                              Object.class, o -> (V) o);
    }

    /**
     * 按 keyCol 分组取最小值。<b>SQL 真下推</b>。
     * <p>SQL: {@code SELECT keyCol, MIN(minCol) FROM ... WHERE ... GROUP BY keyCol}
     */
    @SuppressWarnings("unchecked")
    public <K, V extends Comparable<V>> Map<K, V> toMapMin(SFunction<T, K> keyCol, SFunction<T, V> minCol) {
        return executeGroupBy(keyCol, "MIN(" + MybatisUtil.columnOf(minCol) + ")",
                              Object.class, o -> (V) o);
    }

    /* ====== 内部 helper：复用 wrapper + baseMapper 完成 SQL 下推 ====== */

    /** 单列投影：SELECT col FROM ... */
    private <K> java.util.List<Map<String, Object>> executeSelectOneColumn(SFunction<T, K> col) {
        String colName = MybatisUtil.columnOf(col);
        queryWrapper.select(colName + " AS mps_k");
        java.util.List<Map<String, Object>> rows = baseMapper.list(queryWrapper);
        queryWrapper.reset();
        return rows;
    }

    /** 双列投影：SELECT keyCol, valCol FROM ... */
    private <K, V> java.util.List<Map<String, Object>> executeSelectTwoColumns(
            SFunction<T, K> keyCol, SFunction<T, V> valCol) {
        String k = MybatisUtil.columnOf(keyCol);
        String v = MybatisUtil.columnOf(valCol);
        queryWrapper.select(k + " AS mps_k, " + v + " AS mps_v");
        java.util.List<Map<String, Object>> rows = baseMapper.list(queryWrapper);
        queryWrapper.reset();
        return rows;
    }

    /** GROUP BY 聚合：SELECT keyCol, aggExpr FROM ... GROUP BY keyCol */
    private <K, V> Map<K, V> executeGroupBy(SFunction<T, K> keyCol, String aggExpr,
                                            Class<?> rawType, java.util.function.Function<Object, V> caster) {
        Class<?> keyType = MybatisUtil.valueTypeOf(keyCol);
        String k = MybatisUtil.columnOf(keyCol);
        queryWrapper.select(k + " AS mps_k, " + aggExpr + " AS mps_v");
        queryWrapper.groupBy(k);
        java.util.List<Map<String, Object>> rows = baseMapper.list(queryWrapper);
        queryWrapper.reset();
        // M10: reset() 只重置 paramNameSeq，不清 GROUP BY。清掉本次添加的 groupBy，
        // 避免同一 stream 实例被复用做第二次终端聚合时 GROUP BY 叠加（select 为覆盖语义不累加）。
        queryWrapper.getExpression().getGroupBy().clear();
        java.util.LinkedHashMap<K, V> result = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            // 4.1.1: 显式按 alias 取值，避免 Map iterator 顺序不可靠导致 key/value 错位
            // 4.x: 按 key 列声明类型归一驱动返回值（达梦 TINYINT→Byte 转回 Boolean）
            @SuppressWarnings("unchecked")
            K key = (K) MybatisUtil.coerceValue(valueOfAlias(row, "mps_k", "mpsK", "K"), keyType);
            Object raw = valueOfAlias(row, "mps_v", "mpsV", "V");
            if (raw == null) continue;
            if (!rawType.isInstance(raw)) {
                throw new IllegalStateException("Unexpected aggregate type: " + raw.getClass());
            }
            result.put(key, caster.apply(raw));
        }
        return result;
    }
}
