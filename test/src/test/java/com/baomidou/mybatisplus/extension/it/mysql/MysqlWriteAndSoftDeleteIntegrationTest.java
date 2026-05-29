package com.baomidou.mybatisplus.extension.it.mysql;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MysqlWriteAndSoftDeleteIntegrationTest extends MysqlIntegrationTestBase {

    @Test
    void saveBatchDuplicateIgnoreAndReplaceUseMysqlWriteModes() {
        int inserted = userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(10L, "Frank", "user", 23, 55, "10.00", true, "new", 1L, T0, 0)
        ));
        assertEquals(1, inserted);

        int duplicateRows = userService.saveDuplicate(List.of(
            MysqlUserDo.of(10L, "Frank", "auditor", 24, 60, "20.00", false, "dup", 1L, T0, 0)
        ), duplicate -> duplicate
            .duplicate(MysqlUserDo::getRoleCode)
            .setFunc(MysqlUserDo::getCreditScore, func -> func.add(MysqlUserDo::getCreditScore, 5))
            .setFunc(MysqlUserDo::getActive, func -> func.value(true)));
        assertTrue(duplicateRows >= 1);
        MysqlUserDo frank = userService.get(MysqlUserDo::getUsername, "Frank");
        assertEquals("auditor", frank.getRoleCode());
        assertEquals(60, frank.getCreditScore());
        assertTrue(frank.getActive());

        int ignored = userService.saveIgnore(List.of(
            MysqlUserDo.of(10L, "FrankIgnored", "user", 99, 99, "99.00", false, "ignored", null, T0, 0)
        ));
        assertEquals(0, ignored);
        assertEquals("Frank", userService.get(MysqlUserDo::getId, 10L).getUsername());

        int replaced = userService.saveReplace(List.of(
            MysqlUserDo.of(10L, "FrankReplaced", "admin", 44, 77, "33.00", false, "replaced", null, T0, 0)
        ));
        assertTrue(replaced >= 1);
        MysqlUserDo replacedUser = userService.get(MysqlUserDo::getId, 10L);
        assertEquals("FrankReplaced", replacedUser.getUsername());
        assertEquals("admin", replacedUser.getRoleCode());

        assertSqlContains("insert into");
        assertSqlContains("on duplicate key update");
        assertSqlContains("insert ignore into");
        assertSqlContains("replace into");
    }

    @Test
    void executableStreamUpdateEntityUpdateJoinAndDeleteCoverWriteDocs() {
        int updated = userService.executableStream()
            .set(set -> set
                .set(MysqlUserDo::getRoleCode, "vip")
                .setFunc(MysqlUserDo::getCreditScore, func -> func.add(MysqlUserDo::getCreditScore, 2)))
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Bob"))
            .executeUpdate();
        assertEquals(1, updated);
        MysqlUserDo bob = userService.get(MysqlUserDo::getUsername, "Bob");
        assertEquals("vip", bob.getRoleCode());
        assertEquals(74, bob.getCreditScore());

        MysqlUserDo patch = new MysqlUserDo();
        patch.setBalance(new BigDecimal("999.00"));
        int entityUpdated = userService.executableStream()
            .effects(MysqlUserDo::getBalance)
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Carol"))
            .executeUpdate(patch);
        assertEquals(1, entityUpdated);
        assertEquals(0, new BigDecimal("999.00")
            .compareTo(userService.get(MysqlUserDo::getUsername, "Carol").getBalance()));

        int joinUpdated = userService.updateJoin(
            join -> join.innerJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            set -> set.set(MysqlUserDo::getActive, false),
            where -> where.eq(MysqlOrderDo::getStatus, "cancelled"));
        assertEquals(1, joinUpdated);
        assertFalse(userService.get(MysqlUserDo::getUsername, "Dave").getActive());

        int deleted = userService.executableStream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Dave"))
            .executeDelete();
        assertEquals(1, deleted);
        assertNull(userService.get(MysqlUserDo::getUsername, "Dave"));
        assertNotNull(userService.stream().withDeleted()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Dave"))
            .findFirst()
            .orElse(null));

        assertSqlContains("update mps_user");
        assertSqlContains("inner join mps_order");
        assertSqlContains("set");
        assertSqlContains("deleted");
    }

    @Test
    void logicalDeleteDefaultFilterWithDeletedAndRestoreFlow() {
        List<String> visibleNames = userService.list(where -> where.isNotNull(MysqlUserDo::getId)).stream()
            .map(MysqlUserDo::getUsername)
            .toList();
        assertFalse(visibleNames.contains("Eve"));

        List<String> allNames = userService.stream().withDeleted()
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .map(MysqlUserDo::getUsername)
            .collect(java.util.stream.Collectors.toList());
        assertTrue(allNames.contains("Eve"));

        int restored = userService.executableStream()
            .set(set -> set.set(MysqlUserDo::getDeleted, 0))
            .filter(where -> where.withDeleted().eq(MysqlUserDo::getUsername, "Eve"))
            .executeUpdate();
        assertEquals(1, restored);
        assertNotNull(userService.get(MysqlUserDo::getUsername, "Eve"));

        int removed = userService.remove(where -> where.eq(MysqlUserDo::getUsername, "Eve"));
        assertEquals(1, removed);
        assertNull(userService.get(MysqlUserDo::getUsername, "Eve"));

        assertSqlContains("`deleted` = 0");
    }
}
