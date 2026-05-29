package com.baomidou.mybatisplus.extension.it.dm;

import com.baomidou.mybatisplus.extension.it.mysql.CapturedSql;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlDemandDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlOrderDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserDo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 写入路径全覆盖集成测试（达梦 DAMENG 方言）。
 * 覆盖场景：saveBatchWithoutId / saveDuplicate（MERGE INTO）/ saveIgnore（MERGE WHEN NOT MATCHED）/
 *           saveReplace（MERGE 全列覆盖）/ update / updateJoin / remove / 逻辑删除+withDeleted 查回。
 * 修复回归：M12（批量含 null 元素）/ M13（三种写入主体一致）/ H3（空条件抛异常）/
 *           M19（达梦侧 saveDuplicate/Replace 走 MERGE INTO 正确）
 */
class DmWriteCoverageIntegrationTest extends DmIntegrationTestBase {

    // -------------------------------------------------------------------------
    // saveBatchWithoutId
    // -------------------------------------------------------------------------

    @Test
    void saveBatchWithoutIdInsertsNewRowsAndReturnsCorrectCountOnDameng() {
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
    void saveBatchWithoutIdWithNullElementsSkipsNullAndInsertsNonNullOnDameng() {
        // M12 回归：集合中夹 null 元素，非 null 数据不能被静默漏插
        List<MysqlUserDo> withNulls = new ArrayList<>();
        withNulls.add(MysqlUserDo.of(20L, "Hannah", "user", 22, 50, "0.00", true, null, null, T0, 0));
        withNulls.add(null);
        withNulls.add(MysqlUserDo.of(21L, "Ivan", "user", 33, 70, "100.00", true, null, null, T0, 0));

        // 框架应跳过 null 元素，成功插入 2 条非 null 数据
        int inserted = userService.saveBatchWithoutId(withNulls);
        assertEquals(2, inserted, "M12：含 null 元素的集合，成功插入行数应为 2（跳过 null）");

        assertNotNull(userService.get(MysqlUserDo::getUsername, "Hannah"), "M12：Hannah 应被成功插入");
        assertNotNull(userService.get(MysqlUserDo::getUsername, "Ivan"), "M12：Ivan 应被成功插入");
    }

    // -------------------------------------------------------------------------
    // saveDuplicate（达梦：MERGE INTO ... WHEN MATCHED THEN UPDATE）
    // M19：DM 侧 saveDuplicate 走 MERGE INTO 正确
    // -------------------------------------------------------------------------

    @Test
    void saveDuplicateUsesMergeIntoOnDameng() {
        // M19 回归：达梦 saveDuplicate 应生成 MERGE INTO 语句而非 ON DUPLICATE KEY UPDATE
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
        assertEquals("auditor", frank.getRoleCode(), "MERGE INTO 冲突后应更新 role_code");
        assertEquals(60, frank.getCreditScore(), "MERGE INTO 冲突后 credit_score 应为 55+5=60");
        assertTrue(frank.getActive(), "MERGE INTO 冲突后 active 应为 true");

        // M19：达梦方言应生成 MERGE INTO 而非 INSERT ... ON DUPLICATE KEY UPDATE
        assertSqlContains("merge into");
        assertSqlContains("when matched then update");
        assertSqlContains("when not matched then insert");
    }

    @Test
    void saveDuplicateWithConsistentPrimaryKeyBodyOnDameng() {
        // M13 回归：三条数据主体一致——相同 id 应触发 MERGE INTO 更新
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(30L, "Jay", "user", 20, 40, "0.00", true, null, null, T0, 0)
        ));
        CapturedSql.clear();

        int rows = userService.saveDuplicate(List.of(
            MysqlUserDo.of(30L, "Jay", "user", 25, 45, "5.00", true, null, null, T0, 0)
        ), dup -> dup
            .duplicate(MysqlUserDo::getAge)
            .duplicate(MysqlUserDo::getCreditScore));
        assertTrue(rows >= 1);

        MysqlUserDo jay = userService.get(MysqlUserDo::getUsername, "Jay");
        assertEquals(25, jay.getAge(), "M13：MERGE INTO 后 age 应更新为 25");
        assertEquals(45, jay.getCreditScore(), "M13：MERGE INTO 后 credit_score 应更新为 45");
    }

    // -------------------------------------------------------------------------
    // saveIgnore（达梦：MERGE INTO ... WHEN NOT MATCHED THEN INSERT）
    // M19：DM 侧 saveIgnore 走 MERGE WHEN NOT MATCHED 正确
    // -------------------------------------------------------------------------

    @Test
    void saveIgnoreUsesMergeWhenNotMatchedOnDamengAndDoesNotOverwrite() {
        // M19 回归：达梦 saveIgnore 应生成 MERGE INTO ... WHEN NOT MATCHED THEN INSERT，
        // 不包含 WHEN MATCHED 子句（主键冲突时静默不更新）
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(10L, "Frank", "user", 23, 55, "10.00", true, null, null, T0, 0)
        ));
        CapturedSql.clear();

        int ignored = userService.saveIgnore(List.of(
            MysqlUserDo.of(10L, "FrankIgnored", "admin", 99, 99, "99.00", false, "x", null, T0, 0)
        ));
        assertEquals(0, ignored, "saveIgnore：主键冲突时返回 0");

        // 原行 username 不变
        assertEquals("Frank",
            userService.get(where -> where.eq(MysqlUserDo::getId, 10L)).getUsername(),
            "saveIgnore：达梦已有行的 username 不应被覆盖");

        assertSqlContains("merge into");
        assertSqlContains("when not matched then insert");
    }

    @Test
    void saveIgnoreInsertsNewRowWhenNoPrimaryKeyConflictOnDameng() {
        // 无主键冲突时 saveIgnore 正常插入
        int inserted = userService.saveIgnore(List.of(
            MysqlUserDo.of(40L, "Kai", "user", 28, 65, "200.00", true, null, null, T0, 0)
        ));
        assertEquals(1, inserted, "saveIgnore：无冲突时应插入 1 条");
        assertNotNull(userService.get(MysqlUserDo::getUsername, "Kai"));
    }

    // -------------------------------------------------------------------------
    // saveReplace（达梦：MERGE INTO 全列覆盖，含 WHEN MATCHED 和 WHEN NOT MATCHED）
    // M19：DM 侧 saveReplace 走 MERGE INTO 正确
    // -------------------------------------------------------------------------

    @Test
    void saveReplaceUsesMergeIntoWithFullColumnUpdateOnDameng() {
        // M19 回归：达梦 saveReplace 应生成 MERGE INTO，WHEN MATCHED 全列覆盖
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(10L, "Frank", "user", 23, 55, "10.00", true, "old", null, T0, 0)
        ));
        CapturedSql.clear();

        int replaced = userService.saveReplace(List.of(
            MysqlUserDo.of(10L, "FrankReplaced", "admin", 44, 77, "33.00", false, "new", null, T0, 0)
        ));
        assertTrue(replaced >= 1, "saveReplace 返回受影响行数应 >=1");

        MysqlUserDo replacedUser = userService.get(where -> where.eq(MysqlUserDo::getId, 10L));
        assertEquals("FrankReplaced", replacedUser.getUsername(), "replace 后 username 应被覆盖");
        assertEquals("admin", replacedUser.getRoleCode(), "replace 后 role_code 应被覆盖");
        assertEquals(0, new BigDecimal("33.00").compareTo(replacedUser.getBalance()),
            "replace 后 balance 应被覆盖");

        // M19：达梦方言 replace 应走 MERGE INTO
        assertSqlContains("merge into");
        assertSqlContains("when matched then update");
        assertSqlContains("when not matched then insert");
    }

    @Test
    void saveReplaceInsertsWhenNoConflictOnDameng() {
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
    void updateSetsSpecifiedColumnsMatchingWhereConditionOnDameng() {
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
    void updateJoinSetsColumnBasedOnJoinedTableConditionOnDameng() {
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
    void executableStreamUpdateThenVerifyChangedFieldsOnDameng() {
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
    void executableStreamUpdateEntityPatchesOnlySpecifiedColumnOnDameng() {
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
    void removeLogicallyDeletesRowAndMakesItInvisibleOnDameng() {
        // remove 走 @TableLogic 逻辑删除，删除后默认查询不可见，withDeleted 可见
        int removed = userService.remove(where -> where.eq(MysqlUserDo::getUsername, "Dave"));
        assertEquals(1, removed);
        assertNull(userService.get(MysqlUserDo::getUsername, "Dave"),
            "remove 后 Dave 默认查询应不可见");

        // withDeleted 查仍可见
        MysqlUserDo dave = userService.stream().withDeleted()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Dave"))
            .findFirst()
            .orElse(null);
        assertNotNull(dave, "remove 后 Dave 走 withDeleted 应仍可查（逻辑删除）");
        assertEquals(1, dave.getDeleted(), "逻辑删除后 deleted 字段应为 1");
    }

    @Test
    void removeByActiveConditionDeletesOnlyMatchingRowsOnDameng() {
        // active=false 的可见行：Carol（active=false, deleted=0）；Eve 已 deleted=1 不可见
        int removed = userService.remove(
            where -> where.eq(MysqlUserDo::getActive, false)
        );
        assertEquals(1, removed, "active=false 且可见行只有 Carol，应删除 1 条");
        assertNull(userService.get(MysqlUserDo::getUsername, "Carol"));
    }

    // -------------------------------------------------------------------------
    // M-03：executableStream() 无 filter 直接 execute* 应抛 IllegalStateException
    // -------------------------------------------------------------------------

    @Test
    void executableStreamExecuteDeleteWithoutFilterShouldThrowIllegalStateExceptionOnDameng() {
        // M-03：executableStream() 不加 filter 直接执行 executeDelete，
        // 护栏应抛 IllegalStateException 阻止全表删除
        assertThrows(IllegalStateException.class,
            () -> userService.executableStream().executeDelete(),
            "M-03：executeDelete() 无条件时应抛 IllegalStateException");
    }

    @Test
    void executableStreamExecuteUpdateWithoutFilterShouldThrowIllegalStateExceptionOnDameng() {
        // M-03：executableStream() 加 set 但不加 filter 直接执行 executeUpdate()，
        // 护栏应抛 IllegalStateException 阻止全表更新
        assertThrows(IllegalStateException.class,
            () -> userService.executableStream()
                .set(set -> set.set(MysqlUserDo::getRoleCode, "hacked"))
                .executeUpdate(),
            "M-03：executeUpdate() 无条件时应抛 IllegalStateException");
    }

    @Test
    void executableStreamExecuteUpdateEntityWithoutFilterShouldThrowIllegalStateExceptionOnDameng() {
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
    void updateJoinWithNullPredicateShouldThrowIllegalArgumentExceptionOnDameng() {
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
    void updateJoinWithNullSetterShouldThrowIllegalArgumentExceptionOnDameng() {
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
    void removeWithNullPredicateShouldThrowExceptionOnDameng() {
        // H3 回归：remove(null) 不应静默全表删除
        // 强转消歧：IRepository.remove(Wrapper) 与 IStreamService.remove(Consumer) 均接受 null，需明确指定重载
        assertThrows(Exception.class,
            () -> userService.remove((java.util.function.Consumer<com.baomidou.mybatisplus.extension.wrapper.NormalWhereLambdaQueryWrapper>) null),
            "H3：remove(null) 应抛出异常，不可全表删除");
    }

    @Test
    void updateWithEmptyConditionShouldThrowOrBeGuardedOnDameng() {
        // H3 回归：update 时 where 条件为空应抛出异常，防止全表更新
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
    void softDeletedRowIsInvisibleByDefaultAndVisibleWithWithDeletedOnDameng() {
        // 种子数据：Eve（id=5, deleted=1）已是逻辑删除状态
        assertNull(userService.get(MysqlUserDo::getUsername, "Eve"),
            "Eve 已逻辑删除，默认查询应不可见");

        MysqlUserDo eve = userService.stream().withDeleted()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Eve"))
            .findFirst()
            .orElse(null);
        assertNotNull(eve, "withDeleted 应能查到已逻辑删除的 Eve");
        assertEquals(1, eve.getDeleted());
    }

    @Test
    void restoreSoftDeletedRowMakesItVisibleAgainOnDameng() {
        // 把 Eve 的 deleted 改回 0，验证恢复后可正常查到
        int restored = userService.executableStream()
            .set(set -> set.set(MysqlUserDo::getDeleted, 0))
            .filter(where -> where.withDeleted().eq(MysqlUserDo::getUsername, "Eve"))
            .executeUpdate();
        assertEquals(1, restored, "恢复逻辑删除：应命中 1 行");

        assertNotNull(userService.get(MysqlUserDo::getUsername, "Eve"),
            "恢复后 Eve 应可正常查到");
    }

    @Test
    void softDeleteThenWithDeletedQueryShowsAllIncludingDeletedOnDameng() {
        // 逻辑删除 Dave 后，withDeleted 共 5 条，正常查询 3 条
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
    void removeAliceLogicallyLeavesChildOrdersIntactOnDameng() {
        // 逻辑删除 Alice（mps_user 有 @TableLogic，remove 打 deleted=1 不物理删行）
        // DM 无外键约束，但验证子表数据仍在
        int removed = userService.remove(where -> where.eq(MysqlUserDo::getUsername, "Alice"));
        assertEquals(1, removed);
        assertNull(userService.get(MysqlUserDo::getUsername, "Alice"));

        // Alice 的订单物理行仍存在
        List<MysqlOrderDo> aliceOrders = orderService.list(
            where -> where.eq(MysqlOrderDo::getUserId, 1L)
        );
        assertEquals(2, aliceOrders.size(), "Alice 的订单物理行应仍存在（逻辑删除不删子表）");
    }
}
