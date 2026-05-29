package com.baomidou.mybatisplus.extension.it.dm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.it.mysql.CapturedSql;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlDemandDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlOrderDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserDo;
import com.baomidou.mybatisplus.extension.it.mysql.OrderStatsDo;
import com.baomidou.mybatisplus.extension.it.mysql.UserStatsDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 场景桶：JoinProjection —— JOIN/投影/排序全覆盖（达梦 DAMENG 方言）
 * <p>
 * 与 MysqlJoinProjectionCoverageIntegrationTest 场景对称，
 * 方言差异：列名/别名用双引号而非反引号；
 * 回归修复 M6 验证双引号包裹形态。
 */
class DmJoinProjectionCoverageIntegrationTest extends DmIntegrationTestBase {

    // -----------------------------------------------------------------------
    // listJoin 基础场景
    // -----------------------------------------------------------------------

    @Test
    void listJoinUserOrderReturnsAllMatchingRowsOnDameng() {
        // user LEFT JOIN order，获取有订单的用户记录
        // 种子：Alice(2笔)/Bob(1笔)/Carol(1笔)/Dave(1笔) → 展开共 5 行
        List<MysqlUserDo> rows = userService.listJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.isNotNull(MysqlOrderDo::getId)
        );
        assertEquals(5, rows.size());
        assertSqlContains("left join mps_order");
        assertSqlContains("mps_order.id is not null");
    }

    @Test
    void listJoinWithSelectProjectionMapsToDtoOnDameng() {
        // JOIN 后只 select 指定列，映射到 UserStatsDto
        List<UserStatsDto> rows = userService.listJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getUsername, "Alice"),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getAmount, UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );
        // Alice 有 2 笔订单
        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(r -> "Alice".equals(r.getUsername())));
        List<BigDecimal> amounts = rows.stream().map(UserStatsDto::getTotalAmount).toList();
        assertTrue(amounts.contains(new BigDecimal("100.00")));
        assertTrue(amounts.contains(new BigDecimal("60.00")));
        assertSqlContains("left join mps_order");
        assertSqlContains("mps_user.username");
        assertSqlContains("mps_order.amount");
    }

    @Test
    void listJoinOrderByDescOnJoinedColumnOnDameng() {
        // JOIN order，按 amount DESC 取前 3 行
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
        // 最大金额 220.00（Carol）排第一
        assertEquals(0, new BigDecimal("220.00").compareTo(rows.get(0).getTotalAmount()));
        assertSqlContains("order by mps_order.amount desc");
        assertSqlContains("limit 3");
    }

    @Test
    void listJoinOrderByAscOnMultipleColumnsOnDameng() {
        // 多列排序：credit_score ASC, id ASC
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
        assertFalse(rows.isEmpty());
        // 第一行应是 credit_score 最低的 Dave(65)
        assertEquals("Dave", rows.get(0).getUsername());
        assertSqlContains("order by mps_user.credit_score asc");
        assertSqlContains("mps_user.id asc");
        assertSqlContains("inner join mps_order");
    }

    // -----------------------------------------------------------------------
    // listJoin 带 rename（M6 回归：别名必须带达梦双引号）
    // -----------------------------------------------------------------------

    @Test
    void listJoinWithRenameAliasWrappedInDialectQuotes_M6RegressionOnDameng() {
        // 回归点 M6：select 字段带 rename 时，生成的 AS 别名应被达梦方言的双引号包裹
        // 例如：mps_user.username AS "username"
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
        // M6 核心断言：SQL 中别名应出现被双引号包裹的 token
        String rawSql = CapturedSql.joined();
        assertTrue(
            rawSql.contains("\"username\"") || rawSql.contains("\"totalAmount\"")
                || rawSql.contains("\"roleCode\""),
            () -> "期望别名被双引号包裹（DM 方言），实际 SQL：" + rawSql
        );
    }

    @Test
    void listJoinDemandWithSelectAndOrderAscOnDameng() {
        // user LEFT JOIN demand，select userId/username，按 title ASC
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
        // open demand：201(Alice)/203(Bob)/204(Carol) → 3 行
        assertEquals(3, rows.size());
        assertSqlContains("left join mps_demand");
        assertSqlContains("mps_demand.status");
        assertSqlContains("order by mps_demand.title asc");
    }

    // -----------------------------------------------------------------------
    // pageJoin 分页场景
    // -----------------------------------------------------------------------

    @Test
    void pageJoinFirstPageReturnsCorrectTotalAndSubsetOnDameng() {
        // 第 1 页，每页 2 条；active=true，LEFT JOIN order → 4 行
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
    void pageJoinSecondPageReturnsRemainingRowsOnDameng() {
        // 第 2 页，验证 offset/limit 正确渲染
        IPage<UserStatsDto> page = userService.pageJoin(
            new Page<>(2, 2),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getAmount, UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );
        assertEquals(4, page.getTotal());
        assertEquals(2, page.getRecords().size());
        assertSqlContains("limit 2");
    }

    @Test
    void pageJoinDemandWithProjectionAndTotalOnDameng() {
        // user LEFT JOIN demand，分页投影 userId + username，total=5
        IPage<UserStatsDto> page = userService.pageJoin(
            new Page<>(1, 3),
            join -> join.leftJoin(MysqlDemandDo.class, MysqlUserDo::getId, MysqlDemandDo::getUserId),
            where -> where.isNotNull(MysqlDemandDo::getId),
            select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername),
            UserStatsDto.class
        );
        assertEquals(5, page.getTotal());
        assertEquals(3, page.getRecords().size());
        assertSqlContains("left join mps_demand");
    }

    // -----------------------------------------------------------------------
    // listGroupJoin 分组 + JOIN
    // -----------------------------------------------------------------------

    @Test
    void listGroupJoinUserOrderCountAndSumPerUserOnDameng() {
        // user LEFT JOIN order，按 user.id/username 分组，统计订单数和金额合计
        // DM 要求 SELECT 中非聚合列必须都出现在 GROUP BY 中
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
    void listGroupJoinWithOrderByAndLimitOnDameng() {
        // 分组后按 creditScore DESC，取 top-2
        List<UserStatsDto> top2 = userService.listGroupJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            group -> group.groupBy(MysqlUserDo::getId)
                .groupBy(MysqlUserDo::getUsername)
                .groupBy(MysqlUserDo::getCreditScore),
            where -> where.isNotNull(MysqlOrderDo::getId),
            order -> order.orderDesc(MysqlUserDo::getCreditScore),
            2,
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .selectFunc(func -> func.count(MysqlOrderDo::getId), UserStatsDto::getOrderCount),
            UserStatsDto.class
        );
        assertEquals(2, top2.size());
        // Alice(98) > Carol(88)
        assertEquals("Alice", top2.get(0).getUsername());
        assertSqlContains("group by");
        assertSqlContains("order by mps_user.credit_score desc");
        assertSqlContains("limit 2");
    }

    @Test
    void listGroupJoinUserDemandCountsOpenDemandsOnDameng() {
        // user LEFT JOIN demand，按 user.id/username 分组，统计 open 需求数
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
        UserStatsDto alice = stats.stream()
            .filter(r -> "Alice".equals(r.getUsername()))
            .findFirst().orElseThrow();
        assertEquals(1L, alice.getOpenDemandCount());
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
    void listJoinWithSelectAllMapsFullEntityColumnsOnDameng() {
        // selectAll：INNER JOIN order WHERE status='paid'，返回主表全字段
        List<MysqlUserDo> rows = userService.listJoin(
            join -> join.innerJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlOrderDo::getStatus, "paid")
        );
        // paid 订单：101(Alice)/102(Alice)/104(Carol) → 3 行
        assertEquals(3, rows.size());
        assertTrue(rows.stream().allMatch(r -> r.getUsername() != null));
        assertTrue(rows.stream().allMatch(r -> r.getId() != null));
        assertSqlContains("inner join mps_order");
        assertSqlContains("mps_order.status");
    }

    // -----------------------------------------------------------------------
    // 子查询 select 场景
    // -----------------------------------------------------------------------

    @Test
    void listJoinWithSubQuerySelectProducesCorrelatedSubSqlOnDameng() {
        // select 嵌入标量子查询，计算每个用户的 open demand 数
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
        // Alice 有 2 笔订单，各行 openDemandCount=1
        assertEquals(2, rows.size());
        rows.forEach(r -> assertEquals(1L, r.getOpenDemandCount()));
        assertSqlContains("(select count");
        assertSqlContains("from mps_demand");
        assertSqlContains("left join mps_order");
    }

    // -----------------------------------------------------------------------
    // M6 回归：派生表 JOIN（子查询作为 JOIN 右侧）中别名带达梦双引号
    // -----------------------------------------------------------------------

    @Test
    void listJoinDerivedTableAliasWrappedInDialectQuotes_M6RegressionOnDameng() {
        // 子查询作为 JOIN 右侧，SELECT 字段列别名应带双引号（DM 方言）
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
        // M6 断言：生成的 SELECT 列别名应带双引号（DM 方言）
        String rawSql = CapturedSql.joined();
        assertTrue(
            rawSql.contains("\"username\"") || rawSql.contains("\"totalAmount\""),
            () -> "期望 SELECT 别名被双引号包裹（DM 方言），实际 SQL：" + rawSql
        );
        assertSqlContains("inner join (select");
        assertSqlContains("as os");
    }
}
