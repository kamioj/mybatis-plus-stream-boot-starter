<div align="center">

# MyBatis-Plus Stream Boot Starter

**Make database operations as elegant as writing Java Streams**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kamioj/mybatis-plus-stream-boot-starter)](https://central.sonatype.com/artifact/io.github.kamioj/mybatis-plus-stream-boot-starter)
[![License](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)
[![JDK](https://img.shields.io/badge/JDK-17+-green.svg)](https://adoptium.net)
[![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.16-blue.svg)](https://baomidou.com)

[Documentation](https://kamioj.github.io/mybatis-plus-stream-docs/) | [Mirror (CN)](https://mybatis-plus-stream-docs.545329844.workers.dev/) | [Quick Start](#quick-start) | [Features](#features) | [Examples](#usage-examples)

</div>

---

## Introduction

MyBatis-Plus Stream is an enhancement framework built on [MyBatis-Plus](https://baomidou.com) that brings the **Java Stream programming style** to database operations. With Lambda type-safe chained APIs, you can use `stream().filter().sorted().limit().collect()` to handle everything from simple queries to multi-table joins and group aggregations — **no more hand-written SQL**.

### Key Features

| Feature | Description |
|---------|-------------|
| 🚀 **Stream Query** | `stream().filter().sorted().limit().collect()` chained calls |
| 🔗 **Join Query** | Built-in LEFT / RIGHT / INNER / CROSS JOIN with Lambda type safety |
| 📊 **Aggregate Functions** | 100+ SQL functions (COUNT, SUM, AVG, string, date, math, etc.) |
| 📄 **Pagination** | Single-table, join, and group pagination in one line |
| 🛡️ **Soft Delete** | `withDeleted()` to toggle query mode |
| ✏️ **Batch Insert** | `saveBatchWithoutId` / `saveDuplicate` / `saveIgnore` / `saveReplace` |
| 🔄 **Join Update** | Multi-table JOIN UPDATE support |
| 🎯 **Zero Intrusion** | Just extend `IMysqlServiceBase`, no changes to existing code |

---

## Installation

### Maven

```xml
<dependency>
    <groupId>io.github.kamioj</groupId>
    <artifactId>mybatis-plus-stream-boot-starter</artifactId>
    <version>4.0.2.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.kamioj:mybatis-plus-stream-boot-starter:4.0.2.0'
```

> **Requirements**: JDK 17+, Spring Boot 3.x, MyBatis-Plus 3.5.16 (auto-included)

---

## Quick Start

### 1. Update Mapper

```java
// Replace BaseMapper with MysqlBaseMapper
public interface UserMapper extends MysqlBaseMapper<User> {
}
```

### 2. Update Service

```java
// Interface extends IMysqlServiceBase
public interface UserService extends IMysqlServiceBase<User> {
}

// Implementation extends MysqlServiceBaseImpl
@Service
public class UserServiceImpl extends MysqlServiceBaseImpl<UserMapper, User> implements UserService {
}
```

### 3. Start Using

```java
// Stream-style query
List<User> users = userService.stream()
    .filter(where -> where.eq(User::getRole, "admin"))
    .sorted(order -> order.orderDesc(User::getCreateTime))
    .limit(10)
    .collect(Collectors.toList());
```

---

## Features

### Get Single Entity

```java
User user = userService.get(where -> where.eq(User::getId, 1));
User user = userService.get(User::getId, 1);
User user = userService.getOrDefault(where -> where.eq(User::getId, 1), new User());
User user = userService.getByKeyForUpdate(1); // SELECT ... FOR UPDATE
```

### Get Value

```java
String name = userService.getValue(where -> where.eq(User::getId, 1), User::getUsername);
Integer total = userService.getValue(where -> where.eq(User::getStatus, 1), func -> func.count());
```

### List Query

```java
List<User> users = userService.list(where -> where.eq(User::getRole, "user"));

// With ordering and limit
List<User> users = userService.list(
    where -> where.eq(User::getStatus, 1),
    order -> order.orderDesc(User::getCreateTime),
    10
);

// Map to DTO
List<UserVO> vos = userService.list(
    where -> where.eq(User::getRole, "user"),
    select -> select
        .select(User::getId, UserVO::getId)
        .select(User::getUsername, UserVO::getUsername),
    UserVO.class
);
```

### Join Query

```java
List<UserOrderVO> vos = userService.listJoin(
    join -> join.leftJoin(Order.class, User::getId, Order::getUserId),
    where -> where.eq(User::getStatus, 1),
    select -> select
        .select(User::getUsername, UserOrderVO::getUsername)
        .select(Order::getOrderNo, UserOrderVO::getOrderNo),
    UserOrderVO.class
);
```

### Group + Join + Aggregation

```java
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

### Pagination

```java
IPage<User> page = userService.page(new Page<>(1, 10), where -> where.eq(User::getStatus, 1));

// Join pagination
IPage<UserVO> page = userService.pageJoin(
    new Page<>(1, 10),
    join -> join.leftJoin(Dept.class, User::getDeptId, Dept::getId),
    where -> where.eq(User::getStatus, 1),
    select -> select
        .select(User::getUsername, UserVO::getUsername)
        .select(Dept::getDeptName, UserVO::getDeptName),
    UserVO.class
);
```

### Stream API

```java
Optional<User> user = userService.stream()
    .filter(where -> where.eq(User::getRole, "admin"))
    .sorted(order -> order.orderDesc(User::getId))
    .findFirst();

Optional<String> name = userService.stream()
    .mapToColumn(User::getUsername)
    .filter(where -> where.eq(User::getId, 1))
    .findFirst();
```

### Batch Insert

```java
int rows = userService.saveBatchWithoutId(userList);
int rows = userService.saveDuplicate(userList, set -> set.set(User::getUpdateTime, LocalDateTime.now()));
int rows = userService.saveIgnore(userList);
int rows = userService.saveReplace(userList);
```

### Update & Delete

```java
int rows = userService.update(
    set -> set.set(User::getStatus, 0),
    where -> where.eq(User::getId, 1)
);

// Multi-table JOIN UPDATE
int rows = userService.updateJoin(
    join -> join.innerJoin(Dept.class, User::getDeptId, Dept::getId),
    set -> set.set(User::getStatus, 0),
    where -> where.eq(Dept::getDeptName, "Dissolved")
);

int rows = userService.remove(where -> where.eq(User::getStatus, 0));
```

---

## Usage Examples

<details>
<summary><b>Example 1: Dynamic Conditional Query</b></summary>

```java
public List<User> searchUsers(String keyword, Integer status, String role) {
    return userService.list(where -> {
        if (keyword != null) where.like(User::getUsername, keyword);
        if (status != null) where.eq(User::getStatus, status);
        if (role != null) where.eq(User::getRole, role);
    });
}
```
</details>

<details>
<summary><b>Example 2: Multi-table Report</b></summary>

```java
public List<DeptReportVO> getDeptReport() {
    return userService.listGroupJoin(
        join -> join.leftJoin(Order.class, User::getId, Order::getUserId),
        group -> group.groupBy(User::getDeptId),
        where -> where.eq(User::getStatus, 1),
        select -> select
            .select(User::getDeptId, DeptReportVO::getDeptId)
            .selectFunc(func -> func.count(User::getId), DeptReportVO::getUserCount)
            .selectFunc(func -> func.sum(Order::getAmount), DeptReportVO::getTotalAmount),
        DeptReportVO.class
    );
}
```
</details>

<details>
<summary><b>Example 3: Stream-style Processing</b></summary>

```java
Map<String, List<User>> usersByRole = userService.stream()
    .filter(where -> where.eq(User::getStatus, 1))
    .sorted(order -> order.orderAsc(User::getUsername))
    .collect(Collectors.groupingBy(User::getRole));
```
</details>

---

## Documentation

| Site | URL |
|------|-----|
| 📖 GitHub Pages | **https://kamioj.github.io/mybatis-plus-stream-docs/** |
| 🚀 Mirror - CN (Cloudflare) | **https://mybatis-plus-stream-docs.545329844.workers.dev/** |

## License

[GNU Affero General Public License v3.0](LICENSE)
