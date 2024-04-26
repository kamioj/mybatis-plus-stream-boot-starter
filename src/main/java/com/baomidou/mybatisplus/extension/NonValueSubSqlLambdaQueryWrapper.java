package com.baomidou.mybatisplus.extension;

/**
 * 子语句表达式包装
 */
public final class NonValueSubSqlLambdaQueryWrapper extends AbstractSubSqlLambdaQueryWrapper<NonValueSubSqlLambdaQueryWrapper> {

    public NonValueSubSqlLambdaQueryWrapper() {
        this(null);
    }

    public NonValueSubSqlLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

    /**
     * 获取子查询sql并传入注入参数
     *
     * @param queryWrapper 待传入注入参数的queryWrapper
     * @return 子查询sql
     */
    @Override
    public String getSqlSegment(ExQueryWrapper<?> queryWrapper) {
        String customSqlSegment = "SELECT 1 FROM " + getQueryWrapper().getSqlFrom() + " " + getQueryWrapper().getCustomSqlSegment();
        return getSqlSegmentWithParam(customSqlSegment, getQueryWrapper(), queryWrapper);
    }
}
