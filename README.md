# mybatis-plus-stream-boot-starter

#### 介绍
{**以下是 Gitee 平台说明，您可以替换此简介**
Gitee 是 OSCHINA 推出的基于 Git 的代码托管平台（同时支持 SVN）。专为开发者提供稳定、高效、安全的云端软件开发协作平台
无论是个人、团队、或是企业，都能够用 Gitee 实现代码托管、项目管理、协作开发。企业项目请看 [https://gitee.com/enterprises](https://gitee.com/enterprises)}

#### 软件架构
软件架构说明


#### 安装教程

1.  xxxx
2.  xxxx
3.  xxxx

#### 使用说明

1.  修改mapper继承
```java
/**
 * mapper继承替换为MysqlBaseMapper
 */
public interface UserMapper extends MysqlBaseMapper<UserDo> {

}

```

2.  修改service继承
```java
public interface IUserService extends IMysqlServiceBase<UserDo> {
}

@Service
public class UserServiceImpl extends MysqlServiceBaseImpl<UserMapper, UserDo> implements IUserService {
}
```
3.  开始体验流式查询
```java
@Test
public void test() {
    // 查询整个实体数据
    // 方式一
    Optional<UserDo> optional = userService.stream().filter(where -> where.eq(UserDo::getId, 1)).findFirst();
    UserDo userDo = optional.get();
    // 方式二
    userDo = userService.get(where -> where.eq(UserDo::getId, 1));
    
    // 查询实体某一字段
    // 方式一
    Optional<String> stringOptional = userService.stream().mapToColumn(UserDo::getUserName).filter(where -> where.eq(UserDo::getId, 1)).findFirst();
    // 方式二
    String userName = userService.getValue(where -> where.eq(UserDo::getId, 1), UserDo::getUserName);
    
    // 连表查询
    userService.stream()
            .mapToColumn(UserDo::getUserName)
            .join(join -> join.innerJoin(UserAuthorDo.class,
                    where -> where.eqColumn(UserDo::getId, UserAuthorDo::getUserId)
            ))
            .filter(where -> where.eq(UserDo::getId, 1))
            .findFirst();
    // 我们常常需要将查询的数据放到另一个实体中而非直接返回mapper的实体, 所以我提供了更为便捷的查询方式使的可以让列一一对应
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
    // 如果你的列是一个由函数转换出来的值, 你可以试试selectFunc, 它几乎已经适配了所有常用的函数, 并且每个函数也支持fun以便于你的嵌套使用， 如countFun、sumFun等
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



#### 特技

1.  使用 mybatis-plus-stream-boot-starter 使你能够快速完成复杂的动态SQL查询, 并提供了了更为便捷快速的修改api支持， 是基于mybatis-plus的增强。 它具备mybatis-plus的所有功能， 并且支持动态SQL连表查询。 摒弃传统复杂的动态参数传入到xml文件中， 将sql完全以编码流的形式呈现。
2.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
