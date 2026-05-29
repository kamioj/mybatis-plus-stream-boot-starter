package com.baomidou.mybatisplus.extension.it.mysql;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
            org.apache.ibatis.cache.CacheKey.class, BoundSql.class})
})
public class CapturedSql implements Interceptor {

    private static final ThreadLocal<List<String>> SQL = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        if (args.length >= 2 && args[0] instanceof MappedStatement statement) {
            BoundSql boundSql = args.length == 6 && args[5] instanceof BoundSql existing
                ? existing : statement.getBoundSql(args[1]);
            SQL.get().add(normalize(boundSql.getSql()));
        }
        return invocation.proceed();
    }

    public static void clear() {
        SQL.get().clear();
    }

    public static List<String> all() {
        return List.copyOf(SQL.get());
    }

    public static String joined() {
        return String.join("\n", SQL.get());
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
