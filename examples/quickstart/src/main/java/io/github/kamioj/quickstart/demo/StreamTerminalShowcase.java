package io.github.kamioj.quickstart.demo;

import io.github.kamioj.quickstart.entity.UserDo;
import io.github.kamioj.quickstart.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 进阶篇 · 流式终端操作 —— {@code toMap*} 系列、{@code groupingBy}、{@code forUpdate}。
 *
 * <p>{@code toMapCount/Sum/Avg} 把 GROUP BY 聚合下推到 SQL 执行，只回传聚合结果；
 * {@code groupingBy} 则取回完整实体后在应用层分组。
 */
@Slf4j
@Component
@Order(7)
@RequiredArgsConstructor
public class StreamTerminalShowcase implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("\n---------- 流式终端操作 ----------");
        demo1_toMap();
        demo2_toMapAggregates();
        demo3_groupingBy();
        demo4_forUpdate();
    }

    // ------------------------------------------------------------------
    //  Demo 1 — toMap：两列直接收成 Map<id, name>
    //  SQL: SELECT id, name FROM qs_user WHERE deleted = 0
    // ------------------------------------------------------------------
    void demo1_toMap() {
        Map<Long, String> idToName = userService.stream().toMap(UserDo::getId, UserDo::getName);
        log.info("[toMap]  id->name: {}", idToName);
    }

    // ------------------------------------------------------------------
    //  Demo 2 — toMapCount / toMapSum / toMapAvg：GROUP BY 聚合下推 SQL
    //  SQL: SELECT dept_id, COUNT(*)/SUM(salary)/AVG(age) FROM qs_user
    //       WHERE deleted = 0 GROUP BY dept_id
    // ------------------------------------------------------------------
    void demo2_toMapAggregates() {
        Map<Long, Long> countByDept = userService.stream().toMapCount(UserDo::getDeptId);
        Map<Long, Integer> salarySumByDept = userService.stream().toMapSum(UserDo::getDeptId, UserDo::getSalary);
        Map<Long, Double> ageAvgByDept = userService.stream().toMapAvg(UserDo::getDeptId, UserDo::getAge);
        log.info("[toMapCount]  每部门人数: {}", countByDept);
        log.info("[toMapSum]    每部门薪资总和: {}", salarySumByDept);
        log.info("[toMapAvg]    每部门平均年龄: {}", ageAvgByDept);
    }

    // ------------------------------------------------------------------
    //  Demo 3 — groupingBy：取回完整实体后在应用层按 key 分组
    //  SQL: SELECT * FROM qs_user WHERE deleted = 0
    // ------------------------------------------------------------------
    void demo3_groupingBy() {
        Map<Long, List<UserDo>> usersByDept = userService.stream().groupingBy(UserDo::getDeptId);
        usersByDept.forEach((deptId, users) -> log.info("[groupingBy]  部门{}: {}",
            deptId, users.stream().map(UserDo::getName).toList()));
    }

    // ------------------------------------------------------------------
    //  Demo 4 — forUpdate：查询时对命中行加行锁（SELECT ... FOR UPDATE）
    //  注：行锁需在事务内（@Transactional）才有实际意义；此处仅演示链式写法与 SQL 生成。
    //  SQL: SELECT * FROM qs_user WHERE id = 1 AND deleted = 0 FOR UPDATE
    // ------------------------------------------------------------------
    void demo4_forUpdate() {
        Optional<UserDo> locked = userService.stream()
            .filter(where -> where.eq(UserDo::getId, 1L))
            .forUpdate()
            .findFirst();
        log.info("[forUpdate]  命中并加锁: {}", locked.map(UserDo::getName).orElse("(无)"));
    }
}
