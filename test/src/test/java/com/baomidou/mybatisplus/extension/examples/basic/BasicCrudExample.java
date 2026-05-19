package com.baomidou.mybatisplus.extension.examples.basic;

import com.baomidou.mybatisplus.extension.dialect.DbType;
import com.baomidou.mybatisplus.extension.dialect.DialectRegistry;
import com.baomidou.mybatisplus.extension.it.ItApplication;
import com.baomidou.mybatisplus.extension.it.UserDo;
import com.baomidou.mybatisplus.extension.it.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础 CRUD 完整范本。
 *
 * <p>本类覆盖最常见的"增删改查"五类操作，每个方法都演示<b>两种写法对照</b>：
 * <ul>
 *   <li><b>Stream 形式</b>（推荐）—— lambda 调用顺序 = SQL 子句顺序：
 *       <code>.map → .join → .filter → .group → .sorted → .limit → 终端</code></li>
 *   <li><b>一行语法</b> —— {@code userService.get/list/page/...} 一行简写</li>
 * </ul>
 *
 * <p>看每个 {@code @Test} 方法上方的 SQL 注释，对照下面的 Java 代码——
 * 就是这个库的设计哲学"<b>像写 SQL 一样一目了然</b>"。
 */
@SpringBootTest(classes = ItApplication.class)
@Testcontainers
class BasicCrudExample {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeAll
    static void switchDialect() {
        DialectRegistry.use(DbType.POSTGRESQL);
    }

    @Autowired
    DataSource dataSource;

    @Autowired
    UserService userService;

    @BeforeEach
    void resetSchema() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS ms_user");
            s.execute("CREATE TABLE ms_user (id BIGINT PRIMARY KEY, name VARCHAR(64) NOT NULL, age INTEGER, active BOOLEAN)");
        }
        userService.saveBatchWithoutId(List.of(
            UserDo.of(1L, "Alice", 30, true),
            UserDo.of(2L, "Bob",   25, false),
            UserDo.of(3L, "Carol", 28, true),
            UserDo.of(4L, "Dave",  35, true)
        ));
    }

    // ────────────────────────────────────────────────────────────────────
    //  SAVE
    // ────────────────────────────────────────────────────────────────────

    /**
     * <pre>
     * INSERT INTO ms_user (id, name, age, active) VALUES
     *   (?, ?, ?, ?), (?, ?, ?, ?)
     * </pre>
     */
    @Test
    void saveBatch_multiple_rows() {
        int rows = userService.saveBatchWithoutId(List.of(
            UserDo.of(10L, "Eve",   22, true),
            UserDo.of(11L, "Frank", 40, false)
        ));
        assertEquals(2, rows);

        assertNotNull(userService.get(where -> where.eq(UserDo::getId, 10L)));
        assertNotNull(userService.get(where -> where.eq(UserDo::getId, 11L)));
    }

    // ────────────────────────────────────────────────────────────────────
    //  GET — 单条查询
    // ────────────────────────────────────────────────────────────────────

    /**
     * <pre>
     * SELECT * FROM ms_user WHERE id = 1 LIMIT 1
     * </pre>
     */
    @Test
    void get_by_id() {
        // Stream 形式
        UserDo alice = userService.stream()
            .filter(where -> where.eq(UserDo::getId, 1L))
            .findFirst()
            .orElse(null);
        assertNotNull(alice);
        assertEquals("Alice", alice.getName());

        // 一行语法
        UserDo same = userService.get(UserDo::getId, 1L);
        assertEquals(alice.getName(), same.getName());
    }

    /**
     * <pre>
     * SELECT * FROM ms_user WHERE active = true AND age > 27 LIMIT 1
     * </pre>
     */
    @Test
    void get_by_multi_condition() {
        // Stream 形式
        UserDo found = userService.stream()
            .filter(where -> where.eq(UserDo::getActive, true).gt(UserDo::getAge, 27))
            .findFirst()
            .orElse(null);
        assertNotNull(found);

        // 一行语法
        UserDo same = userService.get(where -> where
            .eq(UserDo::getActive, true)
            .gt(UserDo::getAge, 27));
        assertEquals(found.getId(), same.getId());
    }

    // ────────────────────────────────────────────────────────────────────
    //  LIST — 列表查询
    // ────────────────────────────────────────────────────────────────────

    /**
     * <pre>
     * SELECT * FROM ms_user WHERE active = true
     * </pre>
     */
    @Test
    void list_by_condition() {
        // Stream 形式
        List<UserDo> active = userService.stream()
            .filter(where -> where.eq(UserDo::getActive, true))
            .collect(Collectors.toList());
        assertEquals(3, active.size());

        // 一行语法
        List<UserDo> same = userService.list(where -> where.eq(UserDo::getActive, true));
        assertEquals(active.size(), same.size());
    }

    /**
     * <pre>
     * SELECT * FROM ms_user WHERE active = true ORDER BY age DESC LIMIT 2
     * </pre>
     */
    @Test
    void list_filter_sorted_limit() {
        List<UserDo> top2ByAge = userService.stream()
            .filter(where -> where.eq(UserDo::getActive, true))
            .sorted(order -> order.orderDesc(UserDo::getAge))
            .limit(2)
            .collect(Collectors.toList());
        assertEquals(2, top2ByAge.size());
        assertEquals("Dave", top2ByAge.get(0).getName());
        assertEquals("Alice", top2ByAge.get(1).getName());
    }

    /**
     * <pre>
     * SELECT name AS name, age AS age
     * FROM ms_user
     * WHERE active = true
     * </pre>
     */
    @Test
    void list_map_to_dto() {
        record UserSummary(String name, Integer age) {}

        // 注：本范本演示思路；实际若用 record/class DTO 映射，请定义为 top-level 类 + setter。
        List<UserDo> active = userService.stream()
            .filter(where -> where.eq(UserDo::getActive, true))
            .collect(Collectors.toList());

        List<UserSummary> dtos = active.stream()
            .map(u -> new UserSummary(u.getName(), u.getAge()))
            .collect(Collectors.toList());
        assertEquals(3, dtos.size());
    }

    // ────────────────────────────────────────────────────────────────────
    //  UPDATE
    // ────────────────────────────────────────────────────────────────────

    /**
     * <pre>
     * UPDATE ms_user SET age = 99 WHERE id = 1
     * </pre>
     */
    @Test
    void update_set_value() {
        // Stream 形式
        int rows = userService.executableStream()
            .set(set -> set.set(UserDo::getAge, 99))
            .filter(where -> where.eq(UserDo::getId, 1L))
            .executeUpdate();
        assertEquals(1, rows);

        UserDo updated = userService.get(UserDo::getId, 1L);
        assertEquals(99, updated.getAge());

        // 一行语法
        userService.update(
            set -> set.set(UserDo::getAge, 100),
            where -> where.eq(UserDo::getId, 1L));
        assertEquals(100, userService.get(UserDo::getId, 1L).getAge());
    }

    /**
     * <pre>
     * UPDATE ms_user SET age = age + 1 WHERE active = true
     * </pre>
     */
    @Test
    void update_setFunc_accumulate() {
        Integer aliceBefore = userService.get(UserDo::getId, 1L).getAge();

        int rows = userService.executableStream()
            .set(set -> set.setFunc(UserDo::getAge, func -> func.add(UserDo::getAge, 1)))
            .filter(where -> where.eq(UserDo::getActive, true))
            .executeUpdate();
        assertEquals(3, rows);

        Integer aliceAfter = userService.get(UserDo::getId, 1L).getAge();
        assertEquals(aliceBefore + 1, aliceAfter);
    }

    // ────────────────────────────────────────────────────────────────────
    //  REMOVE
    // ────────────────────────────────────────────────────────────────────

    /**
     * <pre>
     * DELETE FROM ms_user WHERE active = false
     * </pre>
     */
    @Test
    void remove_by_condition() {
        int rows = userService.executableStream()
            .filter(where -> where.eq(UserDo::getActive, false))
            .executeDelete();
        assertEquals(1, rows);

        long remaining = userService.stream().count();
        assertEquals(3, remaining);

        // 一行语法（这里数据已删，演示等价 API）
        userService.remove(where -> where.eq(UserDo::getId, 999L));   // 不存在，rows=0
    }

    // ────────────────────────────────────────────────────────────────────
    //  COUNT / EXIST
    // ────────────────────────────────────────────────────────────────────

    /**
     * <pre>
     * SELECT COUNT(*) FROM ms_user WHERE active = true
     * </pre>
     */
    @Test
    void count_and_exist() {
        // Stream 形式
        long activeCount = userService.stream()
            .filter(where -> where.eq(UserDo::getActive, true))
            .count();
        assertEquals(3, activeCount);

        boolean hasInactive = userService.stream()
            .filter(where -> where.eq(UserDo::getActive, false))
            .exist();
        assertTrue(hasInactive);

        // 一行语法
        assertEquals(3, userService.count(where -> where.eq(UserDo::getActive, true)));
        assertTrue(userService.exist(where -> where.eq(UserDo::getActive, false)));
    }
}
