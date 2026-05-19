# mybatis-plus-stream-test

**可运行的使用范本。本模块永远不发布到 Maven Central。**

## 目的

让你看到"实际项目里怎么用本框架"——每个 example 类聚焦一类场景，全部基于 **真实 PG 容器**（testcontainers + `postgres:17-alpine`）端到端跑通。

阅读建议：先看 `examples/basic/BasicCrudExample.java`，按 SQL 子句顺序排列了 CRUD 全部写法 + 对应 SQL。

## 目录结构

```
test/
├── pom.xml                          ← 引用 starter dep + spring boot test + testcontainers
├── src/test/java/com/baomidou/mybatisplus/extension/
│   ├── examples/
│   │   └── basic/
│   │       └── BasicCrudExample.java  ← 入门：完整 CRUD 范本
│   └── it/                          ← 共享基础设施
│       ├── ItApplication.java       ← Spring Boot test 启动类
│       ├── UserDo.java              ← 示例 entity
│       ├── UserMapper.java          ← StreamBaseMapper 范本
│       ├── UserService.java         ← IStreamService 范本
│       └── PostgreSqlIntegrationTest.java  ← PG 端到端集成测试
└── src/test/resources/
    ├── application-it.yml
    └── schema-pg.sql
```

## 怎么跑

需要 Docker（testcontainers 会拉起 `postgres:17-alpine`）。

```powershell
# 所有 example
mvn -pl test test

# 单一 example
mvn -pl test test -Dtest=BasicCrudExample
```

## 怎么扩展

新增 example 类的模板：

```java
@SpringBootTest(classes = ItApplication.class)
@Testcontainers
class YourExample {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeAll
    static void switchDialect() { DialectRegistry.use(DbType.POSTGRESQL); }

    @BeforeEach
    void resetSchema() throws Exception {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS ms_user");
            s.execute("CREATE TABLE ms_user (id BIGINT PRIMARY KEY, name VARCHAR(64) NOT NULL, age INTEGER, active BOOLEAN)");
        }
    }

    @Autowired DataSource dataSource;
    @Autowired UserService userService;

    @Test
    void your_scenario() {
        // 1) prepare data
        userService.saveBatchWithoutId(...);

        // 2) exercise the API
        var result = userService.stream()
            // .map(...)  ← SELECT
            // .join(...) ← FROM JOIN
            // .filter(...) ← WHERE
            // .collect(...)
            ;

        // 3) assert
        assertEquals(..., result);
    }
}
```

## 计划补的 example（按场景）

- `examples/basic/` ✅ `BasicCrudExample` —— get / list / save / update / remove
- `examples/stream/` 🚧 stream 收集器（toMap / toGroupMap / toMapCount / toMapSum / toMapAvg）
- `examples/join/` 🚧 listJoin / pageJoin / 衍生表
- `examples/subquery/` 🚧 IN / EXISTS / SELECT 标量
- `examples/write/` 🚧 saveDuplicate / saveIgnore / saveReplace 三方言对照
- `examples/lock/` 🚧 LockMode（FOR UPDATE NOWAIT / WAIT n）
