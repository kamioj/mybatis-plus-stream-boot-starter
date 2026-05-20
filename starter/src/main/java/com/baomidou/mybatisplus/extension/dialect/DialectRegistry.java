package com.baomidou.mybatisplus.extension.dialect;

import com.baomidou.mybatisplus.extension.dialect.impl.MySqlDialect;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 方言注册表 + 当前活跃方言持有者。<b>线程安全</b>。
 *
 * <h3>工作机制</h3>
 * <ol>
 *   <li>类加载时通过 {@link ServiceLoader} 扫描所有 {@code META-INF/services/...SqlDialect}
 *       注册的实现，按 {@link SqlDialect#dbType()} 索引进 EnumMap</li>
 *   <li>本库自带的 {@link MySqlDialect} 是默认实现，作为兜底（即使用户不注册任何方言也能用）</li>
 *   <li>用户可在启动时通过 {@link #use(DbType)} 或 {@link #use(SqlDialect)} 切换</li>
 *   <li>查询路径通过 {@link #current()} 获取当前方言</li>
 * </ol>
 *
 * <h3>用户自定义方言</h3>
 * <ol>
 *   <li>继承 {@link AbstractSqlDialect}，逐个实现方言敏感方法</li>
 *   <li>在 {@code META-INF/services/com.baomidou.mybatisplus.extension.dialect.SqlDialect}
 *       中声明实现类全限定名</li>
 *   <li>在 Spring Boot 启动时调 {@code DialectRegistry.use(myDialect)} 切换</li>
 * </ol>
 */
public final class DialectRegistry {

    private static final Map<DbType, SqlDialect> REGISTRY = new EnumMap<>(DbType.class);
    private static final AtomicReference<SqlDialect> CURRENT = new AtomicReference<>();

    static {
        // 1) 兜底注册默认 MySQL 方言（无依赖、无 ServiceLoader 也能 work）
        register(new MySqlDialect());
        // 2) ServiceLoader 扫描用户/扩展提供的方言
        ServiceLoader.load(SqlDialect.class).forEach(DialectRegistry::register);
        // 3) 默认 current = MySQL（兼容 3.x 行为）
        CURRENT.set(REGISTRY.get(DbType.MYSQL));
    }

    private DialectRegistry() {}

    /** 注册或覆盖一个方言实现 */
    public static void register(SqlDialect dialect) {
        Objects.requireNonNull(dialect, "dialect");
        REGISTRY.put(dialect.dbType(), dialect);
    }

    /** 按 DbType 查询已注册方言；未注册返回 null */
    public static SqlDialect get(DbType dbType) {
        return REGISTRY.get(dbType);
    }

    /** 切换当前方言（按 DbType） */
    public static void use(DbType dbType) {
        SqlDialect d = REGISTRY.get(dbType);
        if (d == null) {
            throw new IllegalArgumentException("No dialect registered for: " + dbType
                + ". Available: " + REGISTRY.keySet());
        }
        CURRENT.set(d);
    }

    /** 切换当前方言（按实例，会先 register 再切换） */
    public static void use(SqlDialect dialect) {
        register(dialect);
        CURRENT.set(dialect);
    }

    /** 获取当前活跃方言（永不返回 null，默认 MySQL）*/
    public static SqlDialect current() {
        return CURRENT.get();
    }
}
