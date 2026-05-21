package io.github.kamioj.quickstart.demo;

import io.github.kamioj.quickstart.dto.DeptStatVo;
import io.github.kamioj.quickstart.entity.UserDo;
import io.github.kamioj.quickstart.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 进阶篇 · 分组聚合 {@code listGroup} —— 按部门统计人数与平均薪资。
 *
 * <p>group lambda 声明 GROUP BY 列；select lambda 里 {@code select(...)} 取分组列、
 * {@code selectFunc(...)} 取聚合函数（count / avg / sum），结果投影到 DTO。
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class GroupShowcase implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("\n---------- listGroup 分组聚合 ----------");
        demo_groupByDept();
    }

    // ------------------------------------------------------------------
    //  Demo — 按 dept_id 分组，统计每部门人数与平均薪资
    //  SQL: SELECT dept_id          AS deptId,
    //              COUNT(*)         AS userCount,
    //              AVG(salary)      AS avgSalary
    //       FROM qs_user WHERE deleted = 0
    //       GROUP BY dept_id
    // ------------------------------------------------------------------
    void demo_groupByDept() {
        List<DeptStatVo> stats = userService.listGroup(
            group -> group.groupBy(UserDo::getDeptId),
            where -> {},
            select -> select
                .select(UserDo::getDeptId, DeptStatVo::getDeptId)
                .selectFunc(func -> func.count(), DeptStatVo::getUserCount)
                .selectFunc(func -> func.avg(UserDo::getSalary), DeptStatVo::getAvgSalary),
            DeptStatVo.class
        );
        stats.forEach(s -> log.info("[listGroup]  部门{}: 人数={}, 平均薪资={}",
            s.getDeptId(), s.getUserCount(), s.getAvgSalary()));
    }
}
