package io.github.kamioj.quickstart.demo;

import io.github.kamioj.quickstart.dto.UserDeptVo;
import io.github.kamioj.quickstart.entity.DeptDo;
import io.github.kamioj.quickstart.entity.UserDo;
import io.github.kamioj.quickstart.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 进阶篇 · 联表查询 {@code listJoin} —— qs_user LEFT JOIN qs_dept。
 *
 * <p>join lambda 声明关联表与 ON 条件，返回值有两种形态：
 * 不传 select 时返回主表实体 {@code List<UserDo>}；传 select + DTO class 时投影到 DTO。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class JoinShowcase implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("\n---------- listJoin 联表查询 ----------");
        demo1_joinReturnEntity();
        demo2_joinReturnDto();
    }

    // ------------------------------------------------------------------
    //  Demo 1 — 联表后返回主表实体（dept 表只参与过滤，不出现在结果列）
    //  SQL: SELECT u.* FROM qs_user u
    //       LEFT JOIN qs_dept d ON u.dept_id = d.id
    //       WHERE d.name = '工程部' AND u.deleted = 0
    // ------------------------------------------------------------------
    void demo1_joinReturnEntity() {
        List<UserDo> engineers = userService.listJoin(
            join -> join.leftJoin(DeptDo.class, UserDo::getDeptId, DeptDo::getId),
            where -> where.eq(DeptDo::getName, "工程部")
        );
        log.info("[listJoin 返回实体]  工程部成员: {}",
            engineers.stream().map(UserDo::getName).toList());
    }

    // ------------------------------------------------------------------
    //  Demo 2 — 联表后投影到 DTO（拼出 用户名 + 部门名）
    //  SQL: SELECT u.name AS userName, d.name AS deptName FROM qs_user u
    //       LEFT JOIN qs_dept d ON u.dept_id = d.id
    //       WHERE u.deleted = 0
    // ------------------------------------------------------------------
    void demo2_joinReturnDto() {
        List<UserDeptVo> rows = userService.listJoin(
            join -> join.leftJoin(DeptDo.class, UserDo::getDeptId, DeptDo::getId),
            where -> {},
            select -> select
                .select(UserDo::getName, UserDeptVo::getUserName)
                .select(DeptDo::getName, UserDeptVo::getDeptName),
            UserDeptVo.class
        );
        rows.forEach(r -> log.info("[listJoin 返回DTO]  {} @ {}", r.getUserName(), r.getDeptName()));
    }
}
