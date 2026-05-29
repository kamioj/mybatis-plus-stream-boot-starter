package com.baomidou.mybatisplus.extension.it.mysql;

import java.math.BigDecimal;

public class UserStatsDto {

    private Long userId;
    private String username;
    private String roleCode;
    private Long orderCount;
    private BigDecimal totalAmount;
    private Double avgScore;
    private String grade;
    private Long openDemandCount;
    private String usernames;
    private String distinctRoles;
    private String firstUsername;
    private Long paidCount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Double getAvgScore() { return avgScore; }
    public void setAvgScore(Double avgScore) { this.avgScore = avgScore; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Long getOpenDemandCount() { return openDemandCount; }
    public void setOpenDemandCount(Long openDemandCount) { this.openDemandCount = openDemandCount; }
    public String getUsernames() { return usernames; }
    public void setUsernames(String usernames) { this.usernames = usernames; }
    public String getDistinctRoles() { return distinctRoles; }
    public void setDistinctRoles(String distinctRoles) { this.distinctRoles = distinctRoles; }
    public String getFirstUsername() { return firstUsername; }
    public void setFirstUsername(String firstUsername) { this.firstUsername = firstUsername; }
    public Long getPaidCount() { return paidCount; }
    public void setPaidCount(Long paidCount) { this.paidCount = paidCount; }
}
