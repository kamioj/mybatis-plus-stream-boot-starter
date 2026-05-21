package io.github.kamioj.quickstart.demo;

import io.github.kamioj.quickstart.entity.UserDo;
import io.github.kamioj.quickstart.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 进阶篇 · 单列取值 {@code listValues} —— 只查一列、直接拿到标量列表。
 *
 * <p>相比 {@code list} 取回整个实体再 {@code map} 提取字段，{@code listValues}
 * 在 SQL 层就只 SELECT 目标列，省一次实体映射，返回 {@code List<列类型>}。
 */
@Slf4j
@Component
@Order(6)
@RequiredArgsConstructor
public class ProjectionShowcase implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("\n---------- listValues 单列取值 ----------");
        demo1_allNames();
        demo2_conditionalTopN();
    }

    // ------------------------------------------------------------------
    //  Demo 1 — 取全部用户名 -> List<String>
    //  SQL: SELECT name FROM qs_user WHERE deleted = 0
    // ------------------------------------------------------------------
    void demo1_allNames() {
        List<String> names = userService.listValues(where -> {}, UserDo::getName);
        log.info("[listValues]  全部用户名: {}", names);
    }

    // ------------------------------------------------------------------
    //  Demo 2 — 条件 + 排序 + limit 取单列：年龄>24 的薪资前 3 高
    //  SQL: SELECT salary FROM qs_user WHERE age > 24 AND deleted = 0
    //       ORDER BY salary DESC LIMIT 3
    // ------------------------------------------------------------------
    void demo2_conditionalTopN() {
        List<Integer> topSalaries = userService.listValues(
            where -> where.gt(UserDo::getAge, 24),
            order -> order.orderDesc(UserDo::getSalary),
            3,
            UserDo::getSalary
        );
        log.info("[listValues]  年龄>24 薪资前3: {}", topSalaries);
    }
}
