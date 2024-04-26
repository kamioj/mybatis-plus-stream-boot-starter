package com.baomidou.mybatisplus.extension.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.*;
import com.baomidou.mybatisplus.extension.mapper.MysqlBaseMapper;
import com.baomidou.mybatisplus.extension.service.IMysqlServiceBase;
import com.baomidou.mybatisplus.toolkit.MybatisUtil;
import com.baomidou.mybatisplus.toolkit.ReflectUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class MysqlServiceBaseImpl<M extends MysqlBaseMapper<T>, T> extends ServiceImpl<M, T> implements IMysqlServiceBase<T> {

    @Override
    public MybatisQueryableStream1<T, T> stream() {
        return new MybatisQueryableStream1<>(entityClass, baseMapper, entityClass);
    }

    @Override
    public MybatisExecutableStream<T> executableStream() {
        return new MybatisExecutableStream<>(entityClass, baseMapper);
    }

    @Override
    public boolean exist(Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return getValue(predicate, (GroupFunctionLambdaQueryWrapper x) -> x.value(1)) != null;
    }

    @Override
    public int count(Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return (int) stream().filter(predicate).count();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> T get(SFunction<T, U> eqColumn, Object eqValue) {
        if (eqValue == null) {
            return get(x -> x.isNull(eqColumn));
        } else if (eqValue instanceof Collection) {
            return get(x -> x.in(eqColumn, (Collection<U>) eqValue));
        } else if (eqValue.getClass().isArray()) {
            return get(x -> x.in(eqColumn, (U[]) eqValue));
        } else {
            return get(x -> x.eq(eqColumn, (U) eqValue));
        }
    }

    @Override
    public <U> T getOrDefault(SFunction<T, U> eqColumn, Object eqValue, T defaultValue) {
        T resultValue = get(eqColumn, eqValue);
        return resultValue == null ? defaultValue : resultValue;
    }

    @Override
    public T get(Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return get(predicate, null, entityClass);
    }

    @Override
    public T getOrDefault(Consumer<NormalWhereLambdaQueryWrapper> predicate, T defaultValue) {
        T resultValue = get(predicate, null, entityClass);
        return resultValue == null ? defaultValue : resultValue;
    }

    @Override
    public <R> R get(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        List<R> records = list(predicate, null, 1, select, renameClass);
        if (records != null && records.size() > 0) {
            return records.get(0);
        } else {
            return null;
        }
    }

    @Override
    public T getByKeyForUpdate(Object key) {
        ColumnInfo keyColumn = MybatisUtil.getTableInfo(entityClass).getKeyColumn();
        String keyName = StringPool.BACKTICK + keyColumn.getColumnName() + StringPool.BACKTICK;
        return stream()
                .map(x -> x.selectAll(entityClass), entityClass)
                .filter(x -> x.eqFunc(y -> y.customColumn(keyName), y -> y.value(key)))
                .forUpdate()
                .findFirst()
                .orElse(null);
    }

    @Override
    public T getByEntityForUpdate(T entity) {
        try {
            ColumnInfo keyColumn = MybatisUtil.getTableInfo(entityClass).getKeyColumn();
            String keyName = StringPool.BACKTICK + keyColumn.getColumnName() + StringPool.BACKTICK;
            Object keyValue = ReflectUtils.getPropertyValue(entity, keyColumn.getPropertyName());
            stream()
                    .map(x -> x.selectAll(entityClass), entityClass)
                    .filter(x -> x.eqFunc(y -> y.customColumn(keyName), y -> y.value(keyValue)))
                    .limit(1)
                    .forUpdate()
                    .forEach(x -> {
                        ReflectUtils.Property[] properties = ReflectUtils.getDeclaredProperties(x.getClass());
                        for (ReflectUtils.Property property : properties) {
                            property.setAccessible(true);
                            try {
                                property.set(entity, property.get(x));
                            } catch (InvocationTargetException | IllegalAccessException ignored) {
                            }
                        }
                    });
        } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException ignored) {
        }
        return entity;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U, V> V getValue(SFunction<T, U> eqColumn, Object eqValue, SFunction<T, V> selectColumn) {
        if (eqValue == null) {
            return getValue(x -> x.isNull(eqColumn), selectColumn);
        } else if (eqValue instanceof Collection) {
            return getValue(x -> x.in(eqColumn, (Collection<U>) eqValue), selectColumn);
        } else if (eqValue.getClass().isArray()) {
            return getValue(x -> x.in(eqColumn, (U[]) eqValue), selectColumn);
        } else {
            return getValue(x -> x.eq(eqColumn, (U) eqValue), selectColumn);
        }
    }

    @Override
    public <V> V getValue(Consumer<NormalWhereLambdaQueryWrapper> predicate, SFunction<T, V> selectColumn) {
        return getValue(predicate, (GroupFunctionLambdaQueryWrapper x) -> x.column(selectColumn));
    }

    @Override
    public <V> V getValue(Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        List<V> records = listValues(predicate, null, 1, selectFunc);
        if (records != null && records.size() > 0) {
            return records.get(0);
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> List<T> list(SFunction<T, U> eqColumn, Object eqValue) {
        if (eqValue == null) {
            return list(x -> x.isNull(eqColumn));
        } else if (eqValue instanceof Collection) {
            return list(x -> x.in(eqColumn, (Collection<U>) eqValue));
        } else if (eqValue.getClass().isArray()) {
            return list(x -> x.in(eqColumn, (U[]) eqValue));
        } else {
            return list(x -> x.eq(eqColumn, (U) eqValue));
        }
    }

    @Override
    public List<T> list(Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return list(predicate, null, entityClass);
    }

    @Override
    public <R> List<R> list(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return list(predicate, null, null, select, renameClass);
    }

    @Override
    public List<T> list(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit) {
        return list(predicate, order, limit, null, entityClass);
    }

    @Override
    public <R> List<R> list(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return listJoin(null, predicate, order, limit, select, renameClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U, V> List<V> listValues(SFunction<T, U> eqColumn, Object eqValue, SFunction<T, V> selectColumn) {
        if (eqValue == null) {
            return listValues(x -> x.isNull(eqColumn), selectColumn);
        } else if (eqValue instanceof Collection) {
            return listValues(x -> x.in(eqColumn, (Collection<U>) eqValue), selectColumn);
        } else if (eqValue.getClass().isArray()) {
            return listValues(x -> x.in(eqColumn, (U[]) eqValue), selectColumn);
        } else {
            return listValues(x -> x.eq(eqColumn, (U) eqValue), selectColumn);
        }
    }

    @Override
    public <V> List<V> listValues(Consumer<NormalWhereLambdaQueryWrapper> predicate, SFunction<T, V> selectColumn) {
        return listValues(predicate, (GroupFunctionLambdaQueryWrapper x) -> x.column(selectColumn));
    }

    @Override
    public <V> List<V> listValues(Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        return listValues(predicate, null, null, selectFunc);
    }

    @Override
    public <V> List<V> listValues(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, SFunction<T, V> selectColumn) {
        return listValues(predicate, order, limit, (GroupFunctionLambdaQueryWrapper x) -> x.column(selectColumn));
    }

    @Override
    public <V> List<V> listValues(Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        return listJoinValues(null, predicate, order, limit, selectFunc);
    }

    @Override
    public List<T> listJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return listJoin(joinPredicate, predicate, null, entityClass);
    }

    @Override
    public <R> List<R> listJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return listJoin(joinPredicate, predicate, null, null, select, renameClass);
    }

    @Override
    public List<T> listJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit) {
        return listJoin(joinPredicate, predicate, order, limit, null, entityClass);
    }

    @Override
    public <R> List<R> listJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return listGroupJoin(joinPredicate, null, predicate, order, limit, select, renameClass);
    }

    @Override
    public <V> List<V> listJoinValues(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        return listJoinValues(joinPredicate, predicate, null, null, selectFunc);
    }


    @Override
    public <V> List<V> listJoinValues(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        return listGroupJoinValues(joinPredicate, null, predicate, order, limit, selectFunc);
    }

    @Override
    public <R> List<R> listGroup(Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return listGroupJoin(null, group, predicate, select, renameClass);
    }

    @Override
    public <R> List<R> listGroup(Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return listGroupJoin(null, group, predicate, order, limit, select, renameClass);
    }

    @Override
    public <V> List<V> listGroupValues(Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        return listGroupValues(group, predicate, null, null, selectFunc);
    }

    @Override
    public <V> List<V> listGroupValues(Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        return listGroupJoinValues(null, group, predicate, order, limit, selectFunc);
    }

    @Override
    public <R> List<R> listGroupJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return listGroupJoin(joinPredicate, group, predicate, null, null, select, renameClass);
    }

    @Override
    public <R> List<R> listGroupJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        MybatisQueryableStream1<T, T> stream = stream();
        if (predicate != null) {
            stream.filter(predicate);
        }

        if (order != null) {
            stream.sorted(order);
        }

        if (limit != null && limit > 0) {
            stream.limit(limit);
        }

        if (joinPredicate != null) {
            stream.join(null, joinPredicate);
        }

        if (group != null) {
            stream.group(group);
        }

        if (select != null) {
            return stream.map(select, renameClass).collect(Collectors.toList());
        } else {
            return stream.map(x -> x.selectAll(renameClass), renameClass).collect(Collectors.toList());
        }
    }

    @Override
    public <V> List<V> listGroupJoinValues(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        return listGroupJoinValues(joinPredicate, group, predicate, null, null, selectFunc);
    }

    @Override
    public <V> List<V> listGroupJoinValues(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<OrderLambdaQueryWrapper> order, Integer limit, Function<GroupFunctionLambdaQueryWrapper, V> selectFunc) {
        MybatisQueryableStream1<T, T> stream = stream();
        if (predicate != null) {
            stream.filter(predicate);
        }

        if (order != null) {
            stream.sorted(order);
        }

        if (joinPredicate != null) {
            stream.join(null, joinPredicate);
        }

        if (group != null) {
            stream.group(group);
        }

        if (limit != null && limit > 0) {
            stream.limit(limit);
        }

        return stream.distinct().mapToValue(selectFunc).collect(Collectors.toList());
    }

    @Override
    public IPage<T> page(IPage<T> page, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return page(page, predicate, null, entityClass);
    }

    @Override
    public <R> IPage<R> page(IPage<R> page, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return pageJoin(page, null, predicate, select, renameClass);
    }

    @Override
    public <R> IPage<R> pageJoin(IPage<R> page, Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return pageGroupJoin(page, joinPredicate, null, predicate, select, renameClass);
    }

    @Override
    public <R> IPage<R> pageGroup(IPage<R> page, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        return pageGroupJoin(page, null, group, predicate, select, renameClass);
    }

    @Override
    public <R> IPage<R> pageGroupJoin(IPage<R> page, Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<GroupLambdaQueryWrapper> group, Consumer<NormalWhereLambdaQueryWrapper> predicate, Consumer<SelectLambdaQueryWrapper<R>> select, Class<R> renameClass) {
        MybatisQueryableStream1<T, T> stream = stream();
        if (predicate != null) {
            stream.filter(predicate);
        }

        if (joinPredicate != null) {
            stream.join(null, joinPredicate);
        }

        if (group != null) {
            stream.group(group);
        }

        if (select != null) {
            return stream.map(select, renameClass).page(page);
        } else {
            return stream.map(x -> x.selectAll(renameClass), renameClass).page(page);
        }
    }

    @Override
    public int saveBatchWithoutId(Collection<T> entityList) {
        return executableStream().executeInsert(entityList);
    }

    @Override
    public int saveDuplicate(Collection<T> entityList, Consumer<DuplicateSetLambdaQueryWrapper<T>> duplicateSet) {
        return executableStream().duplicate(duplicateSet).executeInsert(entityList);
    }

    @Override
    public int saveIgnore(Collection<T> entityList) {
        return executableStream().executeIgnore(entityList);
    }

    @Override
    public int saveReplace(Collection<T> entityList) {
        return executableStream().executeReplace(entityList);
    }

    @Override
    public int remove(Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return executableStream().filter(predicate).executeDelete();
    }

    @Override
    public int update(Consumer<NormalSetLambdaQueryWrapper> setter, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        return updateJoin(null, setter, predicate);
    }

    @Override
    public int updateJoin(Consumer<JoinLambdaQueryWrapper<T>> joinPredicate, Consumer<NormalSetLambdaQueryWrapper> setter, Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        assert predicate != null : "更新条件不可为空";
        MybatisExecutableStream<T> stream = executableStream();
        stream.filter(predicate);

        if (joinPredicate != null) {
            stream.join(null, joinPredicate);
        }

        return stream.set(setter).executeUpdate();
    }

    @SuppressWarnings("unchecked")
    protected <R> List<R> callProcedureForList(String procedureName, Class<R> renameClass, ProcedureParam... procedureParams) {
        List<ProcedureParamDef> definitions = new ArrayList<>();
        Map<String, Object> param = new HashMap<>();
        for (ProcedureParam procedureParam : procedureParams) {
            definitions.add(procedureParam);
            param.put(procedureParam.getKey(), procedureParam.getValue());
        }
        List<Map<String, Object>> result = getBaseMapper().callProcedureForList(procedureName, definitions, param);
        for (ProcedureParam procedureParam : procedureParams) {
            procedureParam.setValue(param.get(procedureParam.getKey()));
        }
        if (Object.class.equals(renameClass) || ReflectUtils.isAssignableFrom(renameClass, Map.class)) {
            return (List<R>) result;
        } else if (ReflectUtils.isPrimitive(renameClass)) {
            // 单值类型
            return result.stream().filter(Objects::nonNull).map(x -> (R) x.values().stream().findFirst().orElse(null)).filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            // 实体类型
            return MybatisUtil.mapStream(result, renameClass).map(x -> (R) x[0]).collect(Collectors.toList());
        }
    }
}
