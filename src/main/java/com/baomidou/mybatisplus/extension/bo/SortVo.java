package com.baomidou.mybatisplus.extension.bo;


public class SortVo {

    /**
     * 排序字段名
     */
    private String key;

    /**
     * 是否按升序排序
     */
    private Boolean asc;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Boolean getAsc() {
        return asc;
    }

    public void setAsc(Boolean asc) {
        this.asc = asc;
    }
}
