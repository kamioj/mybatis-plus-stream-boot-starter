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
import com.baomidou.mybatisplus.extension.ColumnInfo;
import com.baomidou.mybatisplus.extension.Converter;
import com.baomidou.mybatisplus.extension.LambdaOrderItem;
import com.baomidou.mybatisplus.extension.TableInfo;
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

public final class MybatisUtil {
    private final static Map<Class<?>, TableInfo<?>> TableCacheMap = new HashMap<>();

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
            List<OrderItem> orderItemList = page.getOrder().stream().map(x -> new OrderItem(){{
                setColumn(x.getKey());
                setAsc(x.getAsc());
            }}).collect(Collectors.toList());
            iPage.setOrders(orderItemList);
        }
        iPage.addOrder(defaultOrderItems);
        return iPage;
    }

    @SuppressWarnings("unchecked")
    public synchronized static <T> TableInfo<T> getTableInfo(Class<T> clazz) {
        if (TableCacheMap.get(clazz) != null) {
            return (TableInfo<T>) TableCacheMap.get(clazz);
        }

        TableInfo<T> table = new TableInfo<>();
        table.setEntityClass(clazz);
        table.setTableName(ReflectUtils.getAnnotation(clazz, TableName.class));
        Field[] fields = ReflectUtils.getDeclaredFields(clazz, Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
        List<ColumnInfo> columns = new ArrayList<>();
        ColumnInfo column;
        for (Field field : fields) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                continue;
            }
            column = new ColumnInfo();
            column.setField(field);
            column.setTableField(field.getAnnotation(TableField.class));
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

        TableCacheMap.put(clazz, table);
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
                    objArray[i] = ReflectUtils.newInstance(ReflectUtils.getGenericClass(toType[i]));
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
                    if (Object.class.equals(toType[i]) || ReflectUtils.isPrimitive(ReflectUtils.getGenericClass(toType[i]))) {
                        // 基础类型直接转换
                        objArray[i] = valueMap.values().stream().filter(Objects::nonNull).findFirst().orElse(null);
                    } else {
                        // 实体类型
                        for (ReflectUtils.Property property : propertiesArr.get(i)) {
                            for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                                if (!property.getName().equals(entry.getKey()) && !property.getName().replace("_", "").equalsIgnoreCase(entry.getKey().replace("_", ""))) {
                                    continue;
                                }
                                if (entry.getValue() != null) {
                                    // 赋值
                                    property.setAccessible(true);
                                    List<Converter<?, ?>> converterList = new ArrayList<>();
                                    converterList.add(new Converter<Long, Date>() {
                                        @Override
                                        public Date convert(Long o) {
                                            return new Date(o);
                                        }
                                    });
                                    converterList.add(new Converter<Date, Long>() {
                                                          @Override
                                                          public Long convert(Date o) {
                                                              return o.getTime();
                                                          }
                                                      }
                                    );
                                    converterList.add(new Converter<Date, String>() {
                                        @Override
                                        public String convert( Date o) {
                                            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(o);
                                        }
                                    });
                                    converterList.add(new Converter<String, Date>() {
                                        @Override
                                        public Date convert(String o) {
                                            return DateUtil.parse(o);
                                        }
                                    });
                                    converterList.add(new Converter<Number, Boolean>() {
                                        @Override
                                        public Boolean convert(Number o) {
                                            return o.longValue() != 0;
                                        }
                                    });
                                    if (ReflectUtils.isPrimitive(entry.getValue().getClass())) {
                                        property.set(objArray[i], ReflectUtils.convertPrimitive(entry.getValue(), ReflectUtils.getGenericClass(property.getRealType()), converterList.toArray(new Converter[0])));
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
