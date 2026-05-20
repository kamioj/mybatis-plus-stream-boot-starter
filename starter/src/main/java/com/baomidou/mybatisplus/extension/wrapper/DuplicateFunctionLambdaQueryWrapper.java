package com.baomidou.mybatisplus.extension.wrapper;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.dialect.DialectQuoteTranslator;

import java.util.function.Consumer;

/**
 * duplicate语句中函数表达式包装
 *
 * @param <T> 实体类
 */
public final class DuplicateFunctionLambdaQueryWrapper<T> extends AbstractFunctionLambdaQueryWrapper<DuplicateWhereLambdaQueryWrapper<T>, DuplicateFunctionLambdaQueryWrapper<T>> {

    public DuplicateFunctionLambdaQueryWrapper() {
        super();
    }

    @Override
    @SuppressWarnings({"unchecked", "RedundantCast"})
    protected <C extends AbstractCaseLambdaQueryWrapper<DuplicateWhereLambdaQueryWrapper<T>, DuplicateFunctionLambdaQueryWrapper<T>, V, C>, V> C getCaseLambdaQueryWrapperInstance() {
        return (C) (Object) new DuplicateCaseLambdaQueryWrapper<T, V>();
    }

    public <V> V _case(Consumer<DuplicateCaseLambdaQueryWrapper<T, V>> _case) {
        return _case0(_case);
    }

    public <V> V duplicateValue(SFunction<T, ?> column) {
        // Phase 4: 产出方言中立的 INCOMING token，由渲染边界翻译为 VALUES/EXCLUDED/src
        try {
            String[] qc = getQualifierAndColumn(column);
            sqlSegment = DialectQuoteTranslator.incomingToken(qc[1]);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

}
