package com.baomidou.mybatisplus.extension.dialect;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis {@code @InsertProvider} 入口。
 *
 * <p>本类<b>不要直接调用</b>——由 {@code StreamBaseMapper#mergeInto} 通过 {@code @InsertProvider}
 * 自动反射调用。它把请求委托给当前活跃 {@link SqlDialect} 的 {@link SqlDialect#buildMergeIntoScript}。
 *
 * <p>对单一 mapper 方法绑定多个方言实现（每个方言生成不同的 SQL）的标准 MyBatis 模式。
 */
public final class MergeIntoSqlProvider {

    public MergeIntoSqlProvider() {}

    public String buildSql(@Param("columns") String[] columns,
                           @Param("values") Object[][] values,
                           @Param("flatValues") Object[] flatValues,
                           @Param(Constants.WRAPPER) ExecutableQueryWrapper<?> wrapper) {
        return DialectRegistry.current().buildMergeIntoScript(
            new MergeIntoContext(columns, values, flatValues, wrapper));
    }
}
