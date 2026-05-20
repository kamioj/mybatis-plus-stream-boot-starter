package com.baomidou.mybatisplus.extension.dialect;

import com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper;

/**
 * {@link SqlDialect#buildMergeIntoScript(MergeIntoContext)} 的上下文参数（4.x Phase 5 引入）。
 *
 * <p>取代此前 {@code buildMergeIntoScript} 的三个重载——把列名、批量值、扁平值、执行 wrapper
 * 收敛进一个不可变上下文对象。后续若 MERGE 渲染还需要更多输入，加字段即可，不必再叠重载。
 */
public final class MergeIntoContext {

    private final String[] columns;
    private final Object[][] values;
    private final Object[] flatValues;
    private final ExecutableQueryWrapper<?> wrapper;

    public MergeIntoContext(String[] columns, Object[][] values, Object[] flatValues,
                            ExecutableQueryWrapper<?> wrapper) {
        this.columns = columns;
        this.values = values;
        this.flatValues = flatValues;
        this.wrapper = wrapper;
    }

    /** 列名数组（已带方言引号） */
    public String[] getColumns() {
        return columns;
    }

    /** 批量值（二维：行 × 列） */
    public Object[][] getValues() {
        return values;
    }

    /** 扁平化值数组（供 {@code #{flatValues[n]}} 占位符绑定） */
    public Object[] getFlatValues() {
        return flatValues;
    }

    /** 执行 wrapper（含表名、PK、setters、writeMode 等元数据） */
    public ExecutableQueryWrapper<?> getWrapper() {
        return wrapper;
    }
}
