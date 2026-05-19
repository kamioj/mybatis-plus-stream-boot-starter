package com.baomidou.mybatisplus.extension.bo;

import java.io.Serializable;
import java.util.List;

public class PageVo implements Serializable {
    /**
     * 页码
     */
    private Integer pageNum;
    /**
     * 页大小
     */
    private Integer pageSize;
    private String orderBy;
    /**
     * 排序规则
     */
    private List<SortVo> order;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    @Deprecated
    public String getOrderBy() {
        return orderBy;
    }

    @Deprecated
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public List<SortVo> getOrder() {
        return order;
    }

    public void setOrder(List<SortVo> order) {
        this.order = order;
    }
}
