package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("order_stats")
public class OrderStatsDo {

    @TableField("user_id")
    private Long userId;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
