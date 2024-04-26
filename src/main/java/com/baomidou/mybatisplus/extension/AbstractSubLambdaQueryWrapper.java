package com.baomidou.mybatisplus.extension;

/**
 * 抽象子语句表达式包装
 *
 * @param <Children> 子类类型
 */
public abstract class AbstractSubLambdaQueryWrapper<Children extends AbstractSubLambdaQueryWrapper<Children>> extends LambdaQueryWrapper<Children> {

    public AbstractSubLambdaQueryWrapper() {
        super();
    }

    public AbstractSubLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

    /**
     * 获取sql，并添加输入参数
     *
     * @param queryWrapper queryWrapper
     * @return sqlSegment
     */
    public abstract String getSqlSegment(ExQueryWrapper<?> queryWrapper);
}
