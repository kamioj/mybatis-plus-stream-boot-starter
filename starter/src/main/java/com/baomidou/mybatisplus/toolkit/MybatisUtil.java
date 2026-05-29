package com.baomidou.mybatisplus.toolkit;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.bo.PageVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.baomidou.mybatisplus.extension.core.Converter;
import com.baomidou.mybatisplus.extension.metadata.ColumnInfo;
import com.baomidou.mybatisplus.extension.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.support.LambdaOrderItem;

public final class MybatisUtil {
    /**
     * 实体类 → TableInfo 缓存。4.0 起从 {@code synchronized HashMap} 升级为 {@link ClassValue}，
     * 无锁、线程局部高速缓存；命中率 100%（每个 Entity 类的 TableInfo 只构造一次）。
     */
    private static final ClassValue<TableInfo<?>> TABLE_INFO_CACHE = new ClassValue<>() {
        @Override
        protected TableInfo<?> computeValue(Class<?> clazz) {
            return buildTableInfo(clazz);
        }
    };

    private static GlobalConfig.DbConfig defaultDbConfig = null;

    @SuppressWarnings("unchecked")
    public static GlobalConfig.DbConfig getDefaultDbConfig() {
        if (defaultDbConfig == null) {
            try {
                Field field = ReflectUtils.getDeclaredField(GlobalConfigUtils.class, "GLOBAL_CONFIG");
                field.setAccessible(true);
                Map<String, GlobalConfig> GLOBAL_CONFIG = (Map<String, GlobalConfig>) field.get(null);
                defaultDbConfig = GLOBAL_CONFIG.entrySet().iterator().next().getValue().getDbConfig();
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                defaultDbConfig = GlobalConfigUtils.defaults().getDbConfig();
            }
        }
        return defaultDbConfig;
    }


    /**
     * 生成page方法
     *
     * @param page              前端传入page
     * @param defaultOrderItems 默认排序项
     * @param <T>               实体类型
     * @return page实体
     */
    @SafeVarargs
    public static <T> IPage<T> buildPage(PageVo page, LambdaOrderItem<T>... defaultOrderItems) {
        Page<T> iPage = new Page<>(page.getPageNum(), page.getPageSize());
        if (!CollectionUtils.isEmpty(page.getOrder())) {
            List<OrderItem> orderItemList = page.getOrder().stream().map(x -> {
                String key = x.getKey();
                // 安全校验：排序列名以 ${} 原样进入 ORDER BY，无法参数化；仅允许字母/数字/下划线，
                // 拒绝空格、引号、括号、逗号、分号等注入字符（前端传入的 SortVo.key 不可信）。
                if (key == null || !key.matches("^[A-Za-z0-9_]+$")) {
                    throw new IllegalArgumentException("非法排序列名: " + key);
                }
                OrderItem orderItem = new OrderItem();
                orderItem.setColumn(key);
                // SortVo.asc 是装箱 Boolean，为 null 时默认升序，避免拆箱 NPE
                orderItem.setAsc(x.getAsc() == null || x.getAsc());
                return orderItem;
            }).collect(Collectors.toList());
            iPage.setOrders(orderItemList);
        }
        iPage.addOrder(defaultOrderItems);
        return iPage;
    }

    @SuppressWarnings("unchecked")
    public static <T> TableInfo<T> getTableInfo(Class<T> clazz) {
        return (TableInfo<T>) TABLE_INFO_CACHE.get(clazz);
    }

    /**
     * 解析 SFunction 对应的 Java 属性名（如 {@code User::getId} → {@code "id"}）。
     * <p>4.0 起，本方法是 SQL-aware terminal operations（toMap / toSet / groupingBy / toMapXxx）
     * 实现 SQL 下推的核心 helper。
     */
    public static String propertyOf(com.baomidou.mybatisplus.core.toolkit.support.SFunction<?, ?> col) {
        java.lang.invoke.SerializedLambda lambda = ReflectUtils.getLambda(col);
        if (lambda == null) {
            throw new IllegalStateException("Not a serializable lambda. Use SFunction (e.g. User::getId).");
        }
        return ReflectUtils.getMethodPropertyName(lambda.getImplMethodName());
    }

    /**
     * 解析 SFunction 对应的数据库列名（含方言引号，如 MySQL 的 {@code `id`}）。
     * <p>查 {@link TableInfo} 的 {@code @TableField} 映射；若实体未声明则按属性名兜底。
     */
    public static String columnOf(com.baomidou.mybatisplus.core.toolkit.support.SFunction<?, ?> col) {
        java.lang.invoke.SerializedLambda lambda = ReflectUtils.getLambda(col);
        if (lambda == null) {
            throw new IllegalStateException("Not a serializable lambda. Use SFunction (e.g. User::getId).");
        }
        String property = ReflectUtils.getMethodPropertyName(lambda.getImplMethodName());
        String implClassName = lambda.getImplClass().replace('/', '.');
        com.baomidou.mybatisplus.extension.dialect.SqlDialect dialect =
            com.baomidou.mybatisplus.extension.dialect.DialectRegistry.current();
        try {
            Class<?> entityClass = Class.forName(implClassName);
            TableInfo<?> tableInfo = getTableInfo(entityClass);
            for (ColumnInfo ci : tableInfo.getColumns()) {
                if (ci.getPropertyName().equalsIgnoreCase(property)) {
                    return dialect.quoteIdentifier(ci.getColumnName());
                }
            }
        } catch (ClassNotFoundException ignored) {
            // fall through to property-name fallback
        }
        return dialect.quoteIdentifier(property);
    }

    /**
     * 通过属性名读取实体的属性值。供 groupingBy 内存分组使用。
     */
    @SuppressWarnings("unchecked")
    public static <V> V readProperty(Object entity, String propertyName) {
        try {
            return (V) ReflectUtils.getPropertyValue(entity, propertyName);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read property '" + propertyName
                + "' from " + entity.getClass().getName(), e);
        }
    }

    /**
     * 结果集映射用的默认类型转换器集合：Long↔Date、Date→String、String→Date、Number→Boolean。
     * <p>其中 {@code Number→Boolean}（非零即 true）用于兼容不同 JDBC 驱动对布尔列的返回差异：
     * MySQL 的 {@code TINYINT(1)} 直接返回 {@code Boolean}，达梦的 {@code TINYINT} 返回 {@code Byte}。
     * 转换器无状态，缓存为单例数组（{@link Converter} 构造时反射解析泛型，缓存可免去逐行重复开销）。
     */
    private static final Converter<?, ?>[] DEFAULT_CONVERTERS = new Converter<?, ?>[]{
        new Converter<Long, Date>() {
            @Override
            public Date convert(Long o) {
                return new Date(o);
            }
        },
        new Converter<Date, Long>() {
            @Override
            public Long convert(Date o) {
                return o.getTime();
            }
        },
        new Converter<Date, String>() {
            @Override
            public String convert(Date o) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(o);
            }
        },
        new Converter<String, Date>() {
            @Override
            public Date convert(String o) {
                return DateUtil.parse(o);
            }
        },
        new Converter<Number, Boolean>() {
            @Override
            public Boolean convert(Number o) {
                return o.longValue() != 0;
            }
        }
    };

    /**
     * 解析 SFunction 对应列的声明 Java 类型（如 {@code User::getActive} → {@code Boolean.class}）。
     * <p>用于值列（{@code mapToColumn} / {@code toMap} / {@code toSet} 等）结果映射时，把 JDBC 驱动返回的
     * 原始类型转回实体声明类型——典型场景：达梦 {@code TINYINT} 返回 {@code Byte}，需转回 {@code Boolean}。
     * 解析失败时返回 {@code Object.class}（退化为原样取值，与历史行为一致）。
     */
    public static Class<?> valueTypeOf(com.baomidou.mybatisplus.core.toolkit.support.SFunction<?, ?> col) {
        try {
            java.lang.invoke.SerializedLambda lambda = ReflectUtils.getLambda(col);
            if (lambda == null) {
                return Object.class;
            }
            String property = ReflectUtils.getMethodPropertyName(lambda.getImplMethodName());
            Class<?> entityClass = Class.forName(lambda.getImplClass().replace('/', '.'));
            for (ColumnInfo ci : getTableInfo(entityClass).getColumns()) {
                if (ci.getPropertyName().equalsIgnoreCase(property)) {
                    return ci.getColumnType();
                }
            }
        } catch (Throwable ignored) {
            // 解析失败：退化为 Object.class，保持「原样取值」历史行为
        }
        return Object.class;
    }

    /**
     * 把 JDBC 驱动返回的原始值强制为列声明的 Java 类型。
     * <p>主要解决方言间布尔列差异：达梦 {@code TINYINT} 返回 {@code Byte}，需按 {@code Number→Boolean}
     * 语义（非零即 true）转回 {@code Boolean}。已是目标类型时原样返回，对 MySQL 路径零额外开销。
     *
     * @param raw          原始值（可能为 null）
     * @param declaredType 列声明类型；{@code Object.class} / {@code null} 表示未知类型，原样返回
     */
    public static Object coerceValue(Object raw, Class<?> declaredType) {
        if (raw == null || declaredType == null || Object.class.equals(declaredType) || declaredType.isInstance(raw)) {
            return raw;
        }
        if (ReflectUtils.isPrimitive(raw.getClass()) && ReflectUtils.isPrimitive(declaredType)) {
            return ReflectUtils.convertPrimitive(raw, declaredType, DEFAULT_CONVERTERS);
        }
        return raw;
    }

    private static <T> TableInfo<T> buildTableInfo(Class<T> clazz) {
        TableInfo<T> table = new TableInfo<>();
        table.setEntityClass(clazz);
        table.setTableName(ReflectUtils.getAnnotation(clazz, TableName.class));
        Field[] fields = ReflectUtils.getDeclaredFields(clazz, Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
        List<ColumnInfo> columns = new ArrayList<>();
        for (Field field : fields) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                continue;
            }
            ColumnInfo column = new ColumnInfo();
            column.setField(field);
            column.setTableField(tableField);
            column.setKey(field.getAnnotation(TableId.class) != null);
            TableLogic tableLogicAnnotation = field.getAnnotation(TableLogic.class);
            column.setLogicDelete(tableLogicAnnotation != null);
            columns.add(column);
            if (column.isKey()) {
                table.setKeyColumn(column);
            }
            if (tableLogicAnnotation != null) {
                table.setWithLogicDelete(true);
                table.setLogicDeleteColumn(column);
                table.setLogicNotDeleteValue(StringUtils.isEmpty(tableLogicAnnotation.value()) ? getDefaultDbConfig().getLogicNotDeleteValue() : tableLogicAnnotation.value());
                table.setLogicDeleteValue(StringUtils.isEmpty(tableLogicAnnotation.delval()) ? getDefaultDbConfig().getLogicDeleteValue() : tableLogicAnnotation.delval());
            }
        }
        table.setColumns(columns);
        return table;
    }

    @SuppressWarnings("unused")
    public static Stream<Object[]> mapStream(Collection<Map<String, Object>> entities, Type... toType) {
//        Map<Type, Property[]> propertyMap = new HashMap<>();
//        for (Type type : toType) {
//            propertyMap.put(type, ReflectUtils.getDeclaredProperties(type));
//        }
        List<ReflectUtils.Property[]> propertiesArr = Arrays.stream(toType).map(ReflectUtils::getDeclaredProperties).collect(Collectors.toList());
        return entities.stream().map(x -> {
            try {
                Object[] objArray = new Object[toType.length];
                for (int i = 0; i < toType.length; i++) {
                    // 仅实体类型需要预先 new 实例供后续 setter 赋值；Object/基础类型槽位会被取值分支整体覆盖，
                    // 无需（也不能）反射实例化——如 BigDecimal 等无公开无参构造的类会触发 InaccessibleObjectException。
                    Class<?> genericClass = ReflectUtils.getGenericClass(toType[i]);
                    if (!Object.class.equals(toType[i]) && !ReflectUtils.isPrimitive(genericClass)) {
                        objArray[i] = ReflectUtils.newInstance(genericClass);
                    }
                }
                List<Map<String, Object>> values = Arrays.stream(toType).map(y -> new HashMap<String, Object>()).collect(Collectors.toList());
                if (x == null) {
                    return new Object[toType.length];
                } else {
                    for (Map.Entry<String, Object> entry : x.entrySet()) {
                        String[] split = entry.getKey().split("/");
                        if (split.length <= 1 || split[1].length() == 0) {
                            values.get(0).put(entry.getKey(), entry.getValue());
                        } else {
                            values.get(Integer.parseInt(split[1]) - 1).put(split[0], entry.getValue());
                        }
                    }
                }
                int lastIndexOfPoint;
                Map<String, Object> valueMap;
                for (int i = 0; i < toType.length; i++) {
                    valueMap = values.get(i);
                    Class<?> targetType = ReflectUtils.getGenericClass(toType[i]);
                    if (Object.class.equals(toType[i])) {
                        // 值列/未知目标类型：原样取首个非空值，交调用方处理
                        objArray[i] = valueMap.values().stream().filter(Objects::nonNull).findFirst().orElse(null);
                    } else if (ReflectUtils.isPrimitive(targetType)) {
                        // 基础类型：把驱动返回的原始值转回声明类型（如达梦 TINYINT→Byte 需转回 Boolean）
                        Object raw = valueMap.values().stream().filter(Objects::nonNull).findFirst().orElse(null);
                        objArray[i] = coerceValue(raw, targetType);
                    } else {
                        // 实体类型
                        ReflectUtils.Property[] props = propertiesArr.get(i);
                        // L-01: getDeclaredProperties 对某些 ParameterizedType 可能返回 null，防止增强 for 触发 NPE
                        if (props == null) continue;
                        for (ReflectUtils.Property property : props) {
                            for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                                if (!property.getName().equals(entry.getKey()) && !property.getName().replace("_", "").equalsIgnoreCase(entry.getKey().replace("_", ""))) {
                                    continue;
                                }
                                if (entry.getValue() != null) {
                                    // 赋值
                                    property.setAccessible(true);
                                    if (ReflectUtils.isPrimitive(entry.getValue().getClass())) {
                                        property.set(objArray[i], ReflectUtils.convertPrimitive(entry.getValue(), ReflectUtils.getGenericClass(property.getRealType()), DEFAULT_CONVERTERS));
                                    } else {
                                        property.set(objArray[i], entry.getValue());
                                    }
                                }
                                break;
                            }
                        }
                    }

                }
//                for (Map.Entry<Type, Property[]> propertyEntry : propertyMap.entrySet()) {
//                    for (Property property : propertyEntry.getValue()) {
//                        for (Map.Entry<String, Object> entry : x.entrySet()) {
//                            lastIndexOfPoint = entry.getKey().lastIndexOf("/");
//                            entryTypeKey = lastIndexOfPoint < 0 ? null : entry.getKey().substring(0, lastIndexOfPoint).replace("/", ".");
//                            key = entry.getKey().substring(lastIndexOfPoint + 1);
//                            if (entryTypeKey != null && !entryTypeKey.equals(ReflectUtils.getGenericClass(propertyEntry.getKey()).getName())) {
//                                continue;
//                            }
//                            String propertyName = property.getName();
//                            value = null;
//                            if (propertyName.equals(key) || key.replace("_", "").equalsIgnoreCase(propertyName.replace("_", ""))) {
//                                // 普通字段
//                                value = entry.getValue();
//                            } else if (advConverters.entrySet().stream().anyMatch(y -> y.getKey().getKey2().equals(propertyName) && Arrays.stream(y.getKey().getKey1().split(":")).anyMatch(z -> z.equals(entry.getKey())))) {
//                                // 多字段拼接
//                                Map.Entry<BiMapKey<String, String>, Object> advConverter = advConverters.entrySet().stream()
//                                        .filter(y -> y.getKey().getKey2().equals(propertyName) && Arrays.stream(y.getKey().getKey1().split(":")).anyMatch(z -> z.equals(entry.getKey())))
//                                        .findFirst()
//                                        .orElse(null);
//                                assert advConverter != null;
//                                String[] keys = advConverter.getKey().getKey1().split(":");
//                                Object[] values = new Object[keys.length];
//                                for (int i = 0; i < keys.length; i++) {
//                                    values[i] = x.get(keys[i]);
//                                }
//                                Method[] declaredMethods = ReflectUtils.getDeclaredMethods(advConverter.getValue().getClass(), Modifier.PUBLIC);
//                                Method applyMethod = Arrays.stream(declaredMethods).filter(y -> y.getName().equals("apply")).findFirst().orElse(null);
//                                if (applyMethod != null) {
//                                    applyMethod.setAccessible(true);
//                                    value = applyMethod.invoke(advConverter.getValue(), values);
//                                }
//                            } else {
//                                continue;
//                            }
//                            if (value != null) {
//                                // 赋值
//                                property.setAccessible(true);
//                                List<Converter<?, ?>> converterList = new ArrayList<>();
//                                converterList.add(new Converter<Long, Date>() {
//                                    @Override
//                                    public Date convert(Long o) {
//                                        return new Date(o);
//                                    }
//                                });
//                                converterList.add(new Converter<Date, Long>() {
//                                                      @Override
//                                                      public Long convert(Date o) {
//                                                          return o.getTime();
//                                                      }
//                                                  }
//                                );
//                                converterList.add(new Converter<Date, String>() {
//                                    @Override
//                                    public String convert(Date o) {
//                                        return TmDateUtil.dateToString(o, "yyyy-MM-dd HH:mm:ss");
//                                    }
//                                });
//                                converterList.add(new Converter<String, Date>() {
//                                    @Override
//                                    public Date convert(String o) {
//                                        return TypeUtils.castToDate(o);
//                                    }
//                                });
//                                converterList.add(new Converter<Number, Boolean>() {
//                                    @Override
//                                    public Boolean convert(Number o) {
//                                        return o.longValue() != 0;
//                                    }
//                                });
//                                if (ReflectUtils.isPrimitive(value.getClass())) {
//                                    property.set(objMap.get(propertyEntry.getKey()), ReflectUtils.convertPrimitive(value, ReflectUtils.getGenericClass(property.getRealType()), converterList.toArray(new Converter[0])));
//                                } else {
//                                    property.set(objMap.get(propertyEntry.getKey()), value);
//                                }
//                            }
//                            x.remove(entry.getKey());
//                            break;
//                        }
//                    }
//                }
                return objArray;
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

//    public static <T> Stream<T> mapStream(Collection<Map<String, Object>> entities, Class<T> tClazz) {
//        Field[] fields = ReflectUtils.getDeclaredFields(tClazz, Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
//        Map<Field, String> fieldMap = new HashMap<>();
//        return entities.stream().map(x -> {
//            try {
//                T obj = tClazz.newInstance();
//                for (Field field : fields) {
//                    Object v = null;
//                    if (fieldMap.containsKey(field)) {
//                        v = x.get(fieldMap.get(field));
//                    } else {
//                        for (Map.Entry<String, Object> entry : x.entrySet()) {
//                            if (entry.getKey().replace("_", "").equalsIgnoreCase(field.getName().replace("_", ""))) {
//                                fieldMap.put(field, entry.getKey());
//                                v = entry.getValue();
//                                break;
//                            }
//                        }
//                    }
//                    if (v == null) {
//                        continue;
//                    }
//                    field.setAccessible(true);
//                    field.set(obj, ReflectUtils.convertPrimitive(v, field.getType(),
//                            new Converter<Long, Date>() {
//                                @Override
//                                public Date convert(Long o) {
//                                    return new Date(o);
//                                }
//                            }, new Converter<Date, Long>() {
//                                @Override
//                                public Long convert(Date o) {
//                                    return o.getTime();
//                                }
//                            }, new Converter<String, Date>() {
//                                @Override
//                                public Date convert(String o) {
//                                    return TmDateUtil.stringToDate(o, "yyyy-MM-dd HH:mm:ss");
//                                }
//                            }, new Converter<Date, String>() {
//                                @Override
//                                public String convert(Date o) {
//                                    return TmDateUtil.dateToString(o, "yyyy-MM-dd HH:mm:ss");
//                                }
//                            }));
//                }
//                return obj;
//            } catch (InstantiationException | IllegalAccessException ignored) {
//                return null;
//            }
//        });
//    }

}
