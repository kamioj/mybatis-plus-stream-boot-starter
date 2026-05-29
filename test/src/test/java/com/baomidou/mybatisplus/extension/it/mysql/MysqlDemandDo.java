package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("mps_demand")
public class MysqlDemandDo {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("title")
    private String title;

    @TableField("status")
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static MysqlDemandDo of(long id, long userId, String title, String status) {
        MysqlDemandDo demand = new MysqlDemandDo();
        demand.id = id;
        demand.userId = userId;
        demand.title = title;
        demand.status = status;
        return demand;
    }
}
