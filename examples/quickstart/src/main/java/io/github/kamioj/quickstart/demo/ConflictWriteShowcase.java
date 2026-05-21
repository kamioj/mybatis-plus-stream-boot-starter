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
 * 进阶篇 · 冲突写入 {@code saveDuplicate} / {@code saveIgnore} / {@code saveReplace}。
 *
 * <p>三者对应主键/唯一键冲突时的三种策略：
 * <ul>
 *   <li>{@code saveDuplicate} —— INSERT ... ON DUPLICATE KEY UPDATE，冲突则更新指定列</li>
 *   <li>{@code saveIgnore}    —— INSERT IGNORE，冲突行静默跳过</li>
 *   <li>{@code saveReplace}   —— REPLACE INTO，冲突行整行覆盖</li>
 * </ul>
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class ConflictWriteShowcase implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        log.info("\n---------- 冲突写入策略 ----------");
        demo1_saveDuplicate();
        demo2_saveIgnore();
        demo3_saveReplace();
    }

    // ------------------------------------------------------------------
    //  Demo 1 — saveDuplicate：id=1 已存在，触发 UPDATE 分支
    //  SQL: INSERT INTO qs_user (...) VALUES (...)
    //       ON DUPLICATE KEY UPDATE name = VALUES(name), salary = VALUES(salary)
    // ------------------------------------------------------------------
    void demo1_saveDuplicate() {
        UserDo alice = newUser(1L, "Alice-v2", 31, 26000, 1L);
        int rows = userService.saveDuplicate(
            List.of(alice),
            duplicate -> duplicate
                .duplicate(UserDo::getName)
                .duplicate(UserDo::getSalary)
        );
        log.info("[saveDuplicate]  id=1 冲突触发更新, rows={}", rows);
        log.info("[saveDuplicate]  更新后: {}", userService.get(UserDo::getId, 1L).getName());
    }

    // ------------------------------------------------------------------
    //  Demo 2 — saveIgnore：id=1 冲突被忽略，id=200 为新行
    //  SQL: INSERT IGNORE INTO qs_user (...) VALUES (...), (...)
    // ------------------------------------------------------------------
    void demo2_saveIgnore() {
        UserDo conflict = newUser(1L, "ShouldBeIgnored", 99, 0, 1L);
        UserDo fresh = newUser(200L, "Ivan", 27, 21000, 3L);
        int rows = userService.saveIgnore(List.of(conflict, fresh));
        log.info("[saveIgnore]  传入2行(1冲突+1新), 实际插入 rows={}", rows);
    }

    // ------------------------------------------------------------------
    //  Demo 3 — saveReplace：id=2 已存在，整行被覆盖
    //  SQL: REPLACE INTO qs_user (...) VALUES (...)
    // ------------------------------------------------------------------
    void demo3_saveReplace() {
        UserDo bob = newUser(2L, "Bob-replaced", 26, 19000, 1L);
        int rows = userService.saveReplace(List.of(bob));
        log.info("[saveReplace]  id=2 整行覆盖, rows={}", rows);
        log.info("[saveReplace]  覆盖后: {}", userService.get(UserDo::getId, 2L).getName());
    }

    private UserDo newUser(Long id, String name, Integer age, Integer salary, Long deptId) {
        UserDo u = new UserDo();
        u.setId(id);
        u.setName(name);
        u.setAge(age);
        u.setSalary(salary);
        u.setDeptId(deptId);
        // REPLACE INTO / INSERT IGNORE 是整行重插，需显式给出逻辑删除字段，
        // 否则该列写入 NULL，会被后续 deleted=0 的查询过滤掉。
        u.setDeleted(0);
        return u;
    }
}
