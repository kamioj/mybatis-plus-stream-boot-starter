package com.baomidou.mybatisplus.extension.it.mysql;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MysqlServiceWhereStreamIntegrationTest extends MysqlIntegrationTestBase {

    @Test
    void getListValueCountExistAndPageLikeDocs() {
        MysqlUserDo alice = userService.get(MysqlUserDo::getUsername, "Alice");
        assertEquals(1L, alice.getId());

        MysqlUserDo fallback = MysqlUserDo.of(99L, "fallback", "none", 0, 0, "0", false, null, null, T0, 0);
        assertSame(fallback, userService.getOrDefault(MysqlUserDo::getUsername, "missing", fallback));

        List<MysqlUserDo> activeUsers = userService.list(
            where -> where.eq(MysqlUserDo::getActive, true),
            order -> order.orderDesc(MysqlUserDo::getCreditScore),
            2
        );
        assertEquals(List.of("Alice", "Bob"), activeUsers.stream().map(MysqlUserDo::getUsername).toList());

        String role = userService.getValue(
            where -> where.eq(MysqlUserDo::getUsername, "Bob"),
            MysqlUserDo::getRoleCode
        );
        assertEquals("user", role);

        List<String> names = userService.listValues(
            where -> where.eq(MysqlUserDo::getRoleCode, "user"),
            order -> order.orderAsc(MysqlUserDo::getId),
            10,
            MysqlUserDo::getUsername
        );
        assertEquals(List.of("Bob", "Carol"), names);
        assertEquals(2, userService.count(where -> where.eq(MysqlUserDo::getRoleCode, "user")));
        assertTrue(userService.exist(where -> where.eq(MysqlUserDo::getUsername, "Carol")));
        assertFalse(userService.exist(where -> where.eq(MysqlUserDo::getUsername, "Nobody")));

        assertSqlContains("where (( mps_user.username=");
        assertSqlContains("order by mps_user.credit_score desc");
        assertSqlContains("limit 2");
        // M-02 (part1)：无分组 listValues 语义保留 DISTINCT，验证 SQL 确实含 select distinct
        assertSqlContains("select distinct");
    }

    @Test
    void whereWrapperCoversComparisonLikeNullRangeRegexpColumnAndLogicalGroups() {
        List<MysqlUserDo> users = userService.list(where -> where
            .ge(MysqlUserDo::getAge, 25)
            .lt(MysqlUserDo::getAge, 40)
            .between(MysqlUserDo::getCreditScore, 60, 100)
            .likeRight(MysqlUserDo::getUsername, "A")
            .isNull(MysqlUserDo::getManagerId)
            .regexp(MysqlUserDo::getTags, "mysql|admin"));
        assertEquals(1, users.size());
        assertEquals("Alice", users.get(0).getUsername());

        List<MysqlUserDo> grouped = userService.list(where -> where
            .and(w -> w.eq(MysqlUserDo::getRoleCode, "user").or().eq(MysqlUserDo::getRoleCode, "auditor"))
            .not(w -> w.eq(MysqlUserDo::getUsername, "Carol"))
            .in(MysqlUserDo::getId, 2L, 3L, 4L)
            .notIn(MysqlUserDo::getUsername, List.of("Nobody"))
            .containAny(MysqlUserDo::getTags, List.of("audit")));
        assertEquals(List.of("Dave"), grouped.stream().map(MysqlUserDo::getUsername).toList());

        List<MysqlUserDo> managers = userService.list(where -> where
            .eqColumn(MysqlUserDo::getManagerId, MysqlUserDo::getId));
        assertTrue(managers.isEmpty());

        assertSqlContains("regexp");
        assertSqlContains("not (");
        assertSqlContains(" in (");
    }

    @Test
    void streamTerminalOperationsCoverSqlAwareAndJdkLikePaths() {
        Optional<MysqlUserDo> first = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .sorted(order -> order.orderAsc(MysqlUserDo::getAge))
            .findFirst();
        assertTrue(first.isPresent());
        assertEquals("Bob", first.get().getUsername());

        Set<Long> userIds = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getRoleCode, "user"))
            .toSet(MysqlUserDo::getId);
        assertEquals(Set.of(2L, 3L), userIds);

        Map<Long, String> idToName = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .toMap(MysqlUserDo::getId, MysqlUserDo::getUsername);
        assertEquals("Alice", idToName.get(1L));
        assertEquals("Dave", idToName.get(4L));

        Map<String, Long> countByRole = userService.stream().toMapCount(MysqlUserDo::getRoleCode);
        assertEquals(1L, countByRole.get("admin"));
        assertEquals(2L, countByRole.get("user"));
        assertEquals(1L, countByRole.get("auditor"));

        assertTrue(userService.stream().filter(w -> w.eq(MysqlUserDo::getActive, true))
            .anyMatch(MysqlUserDo::getActive));
        assertTrue(userService.stream().filter(w -> w.eq(MysqlUserDo::getActive, true))
            .allMatch(MysqlUserDo::getActive));
        assertTrue(userService.stream().filter(w -> w.eq(MysqlUserDo::getActive, true))
            .noneMatch(user -> !user.getActive()));

        Integer ageSum = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .mapToInt(MysqlUserDo::getAge)
            .sum();
        assertEquals(90, ageSum);

        String reducedNames = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .map(MysqlUserDo::getUsername)
            .collect(Collectors.joining(","));
        assertEquals("Alice,Bob,Dave", reducedNames);

        Object[] activeArray = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .toArray();
        assertEquals(3, activeArray.length);

        List<String> peeked = new ArrayList<>();
        userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .peek(user -> peeked.add(user.getUsername()))
            .forEach(user -> {});
        assertEquals(List.of("Alice", "Bob", "Dave"), peeked);

        assertSqlContains("select id as mps_k");
        assertSqlContains("group by role_code");
    }

    @Test
    void skipLimitDistinctMapToColumnAndMapToValueArePushedToMysqlSql() {
        List<String> page = userService.stream()
            .mapToColumn(MysqlUserDo::getRoleCode)
            .distinct()
            .sorted(order -> order.orderAsc(MysqlUserDo::getRoleCode))
            .skip(1)
            .limit(2)
            .collect(Collectors.toList());
        assertEquals(List.of("auditor", "user"), page);

        Long activeCount = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .mapToValue(func -> func.count())
            .findFirst()
            .orElseThrow();
        assertEquals(3L, activeCount);

        assertSqlContains("select distinct");
        assertSqlContains("limit 1, 2");
        assertSqlContains("count");
    }

    @Test
    void forUpdateLockTailIsRenderedByMysqlDialect() {
        MysqlUserDo locked = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .forUpdate()
            .findFirst()
            .orElseThrow();

        assertEquals("Alice", locked.getUsername());
        assertSqlContains("for update");
    }
}
