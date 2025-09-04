# mybatis-plus-stream-boot-starter

## 介绍
这是一个基于 MyBatis-Plus 的增强工具包，它提供了流式查询的功能，使得开发者能够以更加直观和函数式的方式进行数据库操作。该工具包支持复杂的动态 SQL 查询，并提供了便捷的 API 来处理连接、过滤、映射等常见数据库操作。

## 软件架构
本项目基于 Java 编写，并依赖于 MyBatis-Plus 框架。主要组件包括：

- **MysqlBaseMapper**: 扩展了 MyBatis-Plus 的 BaseMapper 接口。
- **IMysqlServiceBase 和 MysqlServiceBaseImpl**: 提供了服务层接口及其实现类，用于封装业务逻辑。
- **各种 Lambda Query Wrappers**: 如 `NormalWhereLambdaQueryWrapper`, `GroupFunctionLambdaQueryWrapper` 等，用于构建类型安全的查询条件。
- **Stream API**: 通过 `MybatisQueryableStream` 和 `MybatisExecutableStream` 提供了类似 Java Stream 的 API 来进行数据库查询和更新操作。

## 安装教程

1. 在你的 Spring Boot 项目的 `pom.xml` 文件中添加此库的依赖。
2. 确保你已经配置好了 MyBatis-Plus 和数据库连接信息。
3. 使用提供的 Mapper 和 Service 实现你的数据访问层。

## 使用说明

### 修改 Mapper 继承

将你的 Mapper 接口从继承 `BaseMapper` 改为继承 `MysqlBaseMapper`：

```java
public interface UserMapper extends MysqlBaseMapper<UserDo> {
}
```

### 修改 Service 继承

将你的 Service 接口从继承 `IService` 改为继承 `IMysqlServiceBase`，并让实现类继承 `MysqlServiceBaseImpl`：

```java
public interface IUserService extends IMysqlServiceBase<UserDo> {
}

@Service
public class UserServiceImpl extends MysqlServiceBaseImpl<UserMapper, UserDo> implements IUserService {
}
```

### 开始体验流式查询

你可以使用类似于 Java Stream 的 API 来执行查询：

```java
@Test
public void test() {
    // 查询整个实体数据
    Optional<UserDo> optional = userService.stream().filter(where -> where.eq(UserDo::getId, 1)).findFirst();
    UserDo userDo = optional.get();

    // 或者直接获取实体
    userDo = userService.get(where -> where.eq(UserDo::getId, 1));

    // 查询实体某一字段
    Optional<String> stringOptional = userService.stream().mapToColumn(UserDo::getUserName).filter(where -> where.eq(UserDo::getId, 1)).findFirst();
    String userName = userService.getValue(where -> where.eq(UserDo::getId, 1), UserDo::getUserName);

    // 连表查询
    userService.stream()
            .mapToColumn(UserDo::getUserName)
            .join(join -> join.innerJoin(UserAuthorDo.class,
                    where -> where.eqColumn(UserDo::getId, UserAuthorDo::getUserId)
            ))
            .filter(where -> where.eq(UserDo::getId, 1))
            .findFirst();

    // 映射到 VO 对象
    userService.stream()
            .map(select -> select
                            .select(UserDo::getId, UserVo::getId)
                            .select(UserDo::getUserName, UserVo::getUserName)
                    , UserVo.class)
            .join(join -> join.innerJoin(UserAuthorDo.class,
                    where -> where.eqColumn(UserDo::getId, UserAuthorDo::getUserId)
            ))
            .filter(where -> where.eq(UserDo::getId, 1))
            .findFirst();

    // 使用函数转换值
    userService.stream()
            .map(select -> select
                            .select(UserDo::getId, UserVo::getId)
                            .select(UserDo::getUserName, UserVo::getUserName)
                            .selectFunc(fun -> fun.count(UserAuthorDo::getId), UserVo::getCont)
                    , UserVo.class)
            .join(join -> join.innerJoin(UserAuthorDo.class,
                    where -> where.eqColumn(UserDo::getId, UserAuthorDo::getUserId)
            ))
            .filter(where -> where.eq(UserDo::getId, 1))
            .findFirst();
}
```

## 特技

- **流式查询**：提供了一种新的方式来构建查询，它结合了函数式编程风格与 MyBatis-Plus 的强大功能。
- **动态 SQL**：支持构建复杂的动态 SQL 查询，无需手动拼接 SQL 字符串。
- **连表查询**：简化了多表连接的操作，使得代码更清晰易懂。
- **列映射**：可以轻松地将查询结果映射到不同的对象或字段上。
- **函数支持**：内置了多种常用的数据库函数，如 COUNT, SUM, AVG 等，可以直接在查询中使用。

这个工具包旨在提高开发效率，减少样板代码，并且保持代码的可读性和可维护性。