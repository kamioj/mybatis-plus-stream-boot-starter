package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("mps_user")
public class MysqlUserDo {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("role_code")
    private String roleCode;

    @TableField("age")
    private Integer age;

    @TableField("credit_score")
    private Integer creditScore;

    @TableField("balance")
    private BigDecimal balance;

    @TableField("active")
    private Boolean active;

    @TableField("tags")
    private String tags;

    @TableField("manager_id")
    private Long managerId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Integer getCreditScore() { return creditScore; }
    public void setCreditScore(Integer creditScore) { this.creditScore = creditScore; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public static MysqlUserDo of(long id, String username, String roleCode, int age, int creditScore,
                                 String balance, boolean active, String tags, Long managerId,
                                 LocalDateTime createdAt, int deleted) {
        MysqlUserDo user = new MysqlUserDo();
        user.id = id;
        user.username = username;
        user.roleCode = roleCode;
        user.age = age;
        user.creditScore = creditScore;
        user.balance = new BigDecimal(balance);
        user.active = active;
        user.tags = tags;
        user.managerId = managerId;
        user.createdAt = createdAt;
        user.deleted = deleted;
        return user;
    }
}
