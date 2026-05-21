package io.github.kamioj.quickstart.demo;

import io.github.kamioj.quickstart.entity.DeptDo;
import io.github.kamioj.quickstart.entity.UserDo;
import io.github.kamioj.quickstart.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 进阶篇 · 写操作 {@code update} / {@code updateJoin} / {@code remove}。
 *
 * <p>补全 CRUD 的 U 与 D：{@code ApiShowcase} 的 demo5 演示的是 {@code executableStream}
 * 流式更新，本类演示更常用的 service 级一行式写法。
 *
 * <p>注意 {@code UserDo} 标了 {@code @TableLogic}，所以 {@code remove} 不是物理 DELETE，
 * 而是逻辑删除（UPDATE deleted=1）——这正是 demo3 要演示的点。
 */
@Slf4j
@Component
@Order(8)
@RequiredArgsConstructor
public class WriteShowcase implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("\n---------- 写操作 update / updateJoin / remove ----------");
        demo1_update();
        demo2_updateJoin();
        demo3_remove();
    }

    // ------------------------------------------------------------------
    //  Demo 1 — update：按条件更新指定列（set 直接给值）
    //  SQL: UPDATE qs_user SET name = ?, salary = ? WHERE id = 3 AND deleted = 0
    // ------------------------------------------------------------------
    void demo1_update() {
        log.info("[update]  更新前: {}", userService.get(UserDo::getId, 3L).getName());
        int rows = userService.update(
            set -> set
                .set(UserDo::getName, "Carol-2026")
                .set(UserDo::getSalary, 23000),
            where -> where.eq(UserDo::getId, 3L)
        );
        log.info("[update]  rows={}, 更新后: name={}, salary={}",
            rows,
            userService.get(UserDo::getId, 3L).getName(),
            userService.get(UserDo::getId, 3L).getSalary());
    }

    // ------------------------------------------------------------------
    //  Demo 2 — updateJoin：联表更新（按部门名定位，给工程部每人涨 1000）
    //  set 里用 setFunc + f.add 表达「列 = 列 + 常量」的自增语义。
    //  SQL: UPDATE qs_user u LEFT JOIN qs_dept d ON u.dept_id = d.id
    //       SET u.salary = u.salary + 1000
    //       WHERE d.name = '工程部' AND u.deleted = 0
    // ------------------------------------------------------------------
    void demo2_updateJoin() {
        int rows = userService.updateJoin(
            join -> join.leftJoin(DeptDo.class, UserDo::getDeptId, DeptDo::getId),
            set -> set.setFunc(UserDo::getSalary, f -> f.add(UserDo::getSalary, 1000)),
            where -> where.eq(DeptDo::getName, "工程部")
        );
        log.info("[updateJoin]  工程部全员涨薪, rows={}", rows);
    }

    // ------------------------------------------------------------------
    //  Demo 3 — remove：逻辑删除（因 UserDo 有 @TableLogic，并非物理 DELETE）
    //  SQL: UPDATE qs_user SET deleted = 1 WHERE id = 200 AND deleted = 0
    //  删除后 id=200 的行仍物理存在，只是被后续 deleted=0 的查询过滤掉。
    // ------------------------------------------------------------------
    void demo3_remove() {
        long before = userService.stream().count();
        int rows = userService.remove(where -> where.eq(UserDo::getId, 200L));
        long after = userService.stream().count();
        log.info("[remove]  逻辑删除 id=200, rows={}, 可见行数 {} -> {}", rows, before, after);
        log.info("[remove]  再查 id=200: {} (逻辑删除后已不可见)",
            userService.get(UserDo::getId, 200L));
    }
}
