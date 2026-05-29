package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("mps_order")
public class MysqlOrderDo {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static MysqlOrderDo of(long id, long userId, String amount, String status, LocalDateTime createdAt) {
        MysqlOrderDo order = new MysqlOrderDo();
        order.id = id;
        order.userId = userId;
        order.amount = new BigDecimal(amount);
        order.status = status;
        order.createdAt = createdAt;
        return order;
    }
}
