package io.github.kamioj.quickstart.dto;

import lombok.Data;

/**
 * {@code listJoin} 投影 VO：用户名 + 所属部门名。
 *
 * <p>字段名即投影目标名——select lambda 里 {@code .select(源::getXxx, UserDeptVo::getYyy)}
 * 的第二个参数是本类的 getter 方法引用，库据此反射出列别名并回写。
 */
@Data
public class UserDeptVo {

    /** 来自 qs_user.name */
    private String userName;

    /** 来自 qs_dept.name */
    private String deptName;
}
