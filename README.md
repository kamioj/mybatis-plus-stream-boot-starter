<div align="center">

# MyBatis-Plus Stream Boot Starter

**让数据库操作像写 Java Stream 一样优雅**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kamioj/mybatis-plus-stream-boot-starter)](https://central.sonatype.com/artifact/io.github.kamioj/mybatis-plus-stream-boot-starter)
[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![JDK](https://img.shields.io/badge/JDK-17+-green.svg)](https://adoptium.net)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.9-blue.svg)](https://baomidou.com)

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
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.kamioj:mybatis-plus-stream-boot-starter:1.0.0'
```

> **环境要求**：JDK 17+、Spring Boot 3.x、MyBatis-Plus 3.5.9（自动引入）

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

```
src/main/java/com/baomidou/mybatisplus/extension/
├── service/
│   ├── IMysqlServiceBase.java          # 核心 Service 接口（60+ 方法）
│   └── impl/
│       └── MysqlServiceBaseImpl.java   # Service 实现
├── MysqlBaseMapper.java                # 增强 Mapper 接口
├── MybatisQueryableStream*.java        # 流式查询 API
├── MybatisExecutableStream.java        # 更新流 API
├── JoinLambdaQueryWrapper.java         # 连表条件构造器
├── GroupLambdaQueryWrapper.java        # 分组条件构造器
├── GroupFunctionLambdaQueryWrapper.java # 聚合函数构造器
├── SelectLambdaQueryWrapper.java       # 字段选择构造器
├── AbstractWhereLambdaQueryWrapper.java # WHERE 条件构造器
├── LambdaOrderItem.java                # 排序构造器
└── ...                                 # 其他辅助类
```

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
