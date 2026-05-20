package com.baomidou.mybatisplus.extension.dialect;

import com.baomidou.mybatisplus.extension.metadata.ColumnInfo;
import com.baomidou.mybatisplus.extension.metadata.SqlDataType;

import java.util.List;
// 说明：本类目前只做「强制子类显式实现方言敏感方法」。后续阶段若需要方言无关的
// 共享渲染工具（如字符串字面值转义），再在此补充 protected 方法并同步更新本注释。

/**
 * 方言抽象基类。<b>所有内置方言（MySQL / PostgreSQL / DM）以及推荐的用户自定义方言都应直接继承本类</b>，
 * 而不是互相继承。
 *
 * <h3>为什么需要本类</h3>
 * <p>4.1 之前 {@code PostgreSqlDialect} / {@code DamengDialect} 都 {@code extends MySqlDialect}，
 * 一个方言把另一个具体方言当父类。后果是：只要 {@link SqlDialect} 新增一个方言敏感方法，或
 * MySQL 改了某个 SQL 写法，PG/DM 会<b>静默继承</b> MySQL 行为，直到某个集成测试踩中才暴露
 * （{@code regexp} 就是典型——PG 一直静默继承了 MySQL 的 {@code left REGEXP right}，而 PG 实际用
 * {@code ~} 操作符）。
 *
 * <h3>设计</h3>
 * <p>把方言之间<b>真正有差异</b>的方法在本类重新声明为 {@code abstract}，编译期即强制每个方言
 * 显式实现——要么给出本方言写法，要么显式调用通用写法，<b>不允许靠继承「碰巧拿到」某个实现</b>。
 * 真正方言无关的逻辑（如 {@link #forUpdate(LockMode)} 单参重载、字符串字面值转义）留默认实现。
 *
 * <p>{@link SqlDialect} 接口本身保持不变：其中 {@code regexp} 等方法仍是 {@code default}，
 * 直接 {@code implements SqlDialect} 的旧用户代码不受影响；但继承本类的方言必须显式实现。
 */
public abstract class AbstractSqlDialect implements SqlDialect {

    /* ============== 方言敏感方法：强制子类显式实现 ============== */

    @Override
    public abstract DbType dbType();

    @Override
    public abstract String paginate(String baseSql, long offset, long limit);

    @Override
    public abstract String concat(String... parts);

    @Override
    public abstract String groupConcat(String columnExpr, String separator);

    @Override
    public abstract String regexp(String leftExpr, String rightExpr);

    @Override
    public abstract String cast(String expr, SqlDataType type);

    @Override
    public abstract String dataTypeName(SqlDataType type);

    @Override
    public abstract String forUpdate(LockMode mode, int waitSeconds);

    @Override
    public abstract String quoteIdentifier(String name);

    @Override
    public abstract String insertPrefix(WriteMode mode);

    @Override
    public abstract String conflictClause(WriteMode mode, List<SetterClause> setters, ColumnInfo pkColumn, String[] allColumns);

    @Override
    public abstract String incomingColumnRef(String bareColumn);

    @Override
    public abstract String updateSetTarget(String tableQualifier, String bareColumn);
}
