package com.baomidou.mybatisplus.toolkit;


import com.baomidou.mybatisplus.extension.Converter;
import com.sun.istack.internal.NotNull;

import java.beans.Introspector;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author 小明同学
 * @date 2024年04月25日 15:24
 */
public class ReflectUtils {

    private static final Pattern GET_PATTERN = Pattern.compile("^get[A-Za-z].*");
    private static final Pattern SET_PATTERN = Pattern.compile("^set[A-Za-z].*");
    private static final Pattern IS_PATTERN = Pattern.compile("^is[A-Za-z].*");


    public static Class<?> getGenericClass(Type type) throws IllegalArgumentException {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getGenericClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            // 类型异常
            return Object[].class;
        } else {
            throw new IllegalArgumentException();
        }
    }

    /*
     * 反射获取泛型类型
     * @author 小明同学
     * @date 2024/4/25 15:45
     * @param type
     * @param clazz
     * @param index
     * @return java.lang.Class<T>
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getGenericClass(Type type, Class<?> clazz, int index) throws IllegalArgumentException {
        Type genericType = getGenericType(type, clazz, index);
        if (genericType instanceof Class) {
            return (Class<T>) genericType;
        } else if (genericType instanceof ParameterizedType) {
            return (Class<T>) ((ParameterizedType) genericType).getRawType();
        } else {
            // 类型异常
            throw new IllegalArgumentException();
        }
    }

    public static Type getGenericType(Type type, Class<?> clazz, int index) throws IllegalArgumentException {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            if (rawType.equals(clazz)) {
                // 类上匹配
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (index >= 0 && index < actualTypeArguments.length) {
                    return actualTypeArguments[index];
                } else {
                    throw new IllegalArgumentException("Index out of bounds: " + index);
                }
            } else if (rawType instanceof Class) {
                // 找父类
                return getGenericType(((Class<?>) rawType).getGenericSuperclass(), clazz, index);
            } else {
                throw new IllegalArgumentException("Unsupported raw type: " + rawType);
            }
        } else if (type instanceof Class) {
            // 类型为Class，查找父类的泛型
            return getGenericType(((Class<?>) type).getGenericSuperclass(), clazz, index);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }


    public static SerializedLambda getLambda(Serializable func) {
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            return (SerializedLambda) method.invoke(func);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }


    /**
     * 通过Getter方法名获取属性名
     *
     * @param methodName Getter/Setter方法名
     * @return 属性名
     */
    public static String getMethodPropertyName(String methodName) {
        String propertyName = "";
        if (GET_PATTERN.matcher(methodName).matches() || SET_PATTERN.matcher(methodName).matches()) {
            propertyName = methodName.substring(3);
        } else if (IS_PATTERN.matcher(methodName).matches()) {
            propertyName = methodName.substring(2);
        }
        return Introspector.decapitalize(propertyName);
    }


    /**
     * 获取属性值
     *
     * @param obj          实体
     * @param propertyName 属性名称
     * @param <R>          返回类型
     * @return 属性值
     * @throws IllegalAccessException    反射获取字段异常
     * @throws InvocationTargetException 反射获取字段异常
     * @throws NoSuchFieldException      没有该字段
     */
    @SuppressWarnings("unchecked")
    public static <R> R getPropertyValue(Object obj, String propertyName) throws
            IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Method[] methods = getDeclaredMethods(obj.getClass(), Modifier.PUBLIC);
        for (Method method : methods) {
            if (method.getParameterCount() == 0 && !method.getReturnType().getName().equals("void") && (method.getName().startsWith("get") || method.getName().startsWith("is")) && getMethodPropertyName(method.getName()).equals(propertyName)) {
                method.setAccessible(true);
                return (R) method.invoke(obj);
            }
        }
        Field[] fields = getDeclaredFields(obj.getClass(), Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
        for (Field field : fields) {
            if (field.getName().equals(propertyName)) {
                field.setAccessible(true);
                return (R) field.get(obj);
            }
        }
        throw new NoSuchFieldException();
    }

    /**
     * 获取类上的注解（包括父类）
     *
     * @param clazz           类型
     * @param annotationClass 注解类型
     * @param <T>             注解类型
     * @return 注解实体
     */
    public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationClass) {
        if (clazz.isAnnotationPresent(annotationClass)) {
            return clazz.getAnnotation(annotationClass);
        } else if (clazz.getSuperclass() != null) {
            return getAnnotation(clazz.getSuperclass(), annotationClass);
        } else {
            return null;
        }
    }


    /**
     * 获取类型字段（包括父类）
     *
     * @param clazz 类型
     * @param name  字段名
     * @return 字段
     * @throws NoSuchFieldException 没有该字段
     */
    public static Field getDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException {
        if (clazz.getSuperclass() == null) {
            throw new NoSuchFieldException(name);
        }

        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException ignored) {
            return getDeclaredField(clazz.getSuperclass(), name);
        }
    }


    /**
     * 获取类型所有字段（包括父类）
     *
     * @param clazz 类型
     * @param mod   字段类型，java.lang.reflect.Modifier里查看，多个用或连接，如：Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE
     * @return 字段集合
     */
    public static Field[] getDeclaredFields(Class<?> clazz, int mod) {
        if (clazz.getSuperclass() == null) {
            return new Field[0];
        }
        Map<String, Field> fieldMap = Arrays.stream(getDeclaredFields(clazz.getSuperclass(), mod)).collect(Collectors.toMap(Field::getName, x -> x, (u, v) -> v, LinkedHashMap::new));
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() | mod) == mod) {
                fieldMap.put(field.getName(), field);
            }
        }
        return fieldMap.values().toArray(new Field[0]);
    }

    /**
     * 获取类型所有字段（包括父类）
     *
     * @param clazz           类型
     * @param annotationClazz 注解类型
     * @return 字段集合
     */
    public static Field[] getDeclaredFields(Class<?> clazz, Class<? extends Annotation> annotationClazz) {
        if (clazz.getSuperclass() == null) {
            return new Field[0];
        }
        Map<String, Field> fieldMap = Arrays.stream(getDeclaredFields(clazz.getSuperclass(), annotationClazz)).collect(Collectors.toMap(Field::getName, x -> x, (u, v) -> v, LinkedHashMap::new));
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getAnnotation(annotationClazz) != null) {
                fieldMap.put(field.getName(), field);
            }
        }
        return fieldMap.values().toArray(new Field[0]);
    }


    /**
     * 获取类型所有方法（包括父类）
     *
     * @param clazz 类型
     * @param mod   字段类型，java.lang.reflect.Modifier里查看，多个用或连接，如：Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE
     * @return 方法集合
     */
    public static Method[] getDeclaredMethods(Class<?> clazz, int mod) {
        if (clazz.getSuperclass() == null) {
            return new Method[0];
        }
        Map<String, Method> methodMap = Arrays.stream(getDeclaredMethods(clazz.getSuperclass(), mod)).collect(Collectors.toMap(Method::toString, x -> x, (u, v) -> v, LinkedHashMap::new));
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if ((method.getModifiers() | mod) == mod) {
                methodMap.put(method.toString(), method);
            }
        }
        return methodMap.values().toArray(new Method[0]);
    }


    /**
     * 判断是否是类型的超类或超接口（包括基础类型）
     * 如(isAssignableFrom(int.class, Integer.class)返回true,isAssignableFrom(Number.class, Integer.class)返回true,
     *
     * @param clazz     待判断类型
     * @param fromClazz 判断类型
     * @return 返回是否类型的超类或超接口
     */
    public static boolean isAssignableFrom(Class<?> clazz, Class<?> fromClazz) {
        if (clazz.isPrimitive()) {
            // 基础类型
            try {
                return clazz.isAssignableFrom(((Class<?>) fromClazz.getField("TYPE").get(null)));
            } catch (Throwable ignored) {
                return false;
            }
        }
        return clazz.isAssignableFrom(fromClazz);
    }


    /**
     * 判断是否是基础类型
     * 基础类型包括String,Character,int,Integer,long,Long等类型
     *
     * @param clazz 待判断类型
     * @return 返回是否是基础类型
     */
    public static boolean isPrimitive(Class<?> clazz) {
        return clazz.isPrimitive() || isAssignableFrom(String.class, clazz) || isAssignableFrom(Character.class, clazz) || isAssignableFrom(Number.class, clazz) || isAssignableFrom(Boolean.class, clazz) || isAssignableFrom(Date.class, clazz);
    }


    // 强制访问方法
    public static Object invokeMethod(Object obj, String methodName) {
        Method method = null;
        try {
            method = obj.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true); // 设置为可访问私有方法
            return method.invoke(obj);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return null;
        }
    }


    /**
     * 获取真实完整类型
     *
     * @param type           类型
     * @param genericTypeMap 泛型映射
     * @return 完整类型
     */
    public static Type getRealType(Type type, Map<String, Type> genericTypeMap) {
        if (type instanceof TypeVariable<?>) {
            // 泛型
            String typeName = ((TypeVariable<?>) type).getName();
            return genericTypeMap.get(typeName);
        } else if (type instanceof ParameterizedType) {
            // 带泛型的类
            Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
            for (int i = 0; i < actualTypeArguments.length; i++) {
                actualTypeArguments[i] = getRealType(actualTypeArguments[i], genericTypeMap);
            }
            return new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return actualTypeArguments;
                }

                @Override
                public Type getRawType() {
                    return ((ParameterizedType) type).getRawType();
                }

                @Override
                public Type getOwnerType() {
                    return ((ParameterizedType) type).getOwnerType();
                }
            };
        } else {
            return type;
        }
    }


    /**
     * 获取所有属性名称（包括父类）
     *
     * @param type 类型
     * @return 属性名称集合
     */
    public static Property[] getDeclaredProperties(Type type) {
        // 获取真实类型信息
        Class<?> clazz;
        Map<String, Type> genericTypeMap = new HashMap<>();
        if (type instanceof Class) {
            clazz = (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            try {
                clazz = getGenericClass(type);
                Method getGenericInfoMethod = clazz.getMethod("getGenericInfo");
                getGenericInfoMethod.setAccessible(true);
                Object classRepository = getGenericInfoMethod.invoke(clazz);
                Method getTypeParametersMethod = classRepository.getClass().getMethod("getTypeParameters");
                TypeVariable<?>[] typeParameters = (TypeVariable<?>[]) getTypeParametersMethod.invoke(classRepository);
                Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                for (int i = 0; i < typeParameters.length; i++) {
                    genericTypeMap.put(typeParameters[i].getName(), actualTypeArguments[i]);
                }
            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
                return null;
            }
        } else {
            return null;
        }

        // 递归找父类属性
        Property[] superProperty = clazz.getSuperclass() == null ? new Property[0] : getDeclaredProperties(getRealType(clazz.getGenericSuperclass(), genericTypeMap));
        Map<String, Property> propertyMap = Arrays.stream(superProperty).collect(Collectors.toMap(Property::getName, Function.identity()));

        Set<String> names = new HashSet<>();
        Map<String, Method> getterMethodMap = new HashMap<>();
        Map<String, Method> setterMethodMap = new HashMap<>();
        Map<String, Type> realTypeMap = new HashMap<>();
        Map<String, Field> fieldMap = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && !method.getReturnType().getName().equals("void") && !method.getName().equals("getClass") && (method.getName().startsWith("get") || method.getName().startsWith("is"))) {
                String methodPropertyName = Property.getMethodPropertyName(method.getName());
                names.add(methodPropertyName);
                getterMethodMap.put(methodPropertyName, method);
                realTypeMap.put(methodPropertyName, getRealType(method.getGenericReturnType(), genericTypeMap));
            } else if (method.getParameterCount() == 1 && method.getReturnType().getName().equals("void") && method.getName().startsWith("set")) {
                String methodPropertyName = Property.getMethodPropertyName(method.getName());
                names.add(methodPropertyName);
                setterMethodMap.put(methodPropertyName, method);
                realTypeMap.put(methodPropertyName, getRealType(method.getGenericParameterTypes()[0], genericTypeMap));
            }
        }
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            names.add(field.getName());
            fieldMap.put(field.getName(), field);
            realTypeMap.put(field.getName(), getRealType(field.getGenericType(), genericTypeMap));
        }

        // 字段名不规范兼容处理
        Iterator<String> namesIterator = names.iterator();
        while (namesIterator.hasNext()) {
            String name = namesIterator.next();
            String errorName = name.substring(0, 1).toUpperCase() + name.substring(1);
            if (Character.isLowerCase(name.charAt(0)) // 首字母小写
                    && names.contains(errorName) // 存在首字母大写的同样字段
                    && !fieldMap.containsKey(name)) { // 没有对应的field
                // field首字母错误书写，代码容错
                namesIterator.remove();
                getterMethodMap.put(errorName, getterMethodMap.get(name));
                setterMethodMap.put(errorName, setterMethodMap.get(name));
            }
        }
        names.forEach(x -> propertyMap.put(x, new Property(x, fieldMap.get(x), getterMethodMap.get(x), setterMethodMap.get(x), realTypeMap.get(x))));
        return propertyMap.values().toArray(new Property[0]);
    }

    public static final class Property {
        private final Field field;
        private final Method getterMethod;
        private final Method setterMethod;

        private final Type realType;
        private final String name;

        public Property(String name, Field field, Method getterMethod, Method setterMethod, Type realType) {
            this.name = name;
            this.field = field;
            this.getterMethod = getterMethod;
            this.setterMethod = setterMethod;
            this.realType = realType;
        }

        public void setAccessible(boolean flag) throws SecurityException {
            if (field != null) {
                field.setAccessible(flag);
            }
            if (getterMethod != null) {
                getterMethod.setAccessible(flag);
            }
            if (setterMethod != null) {
                setterMethod.setAccessible(flag);
            }
        }

        public Object get(Object obj) throws InvocationTargetException, IllegalAccessException {
            if (getterMethod != null) {
                return getterMethod.invoke(obj);
            } else if (field != null) {
                return field.get(obj);
            } else {
                throw new IllegalAccessException("属性异常");
            }
        }

        public void set(Object obj, Object value) throws InvocationTargetException, IllegalAccessException {
            if (setterMethod != null) {
                setterMethod.invoke(obj, value);
            } else if (field != null) {
                field.set(obj, value);
            } else {
                throw new IllegalAccessException("属性异常");
            }
        }

        public String getName() {
            return this.name;
        }

        public Class<?> getType() throws IllegalAccessException {
            if (field != null) {
                return field.getType();
            } else if (getterMethod != null) {
                return getterMethod.getReturnType();
            } else if (setterMethod != null) {
                return setterMethod.getParameterTypes()[0];
            } else {
                throw new IllegalAccessException("属性异常");
            }
        }

        public Type getGenericType() throws IllegalAccessException {
            if (field != null) {
                return field.getGenericType();
            } else if (getterMethod != null) {
                return getterMethod.getGenericReturnType();
            } else if (setterMethod != null) {
                return setterMethod.getGenericParameterTypes()[0];
            } else {
                throw new IllegalAccessException("属性异常");
            }
        }

        public Type getRealType() throws IllegalAccessException {
            if (realType != null) {
                return this.realType;
            } else {
                return getType();
            }
        }

        /**
         * 通过Getter方法名获取属性名
         *
         * @param methodName Getter/Setter方法名
         * @return 属性名
         */
        public static String getMethodPropertyName(String methodName) {
            String propertyName = "";
            if (GET_PATTERN.matcher(methodName).matches() || SET_PATTERN.matcher(methodName).matches()) {
                propertyName = methodName.substring(3);
            } else if (IS_PATTERN.matcher(methodName).matches()) {
                propertyName = methodName.substring(2);
            }
            return Introspector.decapitalize(propertyName);
        }

        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            if (getterMethod != null) {
                T annotation = getterMethod.getAnnotation(annotationClass);
                if (annotation != null) {
                    return annotation;
                }
            }
            if (field != null) {
                return field.getAnnotation(annotationClass);
            }
            return null;
        }
    }

    private static final Map<Class<?>, Object> defaultValues = new HashMap<Class<?>, Object>() {{
        put(boolean.class, false);
        put(byte.class, (byte) 0);
        put(short.class, (short) 0);
        put(int.class, 0);
        put(long.class, (long) 0);
    }};

    /*
     * 创建类实体, 适配各种类型
     * @author 小明同学
     * @date 2024/4/25 16:57
     * @param clazz
     * @return T
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T newInstance(Class<T> clazz) {
        if (clazz.isPrimitive()) {
            return (T) defaultValues.get(clazz);
        } else if (clazz.isArray()) {
            return (T) Array.newInstance(clazz.getComponentType(), 0);
        } else if (!clazz.isInterface()) {
            return newInstanceForClass(clazz);
        } else if (isAssignableFrom(Collection.class, clazz)) {
            return (T) newCollection(clazz);
        } else if (isAssignableFrom(Map.class, clazz)) {
            return (T) newMap(clazz);
        } else {
            try {
                Class<?> implClass = getImplClass(clazz);
                if (implClass == null) {
                    return null;
                }
                return (T) newInstanceForClass(implClass);
            } catch (IOException ignored) {
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstanceForClass(@NotNull Class<T> clazz) {
        // 获取 MyClass 的所有构造函数
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        // 遍历所有构造函数，找到第一个有参数的构造函数
        for (Constructor<?> constructor : constructors) {
            try {
                constructor.setAccessible(true);
                // 获取构造函数参数类型
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                // 定义参数数组
                Object[] parameterArgs = new Object[parameterTypes.length];
                // 遍历参数类型，创建参数对象
                for (int i = 0; i < parameterTypes.length; i++) {
                    // 如果是基本数据类型或String，则设置为默认值
                    if (isPrimitive(parameterTypes[i])) {
                        parameterArgs[i] = Class.forName(parameterTypes[i].getName()).newInstance();
                    } else {
                        // 如果是其他引用类型，则递归创建实例对象
                        parameterArgs[i] = newInstanceForClass(parameterTypes[i]);
                    }
                }
                return (T) constructor.newInstance(parameterArgs);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException ignored) {
            }
        }
        return null;
    }


    /**
     * 创建集合
     *
     * @param clazz 集合类型
     * @return 集合
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Collection<T> newCollection(Class<?> clazz) {
        try {
            return (Collection<T>) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ignored) {
            if (isAssignableFrom(clazz, SortedSet.class)) {
                return new TreeSet<>();
            } else if (isAssignableFrom(clazz, Set.class)) {
                return new HashSet<>();
            } else if (isAssignableFrom(clazz, Queue.class)) {
                return new LinkedList<>();
            } else if (isAssignableFrom(clazz, List.class)) {
                return new ArrayList<>();
            } else if (isAssignableFrom(clazz, Vector.class)) {
                return new Vector<>();
            } else {
                return null;
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <K, V> Map<K, V> newMap(Class<?> clazz) {
        try {
            return (Map<K, V>) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ignored) {
            if (isAssignableFrom(clazz, SortedMap.class)) {
                return new TreeMap<>();
            } else if (isAssignableFrom(clazz, Dictionary.class)) {
                return new Hashtable<>();
            } else if (isAssignableFrom(clazz, Map.class)) {
                return new HashMap<>();
            } else {
                return null;
            }
        }
    }

    /**
     * 获取实例类
     *
     * @param clazz 类
     * @return 实例类
     * @throws IOException 读取类异常
     */
    @SafeVarargs
    public static Class<?> getImplClass(Class<?> clazz, Predicate<Class<?>>... p) throws IOException {
        if (!Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers())) {
            return clazz;
        }

        String basePackage = clazz.getPackage().getName().replace(".", "/");//.substring(0, clazz.getPackage().getName().indexOf("."));
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        List<File> classFiles = new ArrayList<>();
        Enumeration<URL> resources = classLoader.getResources(basePackage);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            classFiles.add(new File(url.getFile()));
        }
        for (File classFile : classFiles) {
            List<Class<?>> classList = findClass(classFile, basePackage);
            for (Class<?> c : classList) {
                if (!Modifier.isAbstract(c.getModifiers()) && !Modifier.isInterface(c.getModifiers()) && isAssignableFrom(clazz, c)) {
                    boolean condition = true;
                    for (Predicate<Class<?>> predicate : p) {
                        condition = condition & predicate.test(c);
                    }
                    if (condition) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    public static List<Class<?>> findClass(File file, String basePackage) {
        List<Class<?>> clazzList = new ArrayList<>();
        if (!file.exists()) {
            return clazzList;
        }

        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    List<Class<?>> arrayList = findClass(f, basePackage + "." + f.getName());
                    clazzList.addAll(arrayList);
                } else if (f.getName().endsWith(".class")) {
                    try {
                        clazzList.add(Class.forName(basePackage + "." + f.getName().substring(0, f.getName().length() - 6)));
                    } catch (Throwable ignored) {
                    }
                }

            }
        }
        return clazzList;
    }


    /**
     * 转换基础类型
     *
     * @param obj    待转换实体
     * @param tClass 转换类型
     * @param <T>    转换类型
     * @return 转换结果
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertPrimitive(Object obj, Class<T> tClass, Converter<?, ?>... converters) {

        if (obj == null) {
            return null;
        }
        try {
            for (Converter<?, ?> converter : converters) {
                if (isAssignableFrom(converter.fromClass, obj.getClass()) && isAssignableFrom(tClass, converter.toClass)) {
                    return ((Converter<Object, ? extends T>) converter).convert(obj);
                }
            }
            if (isAssignableFrom(tClass, obj.getClass())) {
                // 类型一致，直接强转
                return (T) obj;
            } else if (tClass.equals(Integer.class) || tClass.equals(int.class)) {
                return (T) (Integer) Integer.parseInt(obj.toString());
            } else if (tClass.equals(Long.class) || tClass.equals(long.class)) {
                return (T) (Long) Long.parseLong(obj.toString());
            } else if (tClass.equals(Short.class) || tClass.equals(short.class)) {
                return (T) (Short) Short.parseShort(obj.toString());
            } else if (tClass.equals(Byte.class) || tClass.equals(byte.class)) {
                return (T) (Byte) Byte.parseByte(obj.toString());
            } else if (tClass.equals(Float.class) || tClass.equals(float.class)) {
                return (T) (Float) Float.parseFloat(obj.toString());
            } else if (tClass.equals(Double.class) || tClass.equals(double.class)) {
                return (T) (Double) Double.parseDouble(obj.toString());
            } else if (tClass.equals(Boolean.class) || tClass.equals(boolean.class)) {
                return (T) (Boolean) Boolean.parseBoolean(obj.toString());
            } else if (tClass.equals(String.class)) {
                return (T) obj.toString();
            } else {
                return null;
            }
        } catch (Throwable e) {
            // 转换异常，使用默认值
            return (T) defaultValues.get(tClass);
        }
    }

}
