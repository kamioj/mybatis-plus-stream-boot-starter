package com.baomidou.mybatisplus.extension.it.dm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlOrderDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserDo;
import com.baomidou.mybatisplus.extension.it.mysql.UserStatsDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DmCoreIntegrationTest extends DmIntegrationTestBase {

    @Test
    void serviceWhereJoinGroupPageAndStreamAggregatesWorkOnDameng() {
        MysqlUserDo alice = userService.get(where -> where.eq(MysqlUserDo::getUsername, "Alice"));
        assertNotNull(alice);
        assertEquals("admin", alice.getRoleCode());

        List<MysqlUserDo> users = userService.list(where -> where
            .ge(MysqlUserDo::getCreditScore, 70)
            .likeRight(MysqlUserDo::getUsername, "A")
            .or(or -> or.eq(MysqlUserDo::getRoleCode, "user")
                .containAny(MysqlUserDo::getTags, List.of("ops"))));
        assertEquals(Set.of("Alice", "Bob"),
            users.stream().map(MysqlUserDo::getUsername).collect(Collectors.toSet()));

        IPage<UserStatsDto> grouped = userService.pageGroupJoin(
            new Page<>(1, 2),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select.select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.count(MysqlOrderDo::getId), UserStatsDto::getOrderCount)
                .selectFunc(func -> func.sum(MysqlOrderDo::getAmount), UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );
        assertEquals(3, grouped.getTotal());
        assertEquals(2, grouped.getRecords().size());

        Map<String, BigDecimal> sumByRole = userService.stream()
            .toMapSum(MysqlUserDo::getRoleCode, MysqlUserDo::getBalance);
        assertEquals(0, new BigDecimal("1080.25").compareTo(sumByRole.get("user")));

        assertSqlContains("left join \"mps_order\"");
        assertSqlContains("group by \"mps_user\".\"role_code\"");
        assertSqlContains("limit 2");
        assertSqlContains("sum(\"balance\")");
        assertSqlContains("regexp_like");
    }

    @Test
    void damengGroupConcatAndMergeWriteModesWork() {
        List<UserStatsDto> stats = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select.select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.groupConcat(MysqlUserDo::getUsername, "|"), UserStatsDto::getUsernames),
            UserStatsDto.class
        );
        UserStatsDto user = stats.stream()
            .filter(row -> "user".equals(row.getRoleCode()))
            .findFirst()
            .orElseThrow();
        assertTrue(user.getUsernames().contains("Bob"));
        assertTrue(user.getUsernames().contains("Carol"));
        assertSqlContains("listagg");

        int inserted = userService.saveIgnore(List.of(
            MysqlUserDo.of(2L, "Bob should ignore", "user", 99, 99, "1.00", true, null, null, T0, 0),
            MysqlUserDo.of(6L, "Frank", "guest", 22, 55, "20.00", true, "new", null, T0, 0)
        ));
        assertEquals(1, inserted);
        assertEquals("Bob", userService.get(where -> where.eq(MysqlUserDo::getId, 2L)).getUsername());
        assertEquals("Frank", userService.get(where -> where.eq(MysqlUserDo::getId, 6L)).getUsername());
        assertSqlContains("merge into");
    }
}
