package com.baomidou.mybatisplus.extension.it.dm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlDemandDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlOrderDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserDo;
import com.baomidou.mybatisplus.extension.it.mysql.OrderStatsDo;
import com.baomidou.mybatisplus.extension.it.mysql.UserStatsDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.value.SingleValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DmComplexSqlIntegrationTest extends DmIntegrationTestBase {

    @Test
    void selectFuncCaseAndScalarSubQueryMapToDtoOnDameng() {
        List<UserStatsDto> rows = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .selectCase(
                    c -> c.whenThenValue(w -> w.ge(MysqlUserDo::getCreditScore, 90), "excellent")
                        .whenThenValue(w -> w.ge(MysqlUserDo::getCreditScore, 70), "normal")
                        .elseValue("risk"),
                    UserStatsDto::getGrade)
                .selectSubSql(
                    sub -> sub.from(MysqlDemandDo.class)
                        .select(s -> s.selectFunc(f -> f.count(), SingleValue::getValue))
                        .where(w -> w.eqColumn(MysqlDemandDo::getUserId, MysqlUserDo::getId)
                            .eq(MysqlDemandDo::getStatus, "open")),
                    UserStatsDto::getOpenDemandCount),
                UserStatsDto.class)
            .collect(java.util.stream.Collectors.toList());

        assertEquals(1, rows.size());
        assertEquals("Alice", rows.get(0).getUsername());
        assertEquals("excellent", rows.get(0).getGrade());
        assertEquals(1L, rows.get(0).getOpenDemandCount());
        assertSqlContains("case when");
        assertSqlContains("(select count");
        assertSqlContains("from mps_demand");
    }

    @Test
    void joinGroupHavingAndAggregateFunctionsReturnExpectedStatsOnDameng() {
        List<UserStatsDto> stats = userService.listGroupJoin(
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            group -> group.groupBy(MysqlUserDo::getId)
                .groupBy(MysqlUserDo::getUsername)
                .groupBy(MysqlUserDo::getCreditScore)
                .having(where -> where.gtFunc(func -> func.count(MysqlOrderDo::getId), func -> func.value(0L))),
            where -> where.eq(MysqlUserDo::getActive, true),
            order -> order.orderDesc(MysqlUserDo::getCreditScore),
            10,
            select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .selectFunc(func -> func.count(MysqlOrderDo::getId), UserStatsDto::getOrderCount)
                .selectFunc(func -> func.sum(MysqlOrderDo::getAmount), UserStatsDto::getTotalAmount)
                .selectFunc(func -> func.avg(MysqlUserDo::getCreditScore), UserStatsDto::getAvgScore),
            UserStatsDto.class
        );

        assertEquals(3, stats.size());
        UserStatsDto alice = stats.get(0);
        assertEquals("Alice", alice.getUsername());
        assertEquals(2L, alice.getOrderCount());
        assertEquals(0, new BigDecimal("160.00").compareTo(alice.getTotalAmount()));
        assertEquals(98.0, alice.getAvgScore());
        assertSqlContains("left join mps_order");
        assertSqlContains("group by mps_user.id");
        assertSqlContains("having");
        assertSqlContains("sum(");
    }

    @Test
    void subQueryWhereExistsNotExistsInAndDerivedJoinAreExecutableOnDameng() {
        List<MysqlUserDo> usersWithPaidOrders = userService.list(where -> where
            .exists(sub -> sub.from(MysqlOrderDo.class)
                .where(sw -> sw.eqColumn(MysqlOrderDo::getUserId, MysqlUserDo::getId)
                    .eq(MysqlOrderDo::getStatus, "paid"))));
        assertEquals(List.of("Alice", "Carol"),
            usersWithPaidOrders.stream().map(MysqlUserDo::getUsername).toList());

        List<MysqlUserDo> usersWithoutOpenDemand = userService.list(where -> where
            .notExists(sub -> sub.from(MysqlDemandDo.class)
                .where(sw -> sw.eqColumn(MysqlDemandDo::getUserId, MysqlUserDo::getId)
                    .eq(MysqlDemandDo::getStatus, "open"))));
        assertEquals(List.of("Dave"), usersWithoutOpenDemand.stream().map(MysqlUserDo::getUsername).toList());

        List<MysqlUserDo> usersInSubQuery = userService.list(where -> where
            .inSubSql(MysqlUserDo::getId, sub -> sub.from(MysqlOrderDo.class)
                .select(s -> s.select(MysqlOrderDo::getUserId, SingleValue::getValue))
                .where(sw -> sw.gt(MysqlOrderDo::getAmount, new BigDecimal("50.00")))));
        assertEquals(List.of("Alice", "Carol"), usersInSubQuery.stream().map(MysqlUserDo::getUsername).toList());

        List<UserStatsDto> derivedJoin = userService.listJoin(
            join -> join.innerJoin(
                sub -> sub.from(MysqlOrderDo.class)
                    .select(s -> s.select(MysqlOrderDo::getUserId)
                        .selectFunc(f -> f.sum(MysqlOrderDo::getAmount), OrderStatsDo::getTotalAmount))
                    .group(g -> g.groupBy(MysqlOrderDo::getUserId)),
                "os",
                on -> on.eqColumn(MysqlUserDo::getId, null, OrderStatsDo::getUserId, "os")),
            where -> where.gt(OrderStatsDo::getTotalAmount, "os", new BigDecimal("100.00")),
            select -> select.select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(OrderStatsDo::getTotalAmount, "os", UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );
        assertEquals(List.of("Alice", "Carol"), derivedJoin.stream().map(UserStatsDto::getUsername).toList());

        assertSqlContains("exists (select");
        assertSqlContains("not exists (select");
        assertSqlContains(" in (select");
        assertSqlContains("inner join (select");
    }

    @Test
    void pageJoinAndPageGroupCoverDocsPaginationShapesOnDameng() {
        Page<UserStatsDto> joinPage = new Page<>(1, 2);
        IPage<UserStatsDto> result = userService.pageJoin(
            joinPage,
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select.select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getStatus, UserStatsDto::getRoleCode),
            UserStatsDto.class
        );
        assertEquals(4, result.getTotal());
        assertEquals(2, result.getRecords().size());

        Page<UserStatsDto> groupPage = new Page<>(1, 2);
        IPage<UserStatsDto> grouped = userService.pageGroup(
            groupPage,
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select.select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.count(), UserStatsDto::getOrderCount),
            UserStatsDto.class
        );
        assertEquals(3, grouped.getTotal());
        assertEquals(2, grouped.getRecords().size());
        assertSqlContains("count(*)");
        assertSqlContains("limit 2");
    }
}
