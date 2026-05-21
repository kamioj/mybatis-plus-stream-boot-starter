package io.github.kamioj.quickstart.demo;

import io.github.kamioj.quickstart.entity.UserDo;
import io.github.kamioj.quickstart.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 进阶篇 · 单行读取 {@code getOrDefault} / {@code getValue} / {@code getByKeyForUpdate}。
 *
 * <p>{@code ApiShowcase} 的 {@code get} 命中不到会返回 null；本类演示三个更省事的变体：
 * <ul>
 *   <li>{@code getOrDefault} —— 查不到时返回兜底实体，免 null 判断</li>
 *   <li>{@code getValue}     —— 只取一行的某个列，免取回整个实体再 getter</li>
 *   <li>{@code getByKeyForUpdate} —— 按主键取一行并加行锁（SELECT ... FOR UPDATE）</li>
 * </ul>
 */
@Slf4j
@Component
@Order(9)
@RequiredArgsConstructor
public class SingleRowShowcase implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("\n---------- 单行读取 getOrDefault / getValue / getByKeyForUpdate ----------");
        demo1_getOrDefault();
        demo2_getValue();
        demo3_getByKeyForUpdate();
    }

    // ------------------------------------------------------------------
    //  Demo 1 — getOrDefault：查不到时返回兜底实体，而不是 null
    //  SQL: SELECT * FROM qs_user WHERE id = 99999 AND deleted = 0 LIMIT 1
    // ------------------------------------------------------------------
    void demo1_getOrDefault() {
        UserDo fallback = new UserDo();
        fallback.setName("(匿名用户)");
        UserDo missing = userService.getOrDefault(UserDo::getId, 99999L, fallback);
        log.info("[getOrDefault]  id=99999 不存在 -> 兜底: {}", missing.getName());
    }

    // ------------------------------------------------------------------
    //  Demo 2 — getValue：只取一行的某一列，省一次整实体映射
    //  SQL: SELECT name FROM qs_user WHERE id = 1 AND deleted = 0 LIMIT 1
    // ------------------------------------------------------------------
    void demo2_getValue() {
        String name = userService.getValue(UserDo::getId, 1L, UserDo::getName);
        log.info("[getValue]  id=1 的 name = {}", name);
    }

    // ------------------------------------------------------------------
    //  Demo 3 — getByKeyForUpdate：按主键取一行并加行锁
    //  注：行锁需在事务内（@Transactional）才有实际意义；此处仅演示 API 与 SQL 生成。
    //  SQL: SELECT * FROM qs_user WHERE id = 1 AND deleted = 0 FOR UPDATE
    // ------------------------------------------------------------------
    void demo3_getByKeyForUpdate() {
        UserDo locked = userService.getByKeyForUpdate(1L);
        log.info("[getByKeyForUpdate]  按主键取并加锁: {}", locked == null ? "(无)" : locked.getName());
    }
}
