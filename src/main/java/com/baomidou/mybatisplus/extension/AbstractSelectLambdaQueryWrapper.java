package com.baomidou.mybatisplus.extension;

/**
 * 选择表达式包装
 */
public abstract class AbstractSelectLambdaQueryWrapper<Children extends AbstractSelectLambdaQueryWrapper<Children>> extends LambdaQueryWrapper<Children> {
    public AbstractSelectLambdaQueryWrapper() {
        this(null);
    }

    public AbstractSelectLambdaQueryWrapper(ExQueryWrapper<?> queryWrapper) {
        super(queryWrapper);
    }

    /**
     * 去重筛选查询项
     *
     * @return 实例本身
     */
    public Children distinct() {
        getQueryWrapper().setDistinct(true);
        return typedThis;
    }
}
