# Quickstart Example

可运行示例，演示 `mybatis-plus-stream` 的核心 API。启动后观察控制台，每条日志上方都有对应 SQL 注释，帮助你建立"链式 API → SQL"的直觉。

## 前置条件

- JDK 17+
- 本地 MySQL 8（默认连 `localhost:3306`，首次启动自动建库 `mps_quickstart` 并建表）

## 运行

数据库账号密码通过环境变量注入，**不在仓库中硬编码**。运行前先设置：

```bash
# 从仓库根目录进入
cd examples/quickstart

# 设置数据库账号密码（按你的本地 MySQL 调整）
export MYSQL_USER=root
export MYSQL_PASSWORD=your_password      # Windows PowerShell: $env:MYSQL_PASSWORD='your_password'

# 启动
mvn spring-boot:run
```

> 也可在 `examples/quickstart/src/main/resources/` 下新建 `application-local.yml` 覆盖连接配置——该文件名已被 `.gitignore` 忽略，不会误提交。

## 演示的 API

### 基础篇 — `ApiShowcase`

| Demo | 方法 | 对应 SQL 语义 |
|------|------|--------------|
| 1 | `get(id)` / `get(where)` | `SELECT ... WHERE id=? LIMIT 1` |
| 2 | `list(where, order, limit, offset)` | `SELECT ... ORDER BY age DESC LIMIT 2` |
| 3 | `stream().filter().sorted().map().collect()` | Stream pipeline |
| 4 | `stream().count()` / `exist(where)` | `COUNT(*)` / `EXISTS` |
| 5 | `executableStream().set().filter().executeUpdate()` | `UPDATE ... SET age=age+1` |
| 6 | `saveBatchWithoutId(list)` | `INSERT ... VALUES (...),(...)` |

### 进阶篇 — 各功能域独立 showcase 类

| Showcase 类 | 演示 API | 能力 |
|-------------|---------|------|
| `JoinShowcase` | `listJoin` | 联表查询（返回实体 / 投影 DTO） |
| `GroupShowcase` | `listGroup` | 分组聚合（COUNT / AVG） |
| `PageShowcase` | `page` / `pageJoin` / `pageGroup` | 分页（单表 / 联表 / 分组） |
| `ConflictWriteShowcase` | `saveDuplicate` / `saveIgnore` / `saveReplace` | 主键冲突的三种写入策略 |
| `ProjectionShowcase` | `listValues` | 单列取值（标量列表投影） |
| `StreamTerminalShowcase` | `toMap` / `toMapCount` / `toMapSum` / `toMapAvg` / `groupingBy` / `forUpdate` | 流式聚合终端与行锁 |
| `WriteShowcase` | `update` / `updateJoin` / `remove` | 更新（单表 / 联表）与逻辑删除 |
| `SingleRowShowcase` | `getOrDefault` / `getValue` / `getByKeyForUpdate` | 单行读取（兜底 / 单值 / 锁行取） |

各 showcase 类用 `@Order` 控制控制台输出顺序，互不干扰。

## 项目结构

```
quickstart/
├── pom.xml
└── src/main/
    ├── java/io/github/kamioj/quickstart/
    │   ├── QuickstartApplication.java   # 启动类
    │   ├── demo/                        # 9 个 showcase 类（演示逻辑看这里）
    │   ├── dto/                         # UserDeptVo, DeptStatVo（投影 VO）
    │   ├── entity/                      # UserDo, DeptDo
    │   ├── mapper/                      # UserMapper, DeptMapper
    │   └── service/UserService.java
    └── resources/
        ├── application.yml              # 连接配置（账号密码用环境变量占位符）
        ├── schema.sql                   # 建表 DDL（DROP+CREATE，可重复运行）
        └── data.sql                     # 种子数据
```
