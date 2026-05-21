package io.github.kamioj.quickstart.dto;

import lombok.Data;

/**
 * {@code listGroup} 投影 VO：按部门聚合的统计行。
 *
 * <p>{@code deptId} 是 GROUP BY 列，其余两个字段对应 COUNT / AVG 聚合结果。
 */
@Data
public class DeptStatVo {

    /** GROUP BY qs_user.dept_id */
    private Long deptId;

    /** COUNT(*) —— 该部门人数 */
    private Long userCount;

    /** AVG(qs_user.salary) —— 该部门平均薪资 */
    private Double avgSalary;
}
