package com.baomidou.mybatisplus.extension;


import com.baomidou.mybatisplus.extension.mapper.MysqlBaseMapper;
import com.baomidou.mybatisplus.toolkit.MybatisUtil;
import com.sun.istack.internal.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.*;
import java.util.stream.*;

public abstract class MybatisStream<T, R, Wrapper extends ExQueryWrapper<T>, Children extends MybatisStream<T, R, Wrapper, Children>> implements AutoCloseable /*Stream<R>*/ {
    @SuppressWarnings("unchecked")
    protected final Children typedThis = (Children) this;

    protected final Wrapper queryWrapper;

    protected final Class<T> entityClass;
    protected final MysqlBaseMapper<T> baseMapper;

    protected Consumer<? super R> peekAction;

    public MybatisStream(Wrapper queryWrapper, Class<T> entityClass, MysqlBaseMapper<T> baseMapper) {
        this.queryWrapper = queryWrapper;
        this.entityClass = entityClass;
        this.baseMapper = baseMapper;
    }

    /**
     * 表
     *
     * @param join 连表表达式
     * @return 实例本身
     */
    public Children join(Consumer<JoinLambdaQueryWrapper<T>> join) {
        return join(null, join);
    }

    /**
     * 表
     *
     * @param rename 表重命名
     * @param join   连表表达式
     * @return 实例本身
     */
    public Children join(String rename, Consumer<JoinLambdaQueryWrapper<T>> join) {
        queryWrapper.setFromTable(MybatisUtil.getTableInfo(entityClass), rename);

        if (join != null) {
            JoinLambdaQueryWrapper<T> joinLambda = new JoinLambdaQueryWrapper<>(queryWrapper, entityClass, rename);
            join.accept(joinLambda);
        }
        return typedThis;
    }

    /**
     * 条件
     *
     * @param predicate 条件表达式
     * @return 实例本身
     */
    public Children filter(Consumer<NormalWhereLambdaQueryWrapper> predicate) {
        NormalWhereLambdaQueryWrapper whereLambda = new NormalWhereLambdaQueryWrapper(queryWrapper);
        predicate.accept(whereLambda);
        return typedThis;
    }

//    @Override
//    public Stream<R> filter(Predicate<? super R> predicate) {
//        throw new UnsupportedOperationException();
//    }

    public <R1> Stream<R1> map(Function<? super R, ? extends R1> mapper) {
        return toPeekStream().map(mapper);
    }

    public IntStream mapToInt(ToIntFunction<? super R> mapper) {
        return toPeekStream().mapToInt(mapper);
    }

    public LongStream mapToLong(ToLongFunction<? super R> mapper) {
        return toPeekStream().mapToLong(mapper);
    }

    public DoubleStream mapToDouble(ToDoubleFunction<? super R> mapper) {
        return toPeekStream().mapToDouble(mapper);
    }

    public <R1> Stream<R1> flatMap(Function<? super R, ? extends Stream<? extends R1>> mapper) {
        return toPeekStream().flatMap(mapper);
    }

    public IntStream flatMapToInt(Function<? super R, ? extends IntStream> mapper) {
        return toPeekStream().flatMapToInt(mapper);
    }

    public LongStream flatMapToLong(Function<? super R, ? extends LongStream> mapper) {
        return toPeekStream().flatMapToLong(mapper);
    }

    public DoubleStream flatMapToDouble(Function<? super R, ? extends DoubleStream> mapper) {
        return toPeekStream().flatMapToDouble(mapper);
    }

    /**
     * 去重
     *
     * @return 实例本身
     */
    public abstract Children distinct();

    public Children peek(Consumer<? super R> action) {
        this.peekAction = action;
        return typedThis;
    }

    /**
     * 排序
     *
     * @param order 排序表达式
     * @return 实例本身
     */
    public abstract Children sorted(Consumer<OrderLambdaQueryWrapper> order);

//    @Override
//    public Stream<R> sorted() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Stream<R> sorted(Comparator<? super R> comparator) {
//        throw new UnsupportedOperationException();
//    }

    /**
     * 限制条数
     *
     * @param maxSize 限制条数
     * @return 实例本身
     */
    public abstract Children limit(long maxSize);

    /**
     * 跳过条数
     *
     * @param n 跳过条数
     * @return 实例本身
     */
    public abstract Children skip(long n);

    public void forEach(Consumer<? super R> action) {
        toPeekStream().forEach(action);
    }

    public void forEachOrdered(Consumer<? super R> action) {
        toPeekStream().forEachOrdered(action);
    }

    @NotNull
    public Object[] toArray() {
        return toPeekStream().toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @NotNull
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return toPeekStream().toArray(generator);
    }

    public R reduce(R identity, BinaryOperator<R> accumulator) {
        return toPeekStream().reduce(identity, accumulator);
    }

    @NotNull
    public Optional<R> reduce(BinaryOperator<R> accumulator) {
        return toPeekStream().reduce(accumulator);
    }

    public <U> U reduce(U identity, BiFunction<U, ? super R, U> accumulator, BinaryOperator<U> combiner) {
        return toPeekStream().reduce(identity, accumulator, combiner);
    }

    public <C> C collect(Supplier<C> supplier, BiConsumer<C, ? super R> accumulator, BiConsumer<C, C> combiner) {
        return toPeekStream().collect(supplier, accumulator, combiner);
    }

    public <C, A> C collect(Collector<? super R, A, C> collector) {
        return toPeekStream().collect(collector);
    }

    @NotNull
    public Optional<R> min(Comparator<? super R> comparator) {
        return toPeekStream().min(comparator);
    }

    @NotNull
    public Optional<R> max(Comparator<? super R> comparator) {
        return toPeekStream().max(comparator);
    }

    public abstract boolean exist();

    public abstract long count();

    public boolean anyMatch(Predicate<? super R> predicate) {
        return toPeekStream().anyMatch(predicate);
    }

    public boolean allMatch(Predicate<? super R> predicate) {
        return toPeekStream().anyMatch(predicate);
    }

    public boolean noneMatch(Predicate<? super R> predicate) {
        return toPeekStream().noneMatch(predicate);
    }

    public Optional<R> findFirst() {
        return findAny();
    }

    public Optional<R> findAny() {
        return limit(1).toPeekStream().findAny();
    }

    @NotNull
    public Iterator<R> iterator() {
        return toPeekStream().iterator();
    }

    @NotNull
    public Spliterator<R> spliterator() {
        return toPeekStream().spliterator();
    }

    public boolean isParallel() {
        return toPeekStream().isParallel();
    }

    @NotNull
    public Stream<R> sequential() {
        return toPeekStream().sequential();
    }

    @NotNull
    public Stream<R> parallel() {
        return toPeekStream().parallel();
    }

    @NotNull
    public Stream<R> unordered() {
        return toPeekStream().unordered();
    }

    @NotNull
    public Stream<R> onClose(Runnable closeHandler) {
        return toPeekStream().onClose(closeHandler);
    }

    @Override
    public void close() {
        queryWrapper.clear();
    }

    protected abstract Stream<R> toStream();

    protected Stream<R> toPeekStream() {
        return peekAction == null ? toStream() : toStream().peek(peekAction);
    }

    public Children withDeleted() {
        return filter(AbstractWhereLambdaQueryWrapper::withDeleted);
    }

}
