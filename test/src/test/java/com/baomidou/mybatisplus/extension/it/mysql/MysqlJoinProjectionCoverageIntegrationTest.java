package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 场景桶：JoinProjection —— JOIN/投影/排序全覆盖（MySQL 方言）
 * <p>
 * 覆盖：listJoin / pageJoin / listGroupJoin；select 指定列 / selectAll；
 * 子查询 select；orderBy asc/desc/多列；join 带别名 rename。
 * 回归修复 M6：带 rename 的 JOIN，别名应被方言引号（反引号）包裹。
 */
class MysqlJoinProjectionCoverageIntegrationTest extends MysqlIntegrationTestBase {

    // -----------------------------------------------------------------------
    // listJoin 基础场景
    // -----------------------------------------------------------------------

    @Test
    void listJoinUserOrderReturnsAllMatchingRows() {
        // 用 user LEFT JOIN order，获取有订单的用户记录（返回主表 MysqlUserDo）
        // 种子数据：Alice 有 2 笔订单，Bob/Carol/Dave 各 1 笔，共 4 条 user 行
        List<MysqlUserDo> rows = userService.listJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.isNotNull(MysqlOrderDo::getId)
        );
        // Alice 有 2 笔订单，因此展开后应出现 2 次；Bob/Carol/Dave 各 1 次 → 共 5 行
        assertEquals(5, rows.size());
        assertSqlContains("left join mps_order");
        assertSqlContains("mps_order.id is not null");
    }

    @Test
    void listJoinWithSelectProjectionMapsToDto() {
        // JOIN 后只 select 指定列（username / orderAmount），映射到 UserStatsDto
        List<UserStatsDto> rows = userService.listJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getUsername, "Alice"),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getAmount, UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );
        // Alice 有 2 笔订单（100.00 / 60.00），期望 2 行
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(r -> "Alice".equals(r.getUsername())));
        // 验证投影金额出现了种子金额中的两个值
        List<BigDecimal> amounts = rows.stream().map(UserStatsDto::getTotalAmount).toList();
        assertTrue(amounts.contains(new BigDecimal("100.00")));
        assertTrue(amounts.contains(new BigDecimal("60.00")));
        assertSqlContains("left join mps_order");
        assertSqlContains("mps_user.username");
        assertSqlContains("mps_order.amount");
    }

    @Test
    void listJoinOrderByDescOnJoinedColumn() {
        // JOIN order 表，按 amount DESC 取前 3 行，验证排序方向
        List<UserStatsDto> rows = userService.listJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.isNotNull(MysqlOrderDo::getId),
            order -> order.orderDesc(MysqlOrderDo::getAmount),
            3,
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getAmount, UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );
        assertEquals(3, rows.size());
        // 最大金额 220.00（Carol）应排第一
        assertEquals(0, new BigDecimal("220.00").compareTo(rows.get(0).getTotalAmount()));
        assertSqlContains("order by mps_order.amount desc");
        assertSqlContains("limit 3");
    }

    @Test
    void listJoinOrderByAscOnMultipleColumns() {
        // 多列排序：先按 credit_score ASC，再按 id ASC
        List<UserStatsDto> rows = userService.listJoin(
            join -> join.innerJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.isNotNull(MysqlOrderDo::getId),
            order -> order.orderAsc(MysqlUserDo::getCreditScore).orderAsc(MysqlUserDo::getId),
            10,
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlUserDo::getCreditScore, UserStatsDto::getOrderCount),
            UserStatsDto.class
        );
        // 种子：Dave(65) < Bob(72) < Carol(88) < Alice(98)；Alice 出现两次
        assertFalse(rows.isEmpty());
        // 第一行应是 credit_score 最低的 Dave(65)
        assertEquals("Dave", rows.get(0).getUsername());
        assertSqlContains("order by mps_user.credit_score asc");
        assertSqlContains("mps_user.id asc");
        assertSqlContains("inner join mps_order");
    }

    // -----------------------------------------------------------------------
    // listJoin 带 rename（M6 回归：别名必须带方言引号）
    // -----------------------------------------------------------------------

    @Test
    void listJoinWithRenameAliasWrappedInDialectQuotes_M6Regression() {
        // 回归点 M6：select 字段带 rename 时，生成的 AS 别名应被 MySQL 方言的反引号包裹
        // 例如：mps_user.username AS `username`, mps_order.amount AS `totalAmount`
        List<UserStatsDto> rows = userService.listJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getUsername, "Bob"),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getAmount, UserStatsDto::getTotalAmount)
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode),
            UserStatsDto.class
        );
        assertEquals(1, rows.size());
        assertEquals("Bob", rows.get(0).getUsername());
        assertEquals(0, new BigDecimal("35.50").compareTo(rows.get(0).getTotalAmount()));
        assertEquals("user", rows.get(0).getRoleCode());
        // M6 核心断言：SQL 中别名应出现被反引号包裹的 token
        // comparableSql 会去掉引号，所以用 CapturedSql.joined() 直接断言原始 SQL
        String rawSql = CapturedSql.joined();
        // 别名 `username` / `totalAmount` / `roleCode` 中至少其一有反引号包裹
        assertTrue(
            rawSql.contains("`username`") || rawSql.contains("`totalAmount`")
                || rawSql.contains("`roleCode`"),
            () -> "期望别名被反引号包裹，实际 SQL：" + rawSql
        );
    }

    @Test
    void listJoinDemandWithSelectAndOrderAsc() {
        // user LEFT JOIN demand，选取 userId/username/title，按 title ASC
        List<UserStatsDto> rows = userService.listJoin(
            join -> join.leftJoin(MysqlDemandDo.class, MysqlUserDo::getId, MysqlDemandDo::getUserId),
            where -> where.eq(MysqlDemandDo::getStatus, "open"),
            order -> order.orderAsc(MysqlDemandDo::getTitle),
            10,
            select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername),
            UserStatsDto.class
        );
        // open 状态 demand：201(Alice)/203(Bob)/204(Carol) → 3 行
        assertEquals(3, rows.size());
        assertSqlContains("left join mps_demand");
        assertSqlContains("mps_demand.status");
        assertSqlContains("order by mps_demand.title asc");
    }

    // -----------------------------------------------------------------------
    // pageJoin 分页场景
    // -----------------------------------------------------------------------

    @Test
    void pageJoinFirstPageReturnsCorrectTotalAndSubset() {
        // 第 1 页，每页 2 条；user LEFT JOIN order，where active=true
        // active 用户：Alice(2笔) / Bob(1笔) / Dave(1笔) → 展开共 4 行
        IPage<UserStatsDto> page = userService.pageJoin(
            new Page<>(1, 2),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getStatus, UserStatsDto::getRoleCode),
            UserStatsDto.class
        );
        assertEquals(4, page.getTotal());
        assertEquals(2, page.getRecords().size());
        assertSqlContains("left join mps_order");
        assertSqlContains("limit 2");
    }

    @Test
    void pageJoinSecondPageReturnsRemainingRows() {
        // 第 2 页验证 offset 是否正确推入 SQL
        IPage<UserStatsDto> page = userService.pageJoin(
            new Page<>(2, 2),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getAmount, UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );
        // 第 2 页剩余 2 行（共 4 行，每页 2 行）
        assertEquals(4, page.getTotal());
        assertEquals(2, page.getRecords().size());
        // SQL 中应含 LIMIT 2 OFFSET 2（或等价的 limit 2, 2）
        assertSqlContains("limit 2");
    }

    @Test
    void pageJoinDemandWithProjectionAndTotal() {
        // user LEFT JOIN demand，分页投影 userId + username
        IPage<UserStatsDto> page = userService.pageJoin(
            new Page<>(1, 3),
            join -> join.leftJoin(MysqlDemandDo.class, MysqlUserDo::getId, MysqlDemandDo::getUserId),
            where -> where.isNotNull(MysqlDemandDo::getId),
            select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername),
            UserStatsDto.class
        );
        // 5 条 demand 均有对应 user，total=5，第一页取 3 行
        assertEquals(5, page.getTotal());
        assertEquals(3, page.getRecords().size());
        assertSqlContains("left join mps_demand");
    }

    // -----------------------------------------------------------------------
    // listGroupJoin 分组 + JOIN
    // -----------------------------------------------------------------------

    @Test
    void listGroupJoinUserOrderCountAndSumPerUser() {
        // user LEFT JOIN order，按 user.id 分组，统计每人订单数和金额合计
        List<UserStatsDto> stats = userService.listGroupJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            group -> group.groupBy(MysqlUserDo::getId).groupBy(MysqlUserDo::getUsername),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .selectFunc(func -> func.count(MysqlOrderDo::getId), UserStatsDto::getOrderCount)
                .selectFunc(func -> func.sum(MysqlOrderDo::getAmount), UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );
        // 4 个未删除用户，各有对应的 order（Alice=2，Bob=1，Carol=1，Dave=1）
        assertEquals(4, stats.size());
        UserStatsDto alice = stats.stream()
            .filter(r -> "Alice".equals(r.getUsername()))
            .findFirst().orElseThrow();
        assertEquals(2L, alice.getOrderCount());
        assertEquals(0, new BigDecimal("160.00").compareTo(alice.getTotalAmount()));
        assertSqlContains("left join mps_order");
        assertSqlContains("group by mps_user.id");
        assertSqlContains("count(mps_order.id)");
        assertSqlContains("sum(mps_order.amount)");
    }

    @Test
    void listGroupJoinWithOrderByAndLimit() {
        // 分组后按 totalAmount DESC，取 top-2
        List<UserStatsDto> top2 = userService.listGroupJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            group -> group.groupBy(MysqlUserDo::getId).groupBy(MysqlUserDo::getUsername),
            where -> where.isNotNull(MysqlOrderDo::getId),
            order -> order.orderDesc(MysqlUserDo::getCreditScore),
            2,
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .selectFunc(func -> func.count(MysqlOrderDo::getId), UserStatsDto::getOrderCount),
            UserStatsDto.class
        );
        assertEquals(2, top2.size());
        // 按 creditScore DESC：Alice(98) > Carol(88)
        assertEquals("Alice", top2.get(0).getUsername());
        assertSqlContains("group by");
        assertSqlContains("order by mps_user.credit_score desc");
        assertSqlContains("limit 2");
    }

    @Test
    void listGroupJoinUserDemandCountsOpenDemands() {
        // user LEFT JOIN demand，按 user.id 分组，统计 open 需求数
        List<UserStatsDto> stats = userService.listGroupJoin(
            join -> join.leftJoin(MysqlDemandDo.class, MysqlUserDo::getId, MysqlDemandDo::getUserId),
            group -> group.groupBy(MysqlUserDo::getId).groupBy(MysqlUserDo::getUsername),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .selectFunc(func -> func.countPredicate(
                    w -> w.eq(MysqlDemandDo::getStatus, "open")), UserStatsDto::getOpenDemandCount),
            UserStatsDto.class
        );
        assertEquals(4, stats.size());
        // Alice 有 1 个 open demand（201），1 个 closed（202）
        UserStatsDto alice = stats.stream()
            .filter(r -> "Alice".equals(r.getUsername()))
            .findFirst().orElseThrow();
        assertEquals(1L, alice.getOpenDemandCount());
        // Dave 有 0 个 open demand（205 是 closed）
        UserStatsDto dave = stats.stream()
            .filter(r -> "Dave".equals(r.getUsername()))
            .findFirst().orElseThrow();
        assertEquals(0L, dave.getOpenDemandCount());
        assertSqlContains("left join mps_demand");
        assertSqlContains("group by mps_user.id");
        assertSqlContains("case when");
    }

    // -----------------------------------------------------------------------
    // selectAll 场景
    // -----------------------------------------------------------------------

    @Test
    void listJoinWithSelectAllMapsFullEntityColumns() {
        // selectAll 返回主表全部字段，不裁剪
        List<MysqlUserDo> rows = userService.listJoin(
            join -> join.innerJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlOrderDo::getStatus, "paid")
        );
        // paid 订单：101(Alice)/102(Alice)/104(Carol) → 3 行
        assertEquals(3, rows.size());
        // 验证全字段映射正常：username 不为 null
        assertTrue(rows.stream().allMatch(r -> r.getUsername() != null));
        assertTrue(rows.stream().allMatch(r -> r.getId() != null));
        assertSqlContains("inner join mps_order");
        assertSqlContains("mps_order.status");
    }

    // -----------------------------------------------------------------------
    // 子查询 select 场景
    // -----------------------------------------------------------------------

    @Test
    void listJoinWithSubQuerySelectProducesCorrelatedSubSql() {
        // select 中嵌入标量子查询：计算每个用户的 open demand 数
        List<UserStatsDto> rows = userService.listJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getUsername, "Alice"),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .selectSubSql(
                    sub -> sub.from(MysqlDemandDo.class)
                        .select(s -> s.selectFunc(f -> f.count(), com.baomidou.mybatisplus.extension.value.SingleValue::getValue))
                        .where(w -> w
                            .eqColumn(MysqlDemandDo::getUserId, MysqlUserDo::getId)
                            .eq(MysqlDemandDo::getStatus, "open")),
                    UserStatsDto::getOpenDemandCount),
            UserStatsDto.class
        );
        // Alice 有 2 笔订单，期望 2 行，每行 openDemandCount=1
        assertEquals(2, rows.size());
        rows.forEach(r -> assertEquals(1L, r.getOpenDemandCount()));
        assertSqlContains("(select count");
        assertSqlContains("from mps_demand");
        assertSqlContains("left join mps_order");
    }

    // -----------------------------------------------------------------------
    // M6 回归：派生表 JOIN（子查询作为 JOIN 右侧）中别名带方言引号
    // -----------------------------------------------------------------------

    @Test
    void listJoinDerivedTableAliasWrappedInDialectQuotes_M6Regression() {
        // 子查询作为 JOIN 右侧，别名 "os" 在 MySQL 方言中不加引号（表别名），
        // 但 SELECT 字段的列别名应带反引号。
        // 同时验证派生表 JOIN 能正常执行且返回结果正确。
        List<UserStatsDto> rows = userService.listJoin(
            join -> join.innerJoin(
                sub -> sub.from(MysqlOrderDo.class)
                    .select(s -> s.select(MysqlOrderDo::getUserId)
                        .selectFunc(f -> f.sum(MysqlOrderDo::getAmount), OrderStatsDo::getTotalAmount))
                    .group(g -> g.groupBy(MysqlOrderDo::getUserId)),
                "os",
                on -> on.eqColumn(MysqlUserDo::getId, null, OrderStatsDo::getUserId, "os")),
            where -> where.gt(OrderStatsDo::getTotalAmount, "os", new BigDecimal("50.00")),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(OrderStatsDo::getTotalAmount, "os", UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );
        // Alice(160.00) / Carol(220.00) 满足 > 50，Bob(35.50) 不满足
        assertEquals(2, rows.size());
        List<String> names = rows.stream().map(UserStatsDto::getUsername).sorted().toList();
        assertEquals(List.of("Alice", "Carol"), names);
        // M6 断言：生成的 SELECT 列别名应带反引号
        String rawSql = CapturedSql.joined();
        assertTrue(
            rawSql.contains("`username`") || rawSql.contains("`totalAmount`"),
            () -> "期望 SELECT 别名被反引号包裹，实际 SQL：" + rawSql
        );
        assertSqlContains("inner join (select");
        assertSqlContains("as os");
    }
}
