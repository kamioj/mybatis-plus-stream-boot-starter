package io.github.kamioj.quickstart.demo;

import io.github.kamioj.quickstart.entity.UserDo;
import io.github.kamioj.quickstart.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基础篇 —— 启动后自动运行，在控制台演示最常用的 API。
 *
 * <p>每个代码块上方的注释就是它实际生成的 SQL，
 * 对照阅读可以快速建立"Java 链式调用 → SQL 子句"的心智模型。
 *
 * <p>进阶用法见同包下的 {@code JoinShowcase} / {@code GroupShowcase} /
 * {@code PageShowcase} / {@code ConflictWriteShowcase} /
 * {@code ProjectionShowcase} / {@code StreamTerminalShowcase} /
 * {@code WriteShowcase} / {@code SingleRowShowcase}。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ApiShowcase implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("\n\n========== MyBatis-Plus Stream Quickstart ==========\n");

        demo1_get();
        demo2_list_filter_sorted_limit();
        demo3_stream_pipeline();
        demo4_count_exist();
        demo5_update();
        demo6_batch_save();

        log.info("\n========== Demo complete. All API calls succeeded. ==========\n");
    }

    // ------------------------------------------------------------------
    //  Demo 1 — 单条查询
    //  SQL: SELECT * FROM qs_user WHERE id = 1 LIMIT 1
    // ------------------------------------------------------------------
    void demo1_get() {
        UserDo alice = userService.get(UserDo::getId, 1L);
        log.info("[GET by id]  name={}, age={}", alice.getName(), alice.getAge());

        // 多条件版
        // SQL: SELECT * FROM qs_user WHERE age > 27 AND deleted = 0 LIMIT 1
        UserDo senior = userService.get(w -> w.gt(UserDo::getAge, 27));
        log.info("[GET by cond] name={}", senior.getName());
    }

    // ------------------------------------------------------------------
    //  Demo 2 — 列表 + 排序 + 分页
    //  SQL: SELECT * FROM qs_user WHERE deleted = 0 ORDER BY age DESC LIMIT 2
    // ------------------------------------------------------------------
    void demo2_list_filter_sorted_limit() {
        List<UserDo> top2 = userService.list(
            where -> {},
            order -> order.orderDesc(UserDo::getAge),
            2
        );
        log.info("[LIST top2 by age]  {}", top2.stream().map(UserDo::getName).toList());
    }

    // ------------------------------------------------------------------
    //  Demo 3 — Stream pipeline（像写 Java Stream 一样）
    //  SQL: SELECT * FROM qs_user WHERE age >= 25 AND deleted = 0 ORDER BY age ASC
    // ------------------------------------------------------------------
    void demo3_stream_pipeline() {
        List<String> names = userService.stream()
            .filter(w -> w.ge(UserDo::getAge, 25))
            .sorted(o -> o.orderAsc(UserDo::getAge))
            .map(UserDo::getName)
            .collect(Collectors.toList());
        log.info("[STREAM pipeline]  age>=25 ordered: {}", names);
    }

    // ------------------------------------------------------------------
    //  Demo 4 — count / exist
    //  SQL: SELECT COUNT(*) FROM qs_user WHERE deleted = 0
    // ------------------------------------------------------------------
    void demo4_count_exist() {
        long total = userService.stream().count();
        boolean hasYoung = userService.exist(w -> w.lt(UserDo::getAge, 25));
        log.info("[COUNT/EXIST]  total={}, hasUserUnder25={}", total, hasYoung);
    }

    // ------------------------------------------------------------------
    //  Demo 5 — 更新
    //  SQL: UPDATE qs_user SET age = age + 1 WHERE id = 5 AND deleted = 0
    // ------------------------------------------------------------------
    void demo5_update() {
        int rows = userService.executableStream()
            .set(s -> s.setFunc(UserDo::getAge, f -> f.add(UserDo::getAge, 1)))
            .filter(w -> w.eq(UserDo::getId, 5L))
            .executeUpdate();
        log.info("[UPDATE setFunc]  rows affected={}", rows);
    }

    // ------------------------------------------------------------------
    //  Demo 6 — 批量写入
    //  SQL: INSERT INTO qs_user (...) VALUES (...), (...)
    // ------------------------------------------------------------------
    void demo6_batch_save() {
        UserDo frank = new UserDo();
        frank.setId(100L);
        frank.setName("Frank");
        frank.setAge(40);
        frank.setDeptId(1L);

        UserDo grace = new UserDo();
        grace.setId(101L);
        grace.setName("Grace");
        grace.setAge(33);
        grace.setDeptId(2L);

        int rows = userService.saveBatchWithoutId(List.of(frank, grace));
        log.info("[BATCH SAVE]  inserted={}", rows);

        long newTotal = userService.stream().count();
        log.info("[COUNT after insert]  total={}", newTotal);
    }
}
