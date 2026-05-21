package io.github.kamioj.quickstart.demo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.kamioj.quickstart.dto.DeptStatVo;
import io.github.kamioj.quickstart.dto.UserDeptVo;
import io.github.kamioj.quickstart.entity.DeptDo;
import io.github.kamioj.quickstart.entity.UserDo;
import io.github.kamioj.quickstart.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 进阶篇 · 分页 {@code page} / {@code pageJoin} / {@code pageGroup}。
 *
 * <p>首参传 MyBatis-Plus 的 {@code Page<>(当前页, 每页条数)}，当前页从 1 开始；
 * 返回的 {@code IPage} 用 {@code getTotal()} 取总数、{@code getRecords()} 取当前页数据。
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class PageShowcase implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("\n---------- page 分页查询 ----------");
        demo1_page();
        demo2_pageJoin();
        demo3_pageGroup();
    }

    // ------------------------------------------------------------------
    //  Demo 1 — 单表分页：第 1 页，每页 2 条
    //  SQL: SELECT * FROM qs_user WHERE deleted = 0 LIMIT 2 OFFSET 0
    //       (另发一条 SELECT COUNT(*) 求总数)
    // ------------------------------------------------------------------
    void demo1_page() {
        IPage<UserDo> page = userService.page(new Page<>(1, 2), where -> {});
        log.info("[page]  total={}, 第1页={}",
            page.getTotal(), page.getRecords().stream().map(UserDo::getName).toList());
    }

    // ------------------------------------------------------------------
    //  Demo 2 — 联表分页投影到 DTO：第 1 页，每页 3 条
    //  SQL: SELECT u.name AS userName, d.name AS deptName FROM qs_user u
    //       LEFT JOIN qs_dept d ON u.dept_id = d.id
    //       WHERE u.deleted = 0 LIMIT 3 OFFSET 0
    // ------------------------------------------------------------------
    void demo2_pageJoin() {
        IPage<UserDeptVo> page = userService.pageJoin(
            new Page<>(1, 3),
            join -> join.leftJoin(DeptDo.class, UserDo::getDeptId, DeptDo::getId),
            where -> {},
            select -> select
                .select(UserDo::getName, UserDeptVo::getUserName)
                .select(DeptDo::getName, UserDeptVo::getDeptName),
            UserDeptVo.class
        );
        log.info("[pageJoin]  total={}, 第1页={}", page.getTotal(),
            page.getRecords().stream().map(r -> r.getUserName() + "@" + r.getDeptName()).toList());
    }

    // ------------------------------------------------------------------
    //  Demo 3 — 分组分页：每部门统计行也能分页
    //  SQL: SELECT dept_id AS deptId, COUNT(*) AS userCount, AVG(salary) AS avgSalary
    //       FROM qs_user WHERE deleted = 0 GROUP BY dept_id LIMIT 2 OFFSET 0
    // ------------------------------------------------------------------
    void demo3_pageGroup() {
        IPage<DeptStatVo> page = userService.pageGroup(
            new Page<>(1, 2),
            group -> group.groupBy(UserDo::getDeptId),
            where -> {},
            select -> select
                .select(UserDo::getDeptId, DeptStatVo::getDeptId)
                .selectFunc(func -> func.count(), DeptStatVo::getUserCount)
                .selectFunc(func -> func.avg(UserDo::getSalary), DeptStatVo::getAvgSalary),
            DeptStatVo.class
        );
        log.info("[pageGroup]  total={}, 第1页={} 个部门统计行", page.getTotal(), page.getRecords().size());
    }
}
