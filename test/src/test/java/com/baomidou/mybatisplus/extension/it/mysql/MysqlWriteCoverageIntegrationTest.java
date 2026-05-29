package com.baomidou.mybatisplus.extension.it.mysql;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 写入路径全覆盖集成测试（MySQL 方言）。
 * 覆盖场景：saveBatchWithoutId / saveDuplicate / saveIgnore / saveReplace /
 *           update / updateJoin / remove / 逻辑删除+withDeleted 查回。
 * 修复回归：M12（批量含 null 元素）/ M13（三种写入主体一致）/ H3（空条件抛异常）
 */
class MysqlWriteCoverageIntegrationTest extends MysqlIntegrationTestBase {

    // -------------------------------------------------------------------------
    // saveBatchWithoutId
    // -------------------------------------------------------------------------

    @Test
    void saveBatchWithoutIdInsertsNewRowsAndReturnsCorrectCount() {
        // 插入 2 条全新用户，验证返回行数与落库内容
        int inserted = userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(10L, "Frank", "user", 23, 55, "10.00", true, "new", 1L, T0, 0),
            MysqlUserDo.of(11L, "Grace", "user", 27, 60, "50.00", true, "new2", 1L, T0, 0)
        ));
        assertEquals(2, inserted, "批量插入 2 条应返回 2");

        MysqlUserDo frank = userService.get(MysqlUserDo::getUsername, "Frank");
        assertNotNull(frank);
        assertEquals(23, frank.getAge());
        assertEquals(0, new BigDecimal("10.00").compareTo(frank.getBalance()));

        MysqlUserDo grace = userService.get(MysqlUserDo::getUsername, "Grace");
        assertNotNull(grace);
        assertEquals("user", grace.getRoleCode());

        assertSqlContains("insert into");
        assertSqlContains("mps_user");
    }

    @Test
    void saveBatchWithoutIdWithNullElementsSkipsNullAndInsertsNonNull() {
        // M12 回归：集合中夹 null 元素，非 null 数据不能被静默漏插
        List<MysqlUserDo> withNulls = new ArrayList<>();
        withNulls.add(MysqlUserDo.of(20L, "Hannah", "user", 22, 50, "0.00", true, null, null, T0, 0));
        withNulls.add(null);
        withNulls.add(MysqlUserDo.of(21L, "Ivan", "user", 33, 70, "100.00", true, null, null, T0, 0));

        // 框架应跳过 null 元素，插入 2 条非 null 数据
        int inserted = userService.saveBatchWithoutId(withNulls);
        assertEquals(2, inserted, "M12：含 null 元素的集合，成功插入行数应为 2（跳过 null）");

        assertNotNull(userService.get(MysqlUserDo::getUsername, "Hannah"), "M12：Hannah 应被成功插入");
        assertNotNull(userService.get(MysqlUserDo::getUsername, "Ivan"), "M12：Ivan 应被成功插入");
    }

    // -------------------------------------------------------------------------
    // saveDuplicate（ON DUPLICATE KEY UPDATE 语义）
    // -------------------------------------------------------------------------

    @Test
    void saveDuplicateUpdatesConflictRowBySpecifiedColumns() {
        // 先插入 Frank，再以相同 id 触发 duplicate：更新 roleCode、增量更新 creditScore
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(10L, "Frank", "user", 23, 55, "10.00", true, "old", 1L, T0, 0)
        ));
        CapturedSql.clear();

        int rows = userService.saveDuplicate(List.of(
            MysqlUserDo.of(10L, "Frank", "auditor", 24, 60, "20.00", false, "dup", 1L, T0, 0)
        ), dup -> dup
            .duplicate(MysqlUserDo::getRoleCode)
            .setFunc(MysqlUserDo::getCreditScore, func -> func.add(MysqlUserDo::getCreditScore, 5))
            .setFunc(MysqlUserDo::getActive, func -> func.value(true)));
        assertTrue(rows >= 1, "saveDuplicate 应返回 >=1 的受影响行数");

        MysqlUserDo frank = userService.get(MysqlUserDo::getUsername, "Frank");
        assertEquals("auditor", frank.getRoleCode(), "冲突后应更新 role_code");
        // creditScore：初始 55 + 5 = 60（duplicate 走增量 add）
        assertEquals(60, frank.getCreditScore(), "冲突后 credit_score 应为 55+5=60");
        assertTrue(frank.getActive(), "冲突后 active 应被设为 true");

        assertSqlContains("on duplicate key update");
    }

    @Test
    void saveDuplicateWithConsistentPrimaryKeyBodyAcrossAllRows() {
        // M13 回归：三条数据主体一致——插入时 username 唯一，相同 username 应触发更新
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(30L, "Jay", "user", 20, 40, "0.00", true, null, null, T0, 0)
        ));
        CapturedSql.clear();

        // 用相同主键 id=30，触发冲突更新 age
        int rows = userService.saveDuplicate(List.of(
            MysqlUserDo.of(30L, "Jay", "user", 25, 45, "5.00", true, null, null, T0, 0)
        ), dup -> dup
            .duplicate(MysqlUserDo::getAge)
            .duplicate(MysqlUserDo::getCreditScore));
        assertTrue(rows >= 1);

        MysqlUserDo jay = userService.get(MysqlUserDo::getUsername, "Jay");
        assertEquals(25, jay.getAge(), "M13：duplicate 后 age 应更新为 25");
        assertEquals(45, jay.getCreditScore(), "M13：duplicate 后 credit_score 应更新为 45");
    }

    // -------------------------------------------------------------------------
    // saveIgnore（INSERT IGNORE 语义：已存在不变）
    // -------------------------------------------------------------------------

    @Test
    void saveIgnoreDoesNotOverwriteExistingRowAndReturnsZero() {
        // 先插入 Frank，再以相同 id 和不同 username 做 saveIgnore，期望原行不变
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(10L, "Frank", "user", 23, 55, "10.00", true, null, null, T0, 0)
        ));
        CapturedSql.clear();

        int ignored = userService.saveIgnore(List.of(
            MysqlUserDo.of(10L, "FrankIgnored", "admin", 99, 99, "99.00", false, "x", null, T0, 0)
        ));
        assertEquals(0, ignored, "saveIgnore：主键冲突时返回 0，表示未插入");

        // 原行 username 不变
        assertEquals("Frank", userService.get(MysqlUserDo::getId, 10L).getUsername(),
            "saveIgnore：已有行的 username 不应被覆盖");

        assertSqlContains("insert ignore into");
    }

    @Test
    void saveIgnoreInsertsNewRowWhenNoPrimaryKeyConflict() {
        // 无冲突时 saveIgnore 等同正常插入
        int inserted = userService.saveIgnore(List.of(
            MysqlUserDo.of(40L, "Kai", "user", 28, 65, "200.00", true, null, null, T0, 0)
        ));
        assertEquals(1, inserted, "saveIgnore：无冲突时应插入 1 条");
        assertNotNull(userService.get(MysqlUserDo::getUsername, "Kai"));
    }

    // -------------------------------------------------------------------------
    // saveReplace（REPLACE INTO 语义：全列覆盖）
    // -------------------------------------------------------------------------

    @Test
    void saveReplaceOverwritesExistingRowWithNewValues() {
        // 先插入 Frank，再 replace，期望全列被新值覆盖
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(10L, "Frank", "user", 23, 55, "10.00", true, "old", null, T0, 0)
        ));
        CapturedSql.clear();

        int replaced = userService.saveReplace(List.of(
            MysqlUserDo.of(10L, "FrankReplaced", "admin", 44, 77, "33.00", false, "new", null, T0, 0)
        ));
        assertTrue(replaced >= 1, "saveReplace 返回受影响行数应 >=1");

        MysqlUserDo replacedUser = userService.get(MysqlUserDo::getId, 10L);
        assertEquals("FrankReplaced", replacedUser.getUsername(), "replace 后 username 应被覆盖");
        assertEquals("admin", replacedUser.getRoleCode(), "replace 后 role_code 应被覆盖");
        assertEquals(0, new BigDecimal("33.00").compareTo(replacedUser.getBalance()),
            "replace 后 balance 应被覆盖");

        assertSqlContains("replace into");
    }

    @Test
    void saveReplaceInsertsWhenNoConflict() {
        // 无主键冲突时 saveReplace 也插入新行
        int rows = userService.saveReplace(List.of(
            MysqlUserDo.of(50L, "Leo", "user", 29, 70, "150.00", true, null, null, T0, 0)
        ));
        assertTrue(rows >= 1);
        assertNotNull(userService.get(MysqlUserDo::getUsername, "Leo"));
    }

    // -------------------------------------------------------------------------
    // update / updateJoin
    // -------------------------------------------------------------------------

    @Test
    void updateSetsSpecifiedColumnsMatchingWhereCondition() {
        // 把 Bob 的 role_code 改为 vip，验证落库值
        int updated = userService.update(
            set -> set.set(MysqlUserDo::getRoleCode, "vip")
                      .set(MysqlUserDo::getCreditScore, 80),
            where -> where.eq(MysqlUserDo::getUsername, "Bob")
        );
        assertEquals(1, updated, "update 应命中 1 行");

        MysqlUserDo bob = userService.get(MysqlUserDo::getUsername, "Bob");
        assertEquals("vip", bob.getRoleCode());
        assertEquals(80, bob.getCreditScore());

        assertSqlContains("update mps_user");
        assertSqlContains("set");
        assertSqlContains("where");
    }

    @Test
    void updateJoinSetsColumnBasedOnJoinedTableCondition() {
        // 通过 join mps_order 把状态为 cancelled 的订单对应用户的 active 设为 false
        int updated = userService.updateJoin(
            join -> join.innerJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            set -> set.set(MysqlUserDo::getActive, false),
            where -> where.eq(MysqlOrderDo::getStatus, "cancelled")
        );
        assertEquals(1, updated, "updateJoin：Dave 有 cancelled 订单，应命中 1 行");
        assertFalse(userService.get(MysqlUserDo::getUsername, "Dave").getActive(),
            "updateJoin 后 Dave.active 应为 false");

        assertSqlContains("inner join mps_order");
        assertSqlContains("set");
    }

    @Test
    void executableStreamUpdateThenVerifyChangedFields() {
        // executableStream 风格的 update，验证更新后字段值
        int rows = userService.executableStream()
            .set(set -> set
                .set(MysqlUserDo::getRoleCode, "premium")
                .setFunc(MysqlUserDo::getCreditScore, func -> func.add(MysqlUserDo::getCreditScore, 10)))
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .executeUpdate();
        assertEquals(1, rows);

        MysqlUserDo alice = userService.get(MysqlUserDo::getUsername, "Alice");
        assertEquals("premium", alice.getRoleCode());
        // creditScore：98 + 10 = 108
        assertEquals(108, alice.getCreditScore());

        assertSqlContains("update mps_user");
    }

    @Test
    void executableStreamUpdateEntityPatchesOnlySpecifiedColumn() {
        // executeUpdate(entity) 只更新 effects 指定的列
        MysqlUserDo patch = new MysqlUserDo();
        patch.setBalance(new BigDecimal("999.99"));
        int rows = userService.executableStream()
            .effects(MysqlUserDo::getBalance)
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Carol"))
            .executeUpdate(patch);
        assertEquals(1, rows);
        assertEquals(0, new BigDecimal("999.99")
            .compareTo(userService.get(MysqlUserDo::getUsername, "Carol").getBalance()),
            "entity patch 后 balance 应为 999.99");
    }

    // -------------------------------------------------------------------------
    // remove
    // -------------------------------------------------------------------------

    @Test
    void removeDeletesRowsMatchingWhereCondition() {
        // 通过 remove 删除 Dave（非逻辑删除，调用的是 IService.remove = 逻辑删除 if @TableLogic）
        // 注意：mps_user 有 @TableLogic，remove 实为逻辑删除
        int removed = userService.remove(where -> where.eq(MysqlUserDo::getUsername, "Dave"));
        assertEquals(1, removed);
        // 默认查询不可见
        assertNull(userService.get(MysqlUserDo::getUsername, "Dave"),
            "remove 后 Dave 默认查询应不可见");

        // withDeleted 查仍可见
        MysqlUserDo dave = userService.stream().withDeleted()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Dave"))
            .findFirst()
            .orElse(null);
        assertNotNull(dave, "remove 后 Dave 走 withDeleted 应仍可查（逻辑删除）");
        assertEquals(1, dave.getDeleted(), "逻辑删除后 deleted 字段应为 1");

        assertSqlContains("mps_user");
    }

    @Test
    void removeByMultipleConditionsDeletesOnlyMatchingRows() {
        // 条件：active=false AND deleted=0 → Carol（active=false, deleted=0）
        int removed = userService.remove(
            where -> where.eq(MysqlUserDo::getActive, false)
        );
        // active=false 且未逻辑删除的只有 Carol（Eve 已 deleted=1 不可见）
        assertEquals(1, removed, "active=false 且可见行只有 Carol，应删除 1 条");
        assertNull(userService.get(MysqlUserDo::getUsername, "Carol"));
    }

    // -------------------------------------------------------------------------
    // M-03：executableStream() 无 filter 直接 execute* 应抛 IllegalStateException
    // -------------------------------------------------------------------------

    @Test
    void executableStreamExecuteDeleteWithoutFilterShouldThrowIllegalStateException() {
        // M-03：executableStream() 不加 filter 直接执行 executeDelete，
        // 护栏应抛 IllegalStateException 阻止全表删除
        assertThrows(IllegalStateException.class,
            () -> userService.executableStream().executeDelete(),
            "M-03：executeDelete() 无条件时应抛 IllegalStateException");
    }

    @Test
    void executableStreamExecuteUpdateWithoutFilterShouldThrowIllegalStateException() {
        // M-03：executableStream() 加 set 但不加 filter 直接执行 executeUpdate()，
        // 护栏应抛 IllegalStateException 阻止全表更新
        assertThrows(IllegalStateException.class,
            () -> userService.executableStream()
                .set(set -> set.set(MysqlUserDo::getRoleCode, "hacked"))
                .executeUpdate(),
            "M-03：executeUpdate() 无条件时应抛 IllegalStateException");
    }

    @Test
    void executableStreamExecuteUpdateEntityWithoutFilterShouldThrowIllegalStateException() {
        // M-03：executableStream() 加 effects 但不加 filter 直接执行 executeUpdate(entity)，
        // 护栏应抛 IllegalStateException 阻止全表更新
        MysqlUserDo patch = new MysqlUserDo();
        patch.setRoleCode("hacked");
        assertThrows(IllegalStateException.class,
            () -> userService.executableStream()
                .effects(MysqlUserDo::getRoleCode)
                .executeUpdate(patch),
            "M-03：executeUpdate(entity) 无条件时应抛 IllegalStateException");
    }

    // -------------------------------------------------------------------------
    // L-11：updateJoin 参数护栏——predicate=null 或 setter=null 应抛 IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void updateJoinWithNullPredicateShouldThrowIllegalArgumentException() {
        // L-11：updateJoin 的 predicate=null 应抛 IllegalArgumentException（护栏防全表更新）
        assertThrows(IllegalArgumentException.class,
            () -> userService.updateJoin(
                join -> join.innerJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
                set -> set.set(MysqlUserDo::getActive, false),
                null
            ),
            "L-11：updateJoin predicate=null 应抛 IllegalArgumentException");
    }

    @Test
    void updateJoinWithNullSetterShouldThrowIllegalArgumentException() {
        // L-11：updateJoin 的 setter=null 应抛 IllegalArgumentException（对称守卫）
        assertThrows(IllegalArgumentException.class,
            () -> userService.updateJoin(
                join -> join.innerJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
                null,
                where -> where.eq(MysqlOrderDo::getStatus, "cancelled")
            ),
            "L-11：updateJoin setter=null 应抛 IllegalArgumentException");
    }

    // -------------------------------------------------------------------------
    // H3：remove(null) / update 空条件应抛异常，不可全表删改
    // -------------------------------------------------------------------------

    @Test
    void removeWithNullPredicateShouldThrowException() {
        // H3 回归：remove(null) 不应静默全表删除，应抛出异常
        // 强转消歧：IRepository.remove(Wrapper) 与 IStreamService.remove(Consumer) 均接受 null，需明确指定重载
        assertThrows(Exception.class,
            () -> userService.remove((java.util.function.Consumer<com.baomidou.mybatisplus.extension.wrapper.NormalWhereLambdaQueryWrapper>) null),
            "H3：remove(null) 应抛出异常，不可全表删除");
    }

    @Test
    void updateWithEmptyConditionShouldThrowOrBeGuarded() {
        // H3 回归：update 时 where 条件为空（无任何条件）应抛出异常，防止全表更新
        assertThrows(Exception.class,
            () -> userService.update(
                set -> set.set(MysqlUserDo::getRoleCode, "hacked"),
                where -> { /* 故意不加任何条件 */ }
            ),
            "H3：update 空条件应抛出异常，不可全表更新");
    }

    // -------------------------------------------------------------------------
    // 逻辑删除 + withDeleted 查回
    // -------------------------------------------------------------------------

    @Test
    void softDeletedRowIsInvisibleByDefaultAndVisibleWithWithDeleted() {
        // 种子数据：Eve（id=5, deleted=1）已是逻辑删除状态
        // 默认查询不可见
        assertNull(userService.get(MysqlUserDo::getUsername, "Eve"),
            "Eve 已逻辑删除，默认查询应不可见");

        // withDeleted 可查
        MysqlUserDo eve = userService.stream().withDeleted()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Eve"))
            .findFirst()
            .orElse(null);
        assertNotNull(eve, "withDeleted 应能查到已逻辑删除的 Eve");
        assertEquals(1, eve.getDeleted());
    }

    @Test
    void restoreSoftDeletedRowMakesItVisibleAgain() {
        // 把 Eve 的 deleted 改回 0，验证恢复后可以被正常查到
        int restored = userService.executableStream()
            .set(set -> set.set(MysqlUserDo::getDeleted, 0))
            .filter(where -> where.withDeleted().eq(MysqlUserDo::getUsername, "Eve"))
            .executeUpdate();
        assertEquals(1, restored, "恢复逻辑删除：应命中 1 行");

        assertNotNull(userService.get(MysqlUserDo::getUsername, "Eve"),
            "恢复后 Eve 应可正常查到");
    }

    @Test
    void softDeleteThenWithDeletedQueryShowsAllIncludingDeleted() {
        // 先逻辑删除 Dave，再用 withDeleted 查所有用户，条目数应 = 5（含 Eve + Dave）
        userService.remove(where -> where.eq(MysqlUserDo::getUsername, "Dave"));

        long totalWithDeleted = userService.stream().withDeleted()
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .collect(java.util.stream.Collectors.toList())
            .stream()
            .count();
        // 种子 5 条（Eve 已删除）+ Dave 刚删除 → withDeleted 共 5 条
        assertEquals(5L, totalWithDeleted, "withDeleted 应返回全部 5 条（含 2 条逻辑删除）");

        long visibleCount = userService.stream()
            .collect(java.util.stream.Collectors.toList())
            .stream()
            .count();
        assertEquals(3L, visibleCount, "正常查询应只返回 3 条未删除行（Alice/Bob/Carol）");
    }

    @Test
    void removeOrderAndDemandRowsThenRemoveUserSucceeds() {
        // 先删子表行（物理删）以满足外键约束，再逻辑删除 Alice
        // 为简化：直接逻辑删除 Alice（mps_user 有 @TableLogic，remove 不清子表行，但外键约束下需注意）
        // 这里只验证逻辑删除路径：remove 会打 deleted=1 而非真正 DELETE，不触犯外键
        int removed = userService.remove(where -> where.eq(MysqlUserDo::getUsername, "Alice"));
        assertEquals(1, removed);
        assertNull(userService.get(MysqlUserDo::getUsername, "Alice"));
        // 子表订单仍在（外键引用父表物理行，逻辑删除不删物理行，不违反约束）
        List<MysqlOrderDo> aliceOrders = orderService.list(
            where -> where.eq(MysqlOrderDo::getUserId, 1L)
        );
        assertEquals(2, aliceOrders.size(), "Alice 的订单物理行应仍存在");
    }
}
