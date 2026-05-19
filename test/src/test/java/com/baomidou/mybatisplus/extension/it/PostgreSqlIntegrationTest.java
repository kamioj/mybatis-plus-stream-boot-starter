package com.baomidou.mybatisplus.extension.it;

import com.baomidou.mybatisplus.extension.dialect.DbType;
import com.baomidou.mybatisplus.extension.dialect.DialectRegistry;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PG 17 集成测试：验证 4.0/4.1 dialect SPI 适配在真实 PG 数据库上工作。
 *
 * <p>覆盖：基础 CRUD + saveDuplicate (ON CONFLICT) + saveIgnore + saveReplace
 * + stream filter / list + listJoin（暂略：测试 entity 简化）+ groupingBy + toMap +
 * BACKTICK token 化路径（隐含验证：任何 SQL 渲染都走 dialect 引号）。
 */
@SpringBootTest(classes = ItApplication.class)
@Testcontainers
class PostgreSqlIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17-alpine");

    /** 容器启动后再 lazy 解析 datasource URL/credentials；避开 @ServiceConnection 与 SpringContext 初始化时机问题 */
    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    DataSource dataSource;

    @Autowired
    UserService userService;

    @BeforeAll
    static void switchDialect() {
        DialectRegistry.use(DbType.POSTGRESQL);
    }

    @BeforeEach
    void resetSchema() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS ms_user");
            s.execute("CREATE TABLE ms_user (id BIGINT PRIMARY KEY, name VARCHAR(64) NOT NULL, age INTEGER, active BOOLEAN)");
        }
    }

    @Test
    void saveBatch_and_list() {
        int rows = userService.saveBatchWithoutId(Arrays.asList(
            UserDo.of(1L, "Alice", 30, true),
            UserDo.of(2L, "Bob",   25, false),
            UserDo.of(3L, "Carol", 28, true)
        ));
        assertEquals(3, rows);

        List<UserDo> all = userService.list(w -> w.eq(UserDo::getActive, true));
        assertEquals(2, all.size());
    }

    @Test
    void stream_filter_collect() {
        userService.saveBatchWithoutId(Arrays.asList(
            UserDo.of(1L, "A", 20, true),
            UserDo.of(2L, "B", 30, true),
            UserDo.of(3L, "C", 40, false)
        ));

        Set<Long> ids = userService.stream()
            .filter(w -> w.eq(UserDo::getActive, true))
            .toSet(UserDo::getId);
        assertTrue(ids.containsAll(Arrays.asList(1L, 2L)));
        assertEquals(2, ids.size());
    }

    @Test
    void toMap_pushes_select_two_columns() {
        userService.saveBatchWithoutId(Arrays.asList(
            UserDo.of(1L, "Alice", 30, true),
            UserDo.of(2L, "Bob",   25, true)
        ));
        Map<Long, String> idToName = userService.stream()
            .filter(w -> w.eq(UserDo::getActive, true))
            .toMap(UserDo::getId, UserDo::getName);
        assertEquals("Alice", idToName.get(1L));
        assertEquals("Bob",   idToName.get(2L));
    }

    @Test
    void toMapCount_pushes_group_by_to_sql() {
        userService.saveBatchWithoutId(Arrays.asList(
            UserDo.of(1L, "A", 20, true),
            UserDo.of(2L, "B", 30, true),
            UserDo.of(3L, "C", 40, false)
        ));
        Map<Boolean, Long> byActive = userService.stream().toMapCount(UserDo::getActive);
        assertEquals(2L, byActive.get(true));
        assertEquals(1L, byActive.get(false));
    }

    @Test
    void saveDuplicate_pg_uses_on_conflict() {
        userService.saveBatchWithoutId(List.of(UserDo.of(1L, "Alice", 30, true)));

        int rows = userService.saveDuplicate(
            List.of(UserDo.of(1L, "AliceUpdated", 31, true)),
            set -> set.set(UserDo::getName, "AliceUpdated").set(UserDo::getAge, 31)
        );
        assertTrue(rows >= 1);

        UserDo got = userService.get(w -> w.eq(UserDo::getId, 1L));
        assertEquals("AliceUpdated", got.getName());
        assertEquals(31, got.getAge());
    }

    @Test
    void saveIgnore_pg_uses_on_conflict_do_nothing() {
        userService.saveBatchWithoutId(List.of(UserDo.of(1L, "Alice", 30, true)));

        int rows = userService.saveIgnore(List.of(UserDo.of(1L, "ShouldBeIgnored", 99, false)));
        // PG ON CONFLICT DO NOTHING 不影响已存在行（返回 0 表示无行受影响）
        assertEquals(0, rows);

        UserDo got = userService.get(w -> w.eq(UserDo::getId, 1L));
        assertEquals("Alice", got.getName());
        assertEquals(30, got.getAge());
    }

    @Test
    void saveReplace_pg_uses_on_conflict_overwrite_all() {
        userService.saveBatchWithoutId(List.of(UserDo.of(1L, "Alice", 30, true)));

        int rows = userService.saveReplace(List.of(UserDo.of(1L, "Replaced", 99, false)));
        assertTrue(rows >= 1);

        UserDo got = userService.get(w -> w.eq(UserDo::getId, 1L));
        assertEquals("Replaced", got.getName());
        assertEquals(99, got.getAge());
        assertFalse(got.getActive());
    }
}
