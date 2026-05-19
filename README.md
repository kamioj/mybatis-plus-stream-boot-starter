<div align="center">

# MyBatis-Plus Stream Boot Starter

**让数据库操作像写 Java Stream 一样优雅**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kamioj/mybatis-plus-stream-boot-starter)](https://central.sonatype.com/artifact/io.github.kamioj/mybatis-plus-stream-boot-starter)
[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![JDK](https://img.shields.io/badge/JDK-17+-green.svg)](https://adoptium.net)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.16-blue.svg)](https://baomidou.com)

[文档](https://kamioj.github.io/mybatis-plus-stream-docs/) | [国内镜像](https://mybatis-plus-stream-docs.545329844.workers.dev/) | [快速开始](#快速开始) | [核心功能](#核心功能) | [示例](#使用示例)

</div>

---

## 介绍

MyBatis-Plus Stream 是一个基于 [MyBatis-Plus](https://baomidou.com) 的增强框架，将 **Java Stream 编程风格**引入数据库操作。通过 Lambda 类型安全的链式 API，你可以用 `stream().filter().sorted().limit().collect()` 的方式完成从简单查询到多表联查、分组聚合的各种数据库操作，**告别手写 SQL**。

### 核心特性

| 特性 | 说明 |
|------|------|
| 🚀 **流式查询** | `stream().filter().sorted().limit().collect()` 链式调用 |
| 🔗 **连表查询** | 内置 LEFT / RIGHT / INNER / CROSS JOIN，Lambda 类型安全 |
| 📊 **聚合函数** | 100+ SQL 函数（COUNT、SUM、AVG、字符串、日期、数学等） |
| 📄 **分页查询** | 单表分页、连表分页、分组分页，一行搞定 |
| 🛡️ **逻辑删除** | `withDeleted()` 一键切换查询模式 |
| ✏️ **批量写入** | `saveBatchWithoutId` / `saveDuplicate` / `saveIgnore` / `saveReplace` |
| 🔄 **联合更新** | 支持多表 JOIN UPDATE |
| 🎯 **零侵入** | 继承 `IMysqlServiceBase` 即用，无需修改现有代码 |

---

## 安装

### Maven

```xml
<dependency>
    <groupId>io.github.kamioj</groupId>
    <artifactId>mybatis-plus-stream-boot-starter</artifactId>
    <version>4.1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.kamioj:mybatis-plus-stream-boot-starter:4.1.0.0'
```

> **环境要求**：JDK 17+、Spring Boot 3.x、MyBatis-Plus 3.5.16（自动引入）

---

## 快速开始

### 1. 修改 Mapper

```java
// 将 BaseMapper 替换为 MysqlBaseMapper
public interface UserMapper extends MysqlBaseMapper<User> {
}
```

### 2. 修改 Service

```java
// 接口继承 IMysqlServiceBase
public interface UserService extends IMysqlServiceBase<User> {
}

// 实现类继承 MysqlServiceBaseImpl
@Service
public class UserServiceImpl extends MysqlServiceBaseImpl<UserMapper, User> implements UserService {
}
```

### 3. 开始使用

```java
// 流式查询
List<User> users = userService.stream()
    .filter(where -> where.eq(User::getRole, "admin"))
    .sorted(order -> order.orderDesc(User::getCreateTime))
    .limit(10)
    .collect(Collectors.toList());
```

**就这么简单！** 更多用法请查看下方核心功能。

---

## 核心功能

### 查询（Get）

```java
// 按条件获取单个实体
User user = userService.get(where -> where.eq(User::getId, 1));

// 按列获取
User user = userService.get(User::getId, 1);

// 获取或返回默认值
User user = userService.getOrDefault(where -> where.eq(User::getId, 1), new User());

// 行锁查询（SELECT ... FOR UPDATE）
User user = userService.getByKeyForUpdate(1);
```

### 取值（GetValue）

```java
// 获取单个字段值
String name = userService.getValue(where -> where.eq(User::getId, 1), User::getUsername);

// 通过聚合函数取值
Integer total = userService.getValue(
    where -> where.eq(User::getStatus, 1),
    func -> func.count()
);
```

### 列表（List）

```java
// 条件查询
List<User> users = userService.list(where -> where.eq(User::getRole, "user"));

// 条件 + 排序 + 限制
List<User> users = userService.list(
    where -> where.eq(User::getStatus, 1),
    order -> order.orderDesc(User::getCreateTime),
    10
);

// 映射到 DTO
List<UserVO> vos = userService.list(
    where -> where.eq(User::getRole, "user"),
    select -> select
        .select(User::getId, UserVO::getId)
        .select(User::getUsername, UserVO::getUsername),
    UserVO.class
);
```

### 列表取值（ListValues）

```java
// 获取某列的值集合
List<String> names = userService.listValues(
    where -> where.eq(User::getStatus, 1),
    User::getUsername
);

// 获取聚合函数值集合
List<Integer> counts = userService.listValues(
    where -> where.eq(User::getStatus, 1),
    func -> func.count()
);
```

### 连表查询（Join）

```java
// LEFT JOIN 查询
List<User> users = userService.listJoin(
    join -> join.leftJoin(Order.class, User::getId, Order::getUserId),
    where -> where.gt(Order::getAmount, 100)
);

// JOIN + 映射到 DTO
List<UserOrderVO> vos = userService.listJoin(
    join -> join.leftJoin(Order.class, User::getId, Order::getUserId),
    where -> where.eq(User::getStatus, 1),
    select -> select
        .select(User::getUsername, UserOrderVO::getUsername)
        .select(Order::getOrderNo, UserOrderVO::getOrderNo),
    UserOrderVO.class
);
```

### 分组查询（Group）

```java
// 分组 + 聚合
List<UserStatVO> stats = userService.listGroup(
    group -> group.groupBy(User::getDeptId),
    where -> where.eq(User::getStatus, 1),
    select -> select
        .select(User::getDeptId, UserStatVO::getDeptId)
        .selectFunc(func -> func.count(), UserStatVO::getUserCount),
    UserStatVO.class
);
```

### 连表 + 分组（GroupJoin）

```java
// JOIN + GROUP BY + 聚合，一行搞定
List<DeptOrderVO> result = userService.listGroupJoin(
    join -> join.leftJoin(Order.class, User::getId, Order::getUserId),
    group -> group.groupBy(User::getDeptId),
    where -> where.eq(User::getStatus, 1),
    select -> select
        .select(User::getDeptId, DeptOrderVO::getDeptId)
        .selectFunc(func -> func.sum(Order::getAmount), DeptOrderVO::getTotalAmount)
        .selectFunc(func -> func.count(), DeptOrderVO::getOrderCount),
    DeptOrderVO.class
);
```

### 分页查询（Page）

```java
// 单表分页
IPage<User> page = userService.page(
    new Page<>(1, 10),
    where -> where.eq(User::getStatus, 1)
);

// 连表分页
IPage<UserVO> page = userService.pageJoin(
    new Page<>(1, 10),
    join -> join.leftJoin(Dept.class, User::getDeptId, Dept::getId),
    where -> where.eq(User::getStatus, 1),
    select -> select
        .select(User::getUsername, UserVO::getUsername)
        .select(Dept::getDeptName, UserVO::getDeptName),
    UserVO.class
);

// 分组分页
IPage<StatVO> page = userService.pageGroupJoin(
    new Page<>(1, 10),
    join -> join.leftJoin(Order.class, User::getId, Order::getUserId),
    group -> group.groupBy(User::getId),
    where -> where.eq(User::getStatus, 1),
    select -> select
        .select(User::getUsername, StatVO::getUsername)
        .selectFunc(func -> func.count(), StatVO::getOrderCount),
    StatVO.class
);
```

### 流式 API（Stream）

```java
// 完整的 Stream 风格链式调用
Optional<User> user = userService.stream()
    .filter(where -> where.eq(User::getRole, "admin"))
    .sorted(order -> order.orderDesc(User::getId))
    .findFirst();

// 映射到 VO
Optional<UserVO> vo = userService.stream()
    .map(select -> select
            .select(User::getId, UserVO::getId)
            .select(User::getUsername, UserVO::getUsername),
        UserVO.class)
    .join(join -> join.innerJoin(Dept.class,
        on -> on.eqColumn(User::getDeptId, Dept::getId)))
    .filter(where -> where.eq(User::getStatus, 1))
    .findFirst();

// 查询单列
Optional<String> name = userService.stream()
    .mapToColumn(User::getUsername)
    .filter(where -> where.eq(User::getId, 1))
    .findFirst();
```

### 写入操作

```java
// 批量插入（不回填 ID）
int rows = userService.saveBatchWithoutId(userList);

// 去重插入（ON DUPLICATE KEY UPDATE）
int rows = userService.saveDuplicate(userList,
    set -> set.set(User::getUpdateTime, LocalDateTime.now()));

// 忽略插入（INSERT IGNORE）
int rows = userService.saveIgnore(userList);

// 替换插入（REPLACE INTO）
int rows = userService.saveReplace(userList);
```

### 更新与删除

```java
// 条件更新
int rows = userService.update(
    set -> set.set(User::getStatus, 0),
    where -> where.eq(User::getId, 1)
);

// 多表联合更新（JOIN UPDATE）
int rows = userService.updateJoin(
    join -> join.innerJoin(Dept.class, User::getDeptId, Dept::getId),
    set -> set.set(User::getStatus, 0),
    where -> where.eq(Dept::getDeptName, "已解散部门")
);

// 条件删除
int rows = userService.remove(where -> where.eq(User::getStatus, 0));
```

### 其他

```java
// 判断是否存在
boolean exists = userService.exist(where -> where.eq(User::getUsername, "admin"));

// 条件计数
int count = userService.count(where -> where.eq(User::getStatus, 1));

// 更新流（批量字段更新）
userService.executableStream()
    .set(set -> set.set(User::getStatus, 0))
    .where(where -> where.lt(User::getExpireTime, LocalDateTime.now()))
    .execute();
```

---

## 使用示例

<details>
<summary><b>示例 1：动态条件查询</b></summary>

```java
public List<User> searchUsers(String keyword, Integer status, String role) {
    return userService.list(where -> {
        if (keyword != null) {
            where.like(User::getUsername, keyword);
        }
        if (status != null) {
            where.eq(User::getStatus, status);
        }
        if (role != null) {
            where.eq(User::getRole, role);
        }
    });
}
```
</details>

<details>
<summary><b>示例 2：多表统计报表</b></summary>

```java
public List<DeptReportVO> getDeptReport() {
    return userService.listGroupJoin(
        join -> join.leftJoin(Order.class, User::getId, Order::getUserId),
        group -> group.groupBy(User::getDeptId),
        where -> where.eq(User::getStatus, 1),
        select -> select
            .select(User::getDeptId, DeptReportVO::getDeptId)
            .selectFunc(func -> func.count(User::getId), DeptReportVO::getUserCount)
            .selectFunc(func -> func.sum(Order::getAmount), DeptReportVO::getTotalAmount)
            .selectFunc(func -> func.avg(Order::getAmount), DeptReportVO::getAvgAmount),
        DeptReportVO.class
    );
}
```
</details>

<details>
<summary><b>示例 3：流式数据处理</b></summary>

```java
// 像操作 Java Stream 一样操作数据库
Map<String, List<User>> usersByRole = userService.stream()
    .filter(where -> where.eq(User::getStatus, 1))
    .sorted(order -> order.orderAsc(User::getUsername))
    .collect(Collectors.groupingBy(User::getRole));
```
</details>

---

## 项目结构

> **4.0.0.0 起** 按职责拆分子包 + 引入方言 SPI；之前用 3.5.x 的请看 [MIGRATION-4.0.md](MIGRATION-4.0.md) 一键迁移。

```
src/main/java/com/baomidou/mybatisplus/extension/
├── mapper/       StreamBaseMapper           # 增强 Mapper 入口（4.0 起；旧名 MysqlBaseMapper @Deprecated）
├── service/      IStreamService + impl/     # 核心 Service 接口（60+ 方法）
├── dialect/      SqlDialect SPI             # 4.0 起：方言扩展（MySQL/PG/DM/自定义）
│   ├── SqlDialect / DbType / LockMode / DialectRegistry
│   └── impl/{MySql,PostgreSql,Dameng}Dialect # 4.0.1 起内置三种主流方言
├── core/         (4 类)                     # 查询执行内核：ExQueryWrapper 等
├── wrapper/      (26 类)                    # Wrapper 家族：按 "上下文 × 角色" 网格命名
│   ├── Abstract* / Normal* / Group* / Duplicate* / Sub*  ← 上下文前缀
│   └── *Where / *Select / *Set / *Function / *Case        ← 角色后缀
├── stream/       (9 类)                     # 流式 API：MybatisQueryableStream{,1..5,Many}
├── value/        (7 类)                     # 单列投影值容器：Single*Value, NonValue
├── metadata/     (6 类)                     # 表/列/类型元数据（含 SqlDataType，旧名 MysqlDataType）
├── support/      (2 类)                     # 工具：StringUtils, LambdaOrderItem
└── bo/
    ├── key/         BiMapKey, MapKey3..5          # 多列组合键（生产高频）
    └── (root)       PageVo, SortVo, BiList        # 通用容器
```

每个子包都附带 `package-info.java` 描述其职责，IDE 与 JavaDoc 可直接浏览。

### 设计原则

- **不依赖第三方服务**：本库自身不引入 JDBC 驱动 / 缓存 / 消息队列；方言以 SPI 形式扩展
- **SQL 标准结构可见**：链式 API 一一对应 SQL 关键字（`.filter`→`WHERE`、`.sorted`→`ORDER BY`、`.exists`→`EXISTS`、`.forUpdateSkipLocked`→`FOR UPDATE SKIP LOCKED`），不抹除 SQL
- **零反射热点**：SFunction / TableInfo 全部走 `ClassValue` 缓存（首次后命中率 &gt; 99%）

## 文档

| 站点 | 地址 |
|------|------|
| 📖 GitHub Pages | **https://kamioj.github.io/mybatis-plus-stream-docs/** |
| 🚀 国内镜像 (Cloudflare) | **https://mybatis-plus-stream-docs.545329844.workers.dev/** |

- [项目介绍](https://kamioj.github.io/mybatis-plus-stream-docs/pages/quickstart/introduce)
- [安装指南](https://kamioj.github.io/mybatis-plus-stream-docs/pages/quickstart/install)
- [快速开始](https://kamioj.github.io/mybatis-plus-stream-docs/pages/quickstart/quickstart)
- [实战案例](https://kamioj.github.io/mybatis-plus-stream-docs/pages/examples/dynamic-query)
- [常见问题](https://kamioj.github.io/mybatis-plus-stream-docs/pages/reference/faq)

## 许可证

[GNU Affero General Public License v3.0](LICENSE)
