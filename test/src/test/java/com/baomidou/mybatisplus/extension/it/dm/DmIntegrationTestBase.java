package com.baomidou.mybatisplus.extension.it.dm;

import com.baomidou.mybatisplus.extension.dialect.DbType;
import com.baomidou.mybatisplus.extension.dialect.DialectRegistry;
import com.baomidou.mybatisplus.extension.it.ItApplication;
import com.baomidou.mybatisplus.extension.it.mysql.CapturedSql;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlDemandDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlDemandService;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlOrderDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlOrderService;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest(classes = ItApplication.class)
abstract class DmIntegrationTestBase {

    static final LocalDateTime T0 = LocalDateTime.of(2026, 5, 20, 9, 0);

    @Autowired
    DataSource dataSource;

    @Autowired
    MysqlUserService userService;

    @Autowired
    MysqlOrderService orderService;

    @Autowired
    MysqlDemandService demandService;

    @DynamicPropertySource
    static void registerDmDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> property("mps.dm.url", "jdbc:dm://127.0.0.1:5236"));
        registry.add("spring.datasource.username", () -> property("mps.dm.username", "SYSDBA"));
        // 密码不写死在代码里：由系统属性 mps.dm.password 或环境变量 MPS_DM_PASSWORD 注入，见仓库根 .env.example
        registry.add("spring.datasource.password", () -> property("mps.dm.password", ""));
        registry.add("spring.datasource.driver-class-name", () -> "dm.jdbc.driver.DmDriver");
    }

    @BeforeAll
    static void switchDialect() {
        DialectRegistry.use(DbType.DAMENG);
    }

    @BeforeEach
    void resetSchemaAndData() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            dropIfExists(statement, "mps_demand");
            dropIfExists(statement, "mps_order");
            dropIfExists(statement, "mps_user");
            statement.execute("""
                CREATE TABLE "mps_user" (
                    "id" BIGINT PRIMARY KEY,
                    "username" VARCHAR(64) NOT NULL,
                    "role_code" VARCHAR(32) NOT NULL,
                    "age" INT NULL,
                    "credit_score" INT NULL,
                    "balance" DECIMAL(12,2) NULL,
                    "active" TINYINT NOT NULL,
                    "tags" VARCHAR(128) NULL,
                    "manager_id" BIGINT NULL,
                    "created_at" TIMESTAMP NOT NULL,
                    "deleted" TINYINT DEFAULT 0 NOT NULL
                )
                """);
            statement.execute("CREATE UNIQUE INDEX \"uk_mps_user_username\" ON \"mps_user\" (\"username\")");
            statement.execute("CREATE INDEX \"idx_mps_user_role\" ON \"mps_user\" (\"role_code\")");
            statement.execute("""
                CREATE TABLE "mps_order" (
                    "id" BIGINT PRIMARY KEY,
                    "user_id" BIGINT NOT NULL,
                    "amount" DECIMAL(12,2) NOT NULL,
                    "status" VARCHAR(32) NOT NULL,
                    "created_at" TIMESTAMP NOT NULL
                )
                """);
            statement.execute("CREATE INDEX \"idx_mps_order_user\" ON \"mps_order\" (\"user_id\")");
            statement.execute("""
                CREATE TABLE "mps_demand" (
                    "id" BIGINT PRIMARY KEY,
                    "user_id" BIGINT NOT NULL,
                    "title" VARCHAR(128) NOT NULL,
                    "status" VARCHAR(32) NOT NULL
                )
                """);
            statement.execute("CREATE INDEX \"idx_mps_demand_user\" ON \"mps_demand\" (\"user_id\")");
        }

        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(1L, "Alice", "admin", 30, 98, "1200.50", true, "java,dm,admin", null,
                T0.minusDays(5), 0),
            MysqlUserDo.of(2L, "Bob", "user", 25, 72, "300.00", true, "java,ops", 1L,
                T0.minusDays(4), 0),
            MysqlUserDo.of(3L, "Carol", "user", 28, 88, "780.25", false, "dm,report", 1L,
                T0.minusDays(3), 0),
            MysqlUserDo.of(4L, "Dave", "auditor", 35, 65, "90.00", true, "audit,ops", 1L,
                T0.minusDays(2), 0),
            MysqlUserDo.of(5L, "Eve", "user", 41, 45, "0.00", false, null, 2L,
                T0.minusDays(1), 1)
        ));
        orderService.saveBatchWithoutId(List.of(
            MysqlOrderDo.of(101L, 1L, "100.00", "paid", T0.minusDays(3)),
            MysqlOrderDo.of(102L, 1L, "60.00", "paid", T0.minusDays(2)),
            MysqlOrderDo.of(103L, 2L, "35.50", "new", T0.minusDays(1)),
            MysqlOrderDo.of(104L, 3L, "220.00", "paid", T0.minusHours(12)),
            MysqlOrderDo.of(105L, 4L, "15.00", "cancelled", T0.minusHours(6))
        ));
        demandService.saveBatchWithoutId(List.of(
            MysqlDemandDo.of(201L, 1L, "Admin dashboard", "open"),
            MysqlDemandDo.of(202L, 1L, "Audit export", "closed"),
            MysqlDemandDo.of(203L, 2L, "User import", "open"),
            MysqlDemandDo.of(204L, 3L, "Report chart", "open"),
            MysqlDemandDo.of(205L, 4L, "Audit review", "closed")
        ));
        CapturedSql.clear();
    }

    static void assertSqlContains(String expected) {
        String sql = comparableSql(CapturedSql.joined());
        String needle = comparableSql(expected);
        org.junit.jupiter.api.Assertions.assertTrue(sql.contains(needle), () -> CapturedSql.joined());
    }

    /** 断言归一化后的捕获 SQL 中不包含 keyword（与 assertSqlContains 用相同的 comparableSql 归一化）。 */
    static void assertSqlNotContains(String keyword) {
        String sql = comparableSql(CapturedSql.joined());
        String needle = comparableSql(keyword);
        org.junit.jupiter.api.Assertions.assertFalse(sql.contains(needle),
                () -> "SQL 不应包含 \"" + keyword + "\"，实际 SQL：\n" + CapturedSql.joined());
    }

    private static void dropIfExists(Statement statement, String table) throws Exception {
        try {
            statement.execute("DROP TABLE \"" + table + "\"");
        } catch (Exception ignored) {
        }
    }

    private static String comparableSql(String sql) {
        return sql.toLowerCase().replace("`", "").replace("\"", "").replaceAll("\\s+", " ").trim();
    }

    private static String property(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        return envValue == null || envValue.isBlank() ? defaultValue : envValue;
    }
}
