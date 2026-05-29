package com.baomidou.mybatisplus.extension.it.dm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.bo.key.BiMapKey;
import com.baomidou.mybatisplus.extension.bo.key.MapKey3;
import com.baomidou.mybatisplus.extension.bo.key.MapKey4;
import com.baomidou.mybatisplus.extension.bo.key.MapKey5;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlOrderDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserDo;
import com.baomidou.mybatisplus.extension.it.mysql.UserStatsDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GroupAggregate 场景桶（达梦方言版）：分组/聚合/Map 全覆盖
 *
 * 覆盖场景：
 *   - group/having（DAMENG 方言）
 *   - toMapCount / toMapSum / toMapAvg / toMapMax / toMapMin
 *   - toMap(k,v) / toSet
 *   - groupingBy（应用层）
 *   - listGroup / listGroupValues
 *   - 多列分组 MapKey2..5（尤其 5 键）
 *
 * 回归点：
 *   M15 - MapKey5 五键分组：第5个 key 不同时必须分到不同桶（hashCode 含 key5）
 *   M7  - count() 在 select 含聚合函数时 total 正确
 *   M10 - 同一 stream 实例先后两次 toMapXxx，第二次不因 GROUP BY 叠加而错乱
 */
class DmGroupAggregateCoverageIntegrationTest extends DmIntegrationTestBase {

    // =====================================================================
    // 一、toMapCount / toMapSum / toMapAvg / toMapMax / toMapMin
    // =====================================================================

    @Test
    void toMapCountGroupsByRoleCodeAndReturnsCorrectCountsOnDameng() {
        // 种子数据：admin=Alice(1), user=Bob+Carol(2), auditor=Dave(1); Eve 逻辑删除不算
        Map<String, Long> countByRole = userService.stream().toMapCount(MysqlUserDo::getRoleCode);

        assertEquals(1L, countByRole.get("admin"));
        assertEquals(2L, countByRole.get("user"));
        assertEquals(1L, countByRole.get("auditor"));
        assertNull(countByRole.get(null));
        assertSqlContains("count(*)");
        assertSqlContains("group by role_code");
    }

    @Test
    void toMapSumGroupsBalanceByRoleCodeCorrectlyOnDameng() {
        // admin: Alice=1200.50; user: Bob(300.00)+Carol(780.25)=1080.25; auditor: Dave=90.00
        Map<String, BigDecimal> sumByRole = userService.stream()
                .toMapSum(MysqlUserDo::getRoleCode, MysqlUserDo::getBalance);

        assertEquals(0, new BigDecimal("1200.50").compareTo(sumByRole.get("admin")));
        assertEquals(0, new BigDecimal("1080.25").compareTo(sumByRole.get("user")));
        assertEquals(0, new BigDecimal("90.00").compareTo(sumByRole.get("auditor")));
        assertSqlContains("sum(balance)");
        assertSqlContains("group by role_code");
    }

    @Test
    void toMapAvgGroupsCreditScoreByRoleCodeCorrectlyOnDameng() {
        // user: (Bob=72 + Carol=88) / 2 = 80.0; admin: Alice=98.0; auditor: Dave=65.0
        Map<String, Double> avgByRole = userService.stream()
                .toMapAvg(MysqlUserDo::getRoleCode, MysqlUserDo::getCreditScore);

        assertEquals(80.0, avgByRole.get("user"));
        assertEquals(98.0, avgByRole.get("admin"));
        assertEquals(65.0, avgByRole.get("auditor"));
        assertSqlContains("avg(credit_score)");
        assertSqlContains("group by role_code");
    }

    @Test
    void toMapMaxAndMinGroupCreditScoreByRoleCodeCorrectlyOnDameng() {
        // user: max=Carol(88), min=Bob(72)
        Map<String, Integer> maxByRole = userService.stream()
                .toMapMax(MysqlUserDo::getRoleCode, MysqlUserDo::getCreditScore);
        Map<String, Integer> minByRole = userService.stream()
                .toMapMin(MysqlUserDo::getRoleCode, MysqlUserDo::getCreditScore);

        assertEquals(88, maxByRole.get("user"));
        assertEquals(72, minByRole.get("user"));
        assertEquals(98, maxByRole.get("admin"));
        assertEquals(98, minByRole.get("admin"));
        assertSqlContains("max(credit_score)");
        assertSqlContains("min(credit_score)");
    }

    // =====================================================================
    // 二、toMap(k, v) / toSet
    // =====================================================================

    @Test
    void toMapReturnsIdToUsernameMappingOnDameng() {
        // filter active 用户，验证 toMap 投影两列
        Map<Long, String> idToName = userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getActive, true))
                .toMap(MysqlUserDo::getId, MysqlUserDo::getUsername);

        assertEquals("Alice", idToName.get(1L));
        assertEquals("Bob", idToName.get(2L));
        assertEquals("Dave", idToName.get(4L));
        assertFalse(idToName.containsKey(3L));   // Carol active=false
        assertFalse(idToName.containsKey(5L));   // Eve 逻辑删除
        assertSqlContains("active");
    }

    @Test
    void toSetReturnsDistinctRoleCodesOnDameng() {
        // 3 种 role：admin, user, auditor（Eve 逻辑删除不算）
        java.util.Set<String> roles = userService.stream().toSet(MysqlUserDo::getRoleCode);

        assertEquals(3, roles.size());
        assertTrue(roles.contains("admin"));
        assertTrue(roles.contains("user"));
        assertTrue(roles.contains("auditor"));
    }

    /**
     * L-09：toSet(布尔列) 在达梦方言下的 Byte→Boolean coerce 验证。
     * 可见用户 active 值：Alice=true, Bob=true, Carol=false, Dave=true（Eve 逻辑删除不计）。
     * 修复前：达梦 TINYINT 列经 JDBC 返回 Byte，toSet 结果集元素类型为 Byte，
     * Boolean.TRUE.equals(element) 恒为 false。
     * 修复后：结果映射层将 Byte 强制转换为 Boolean，Set 中元素运行时类型应为 Boolean。
     */
    @Test
    void toSetWithBooleanColumnReturnsBooleanElementsOnDameng() {
        java.util.Set<Boolean> activeSet = userService.stream().toSet(MysqlUserDo::getActive);

        // 可见用户 active 有两个不同值：true 和 false
        assertTrue(activeSet.contains(Boolean.TRUE),
                "L-09：toSet(active) 应包含 Boolean.TRUE，达梦 Byte→Boolean coerce 必须生效");
        assertTrue(activeSet.contains(Boolean.FALSE),
                "L-09：toSet(active) 应包含 Boolean.FALSE，达梦 Byte→Boolean coerce 必须生效");
        // 不应含 null（4 个可见用户的 active 均非 null）
        assertFalse(activeSet.contains(null), "L-09：active 列无 null 行，toSet 不应包含 null");
        // 元素运行时类型应为 Boolean（修复前为 Byte，此处断言用 instanceof 语义等价的 Class 检查）
        for (Boolean element : activeSet) {
            // 若 coerce 未生效，此循环在 for-each 赋值时即因 ClassCastException 失败
            assertNotNull(element, "元素不应为 null");
        }
        // 集合大小应为 2（true 和 false 两种）
        assertEquals(2, activeSet.size(),
                "L-09：active 列只有两种值（true/false），toSet 应返回 size=2");
    }

    /**
     * Gap2 回归：toMap 值列为 boolean（active）——达梦 TINYINT→Byte 经结果映射层转回 Boolean。
     * 修复前 value 为 Byte，{@code Boolean.TRUE.equals(...)} 恒为 false。
     */
    @Test
    void toMapWithBooleanValueColumnReturnsBooleanOnDameng() {
        Map<Long, Boolean> activeById = userService.stream()
                .filter(where -> where.isNotNull(MysqlUserDo::getId))
                .toMap(MysqlUserDo::getId, MysqlUserDo::getActive);

        // 值已正确映射为 Boolean（修复前为 Byte）
        assertEquals(Boolean.TRUE, activeById.get(1L));   // Alice active
        assertEquals(Boolean.TRUE, activeById.get(2L));   // Bob active
        assertEquals(Boolean.FALSE, activeById.get(3L));  // Carol inactive
        assertEquals(Boolean.TRUE, activeById.get(4L));   // Dave active
    }

    /**
     * Gap2 回归：toMapCount 的 key 列为 boolean（active）——达梦 Byte key 转回 Boolean，
     * 否则 {@code get(Boolean.TRUE)} 取不到（key 实为 Byte）。
     */
    @Test
    void toMapCountWithBooleanKeyReturnsBooleanKeysOnDameng() {
        // 可见用户 active 分布：true=Alice,Bob,Dave(3)；false=Carol(1)（Eve 逻辑删除不计）
        Map<Boolean, Long> countByActive = userService.stream().toMapCount(MysqlUserDo::getActive);

        assertEquals(3L, countByActive.get(Boolean.TRUE));
        assertEquals(1L, countByActive.get(Boolean.FALSE));
        assertNull(countByActive.get(null));
    }

    // =====================================================================
    // 三、groupingBy（应用层，全量加载后分组）
    // =====================================================================

    @Test
    void groupingByRoleCodeReturnsCorrectBucketsOnDameng() {
        // groupingBy 在应用层分组，SQL 不下推 GROUP BY
        Map<String, List<MysqlUserDo>> byRole = userService.stream()
                .filter(where -> where.isNotNull(MysqlUserDo::getRoleCode))
                .groupingBy(MysqlUserDo::getRoleCode);

        assertEquals(3, byRole.size());
        assertEquals(1, byRole.get("admin").size());
        assertEquals(2, byRole.get("user").size());   // Bob + Carol
        assertEquals(1, byRole.get("auditor").size());
        assertEquals("Alice", byRole.get("admin").get(0).getUsername());
    }

    // =====================================================================
    // 四、listGroup / listGroupValues（带 HAVING + ORDER + LIMIT）
    // =====================================================================

    @Test
    void listGroupByRoleCodeReturnsAggregatedStatsPerRoleOnDameng() {
        // 按 role_code 分组，统计各组人数与均分
        List<UserStatsDto> rows = userService.listGroup(
                group -> group.groupBy(MysqlUserDo::getRoleCode),
                where -> where.isNotNull(MysqlUserDo::getId),
                order -> order.orderAsc(MysqlUserDo::getRoleCode),
                10,
                select -> select
                        .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                        .selectFunc(func -> func.count(MysqlUserDo::getId), UserStatsDto::getOrderCount)
                        .selectFunc(func -> func.avg(MysqlUserDo::getCreditScore), UserStatsDto::getAvgScore),
                UserStatsDto.class
        );

        assertEquals(3, rows.size());
        Map<String, UserStatsDto> byRole = rows.stream()
                .collect(Collectors.toMap(UserStatsDto::getRoleCode, r -> r));

        assertEquals(1L, byRole.get("admin").getOrderCount());
        assertEquals(98.0, byRole.get("admin").getAvgScore());
        assertEquals(2L, byRole.get("user").getOrderCount());
        assertEquals(80.0, byRole.get("user").getAvgScore());
        assertEquals(1L, byRole.get("auditor").getOrderCount());
        assertSqlContains("group by mps_user.role_code");
        assertSqlContains("avg(mps_user.credit_score)");
    }

    @Test
    void listGroupWithHavingFiltersGroupsCorrectlyOnDameng() {
        // HAVING count > 1 只应返回 user 组（2 人）
        List<UserStatsDto> rows = userService.listGroup(
                group -> group.groupBy(MysqlUserDo::getRoleCode)
                        .having(where -> where.gtFunc(
                                func -> func.count(MysqlUserDo::getId),
                                func -> func.value(1L))),
                where -> where.isNotNull(MysqlUserDo::getId),
                select -> select
                        .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                        .selectFunc(func -> func.count(MysqlUserDo::getId), UserStatsDto::getOrderCount),
                UserStatsDto.class
        );

        assertEquals(1, rows.size());
        assertEquals("user", rows.get(0).getRoleCode());
        assertEquals(2L, rows.get(0).getOrderCount());
        assertSqlContains("having");
        assertSqlContains("count(");
    }

    @Test
    void listGroupValuesReturnsListOfSumBalancePerRoleOnDameng() {
        // 按 role_code 分组 sum(balance)，验证 BigDecimal 列表包含正确值
        // 达梦版不强制排序（DM 排序行为与 MySQL 可能不同），仅验证 size 和数值存在
        List<BigDecimal> totals = userService.listGroupValues(
                group -> group.groupBy(MysqlUserDo::getRoleCode),
                where -> where.isNotNull(MysqlUserDo::getId),
                func -> func.sum(MysqlUserDo::getBalance)
        );

        assertEquals(3, totals.size());
        assertTrue(totals.stream().anyMatch(v -> v.compareTo(new BigDecimal("1200.50")) == 0));
        assertTrue(totals.stream().anyMatch(v -> v.compareTo(new BigDecimal("90.00")) == 0));
        assertTrue(totals.stream().anyMatch(v -> v.compareTo(new BigDecimal("1080.25")) == 0));
        assertSqlContains("sum(mps_user.balance)");
        assertSqlContains("group by mps_user.role_code");
    }

    /**
     * Gap3 回归：带 order + limit 的 listGroupValues（达梦版）。
     * <p>
     * 修复前：框架对 listGroupValues 固定叠加 DISTINCT，达梦在 {@code SELECT DISTINCT SUM(...) ...
     * ORDER BY role_code LIMIT n} 下报「ORDER BY 项不在 DISTINCT 查询项中」。
     * 修复后：分组场景不再叠加多余 DISTINCT（GROUP BY 已保证每组一行），达梦正常执行
     * GROUP BY + ORDER BY + LIMIT。
     */
    @Test
    void listGroupValuesWithOrderAndLimitReturnsSortedSubsetOnDameng() {
        // 按 role_code 升序取前 2 组的 sum(balance)：admin(1200.50), auditor(90.00)
        List<BigDecimal> totals = userService.listGroupValues(
                group -> group.groupBy(MysqlUserDo::getRoleCode),
                where -> where.isNotNull(MysqlUserDo::getId),
                order -> order.orderAsc(MysqlUserDo::getRoleCode),
                2,
                func -> func.sum(MysqlUserDo::getBalance)
        );

        assertEquals(2, totals.size());
        assertEquals(0, new BigDecimal("1200.50").compareTo(totals.get(0)));
        assertEquals(0, new BigDecimal("90.00").compareTo(totals.get(1)));
        assertSqlContains("sum(mps_user.balance)");
        assertSqlContains("group by mps_user.role_code");
        assertSqlContains("order by mps_user.role_code asc");
    }

    // =====================================================================
    // 五、多列分组 MapKey2..5（BiMapKey / MapKey3 / MapKey4 / MapKey5）
    // =====================================================================

    @Test
    void biMapKeyTwoColumnGroupingReturnsCorrectBucketsOnDameng() {
        // 按 (role_code, active) 二元组分组
        // 种子数据可见：admin+true=1(Alice), user+true=1(Bob), user+false=1(Carol), auditor+true=1(Dave)
        // Gap2 修复后：达梦 TINYINT(active)→Byte 在结果映射层转回 Boolean，key2 可直接作 Boolean 断言
        List<BiMapKey<String, Boolean>> keys = userService.stream()
                .filter(where -> where.isNotNull(MysqlUserDo::getRoleCode))
                .sorted(order -> order.orderAsc(MysqlUserDo::getRoleCode).orderAsc(MysqlUserDo::getActive))
                .mapToColumn(MysqlUserDo::getRoleCode)
                .appendMap()
                .mapToColumn(MysqlUserDo::getActive)
                .collect(Collectors.toList());

        assertEquals(4, keys.size());
        // admin + active=true 组合存在（key2 已正确映射为 Boolean）
        assertTrue(keys.stream().anyMatch(k -> "admin".equals(k.getKey1()) && Boolean.TRUE.equals(k.getKey2())));
        // user 出现两次（active=true 和 false），且两种布尔值都在
        long userCount = keys.stream().filter(k -> "user".equals(k.getKey1())).count();
        assertEquals(2L, userCount);
        assertTrue(keys.stream().anyMatch(k -> "user".equals(k.getKey1()) && Boolean.TRUE.equals(k.getKey2())));
        assertTrue(keys.stream().anyMatch(k -> "user".equals(k.getKey1()) && Boolean.FALSE.equals(k.getKey2())));
        // auditor 分组存在
        assertTrue(keys.stream().anyMatch(k -> "auditor".equals(k.getKey1())));
    }

    /**
     * 三列分组 MapKey3 覆盖测试（达梦版）。
     * <p>
     * 达梦方言局限说明：按 TINYINT 列（active）分组时，达梦返回 Byte，框架尝试 cast 到 Boolean 抛
     * ClassCastException。改用纯非 boolean 列（role_code, age, manager_id）覆盖 MapKey3 功能语义，
     * boolean 列分组在 DM 的局限单独上报。
     */
    @Test
    void mapKey3ThreeColumnGroupingDistinguishesAllCombinationsOnDameng() {
        // 达梦方言局限：TINYINT(active) → Byte，cast 到 Boolean 失败，改用非 boolean 列
        // 按 (role_code, age, manager_id) 三列分组
        // Alice: admin, 30, null; Bob: user, 25, 1; Carol: user, 28, 1; Dave: auditor, 35, 1
        List<MapKey3<String, Integer, Long>> keys = userService.stream()
                .sorted(order -> order.orderAsc(MysqlUserDo::getId))
                .mapToColumn(MysqlUserDo::getRoleCode)
                .appendMap()
                .mapToColumn(MysqlUserDo::getAge)
                .appendMap()
                .mapToColumn(MysqlUserDo::getManagerId)
                .collect(Collectors.toList());

        assertEquals(4, keys.size());
        // Alice 行
        MapKey3<String, Integer, Long> alice = keys.get(0);
        assertEquals("admin", alice.getKey1());
        assertEquals(30, alice.getKey2());
        assertNull(alice.getKey3());   // managerId 为 null
        // Bob 行
        MapKey3<String, Integer, Long> bob = keys.get(1);
        assertEquals("user", bob.getKey1());
        assertEquals(25, bob.getKey2());
        assertEquals(1L, bob.getKey3());
    }

    /**
     * Gap2 回归：MapKey3 含 boolean 列（active）分组——达梦 TINYINT→Byte 经结果映射层转回 Boolean。
     * 修复前 key2 为 Byte，{@code assertTrue(getKey2())} 会触发 Byte→Boolean 的 ClassCastException。
     */
    @Test
    void mapKey3WithBooleanActiveColumnGroupsCorrectlyOnDameng() {
        // 按 (role_code, active, manager_id) 三元组分组
        // Alice: admin, true, null; Bob: user, true, 1; Carol: user, false, 1; Dave: auditor, true, 1
        List<MapKey3<String, Boolean, Long>> keys = userService.stream()
                .sorted(order -> order.orderAsc(MysqlUserDo::getId))
                .mapToColumn(MysqlUserDo::getRoleCode)
                .appendMap()
                .mapToColumn(MysqlUserDo::getActive)
                .appendMap()
                .mapToColumn(MysqlUserDo::getManagerId)
                .collect(Collectors.toList());

        assertEquals(4, keys.size());
        // Alice: admin, true, null
        MapKey3<String, Boolean, Long> alice = keys.get(0);
        assertEquals("admin", alice.getKey1());
        assertTrue(alice.getKey2());
        assertNull(alice.getKey3());
        // Bob: user, true, 1
        MapKey3<String, Boolean, Long> bob2 = keys.get(1);
        assertEquals("user", bob2.getKey1());
        assertTrue(bob2.getKey2());
        assertEquals(1L, bob2.getKey3());
        // Carol: user, false, 1（修复前此处 Byte→Boolean cast 崩溃）
        MapKey3<String, Boolean, Long> carol = keys.get(2);
        assertEquals("user", carol.getKey1());
        assertFalse(carol.getKey2());
        assertEquals(1L, carol.getKey3());
    }

    @Test
    void mapKey4FourColumnGroupingDistinguishesAllRowsOnDameng() {
        // 按 (id, role_code, active, age) 四列，每行应落入不同桶
        List<MapKey4<Long, String, Boolean, Integer>> keys = userService.stream()
                .sorted(order -> order.orderAsc(MysqlUserDo::getId))
                .mapToColumn(MysqlUserDo::getId)
                .appendMap()
                .mapToColumn(MysqlUserDo::getRoleCode)
                .appendMap()
                .mapToColumn(MysqlUserDo::getActive)
                .appendMap()
                .mapToColumn(MysqlUserDo::getAge)
                .collect(Collectors.toList());

        assertEquals(4, keys.size());
        // 4 行互不相等
        long distinctCount = keys.stream().distinct().count();
        assertEquals(4L, distinctCount);
    }

    /**
     * M15 回归（达梦版）：MapKey5 五键分组时，仅第 5 个 key 不同的两行必须分到不同桶。
     * 验证 MapKey5.hashCode() 和 equals() 正确包含了 key5。
     */
    @Test
    void mapKey5FiveColumnHashCodeIncludesKey5AndSeparatesDistinctRowsOnDameng() {
        // 插入两行：key1..4（role_code, age, creditScore, active）完全相同，balance(key5) 不同
        userService.saveBatchWithoutId(List.of(
                MysqlUserDo.of(10L, "X1", "vip", 20, 50, "111.00", true, "t", 1L, T0, 0),
                MysqlUserDo.of(11L, "X2", "vip", 20, 50, "222.00", true, "t", 1L, T0, 0)
        ));

        List<MapKey5<String, Integer, Integer, Boolean, BigDecimal>> keys = userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getRoleCode, "vip"))
                .sorted(order -> order.orderAsc(MysqlUserDo::getId))
                .mapToColumn(MysqlUserDo::getRoleCode)
                .appendMap()
                .mapToColumn(MysqlUserDo::getAge)
                .appendMap()
                .mapToColumn(MysqlUserDo::getCreditScore)
                .appendMap()
                .mapToColumn(MysqlUserDo::getActive)
                .appendMap()
                .mapToColumn(MysqlUserDo::getBalance)
                .collect(Collectors.toList());

        assertEquals(2, keys.size());
        MapKey5<String, Integer, Integer, Boolean, BigDecimal> k1 = keys.get(0);
        MapKey5<String, Integer, Integer, Boolean, BigDecimal> k2 = keys.get(1);

        // key1-4 相同
        assertEquals(k1.getKey1(), k2.getKey1());
        assertEquals(k1.getKey2(), k2.getKey2());
        assertEquals(k1.getKey3(), k2.getKey3());
        assertEquals(k1.getKey4(), k2.getKey4());

        // key5 不同（111.00 vs 222.00）
        assertNotEquals(0, k1.getKey5().compareTo(k2.getKey5()));

        // M15 核心验证：两个 MapKey5 的 equals/hashCode 必须正确包含 key5
        assertNotEquals(k1, k2);
        assertNotEquals(k1.hashCode(), k2.hashCode(),
                "M15 回归：MapKey5.hashCode() 必须包含 key5，否则两个不同 key 哈希碰撞");

        // 放入 Map 后必须是两个独立桶
        Map<MapKey5<String, Integer, Integer, Boolean, BigDecimal>, Integer> map = new java.util.HashMap<>();
        map.put(k1, 1);
        map.put(k2, 2);
        assertEquals(2, map.size(), "M15 回归：仅 key5 不同的两行在 Map 里必须是两个独立 Entry");
    }

    // =====================================================================
    // 六、M7 回归：pageGroup 的 total 在含聚合函数的 SELECT 时应正确（达梦版）
    // =====================================================================

    @Test
    void m7PageGroupTotalIsCorrectWhenSelectContainsAggregateFunctionOnDameng() {
        // 按 role_code 分组，共 3 个分组（admin, user, auditor）
        // 即便 SELECT 含聚合函数，total 仍应正确返回 3
        IPage<UserStatsDto> page = userService.pageGroup(
                new Page<>(1, 2),
                group -> group.groupBy(MysqlUserDo::getRoleCode),
                where -> where.isNotNull(MysqlUserDo::getId),
                select -> select
                        .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                        .selectFunc(func -> func.count(MysqlUserDo::getId), UserStatsDto::getOrderCount)
                        .selectFunc(func -> func.sum(MysqlUserDo::getBalance), UserStatsDto::getTotalAmount),
                UserStatsDto.class
        );

        // M7：total 必须是分组数（3），而非所有行数（4）
        assertEquals(3L, page.getTotal(),
                "M7 回归：pageGroup 的 total 应等于分组数（3），含聚合函数时不应返回错误值");
        assertEquals(2, page.getRecords().size());   // 第1页，每页2条
        assertSqlContains("group by mps_user.role_code");
        assertSqlContains("select count(*) as c from (select");
    }

    // =====================================================================
    // 七、M10 回归：同一 stream 实例先后两次 toMapXxx 不应 GROUP BY 叠加（达梦版）
    // =====================================================================

    @Test
    void m10SameStreamInstanceReusedForTwoToMapCallsDoesNotAccumulateGroupByOnDameng() {
        // 使用同一个 stream() 实例先做 toMapCount，再做 toMapSum
        // 如果 GROUP BY 未被清除，第二次查询会携带两个 GROUP BY 子句，结果错乱
        var stream = userService.stream();

        Map<String, Long> countByRole = stream.toMapCount(MysqlUserDo::getRoleCode);
        Map<String, BigDecimal> sumByRole = stream.toMapSum(MysqlUserDo::getRoleCode, MysqlUserDo::getBalance);

        // 第一次结果验证
        assertEquals(1L, countByRole.get("admin"));
        assertEquals(2L, countByRole.get("user"));

        // 第二次结果验证：如果 GROUP BY 叠加，admin 的 sum 会错误
        assertEquals(0, new BigDecimal("1200.50").compareTo(sumByRole.get("admin")),
                "M10 回归：第二次 toMapSum 若 GROUP BY 叠加，admin 的 sum 会错误");
        assertEquals(0, new BigDecimal("1080.25").compareTo(sumByRole.get("user")));

        assertSqlContains("sum(balance)");
        assertSqlContains("group by role_code");
    }

    // =====================================================================
    // 八、listGroupJoin + having（连表分组过滤，达梦方言）
    // =====================================================================

    @Test
    void listGroupJoinWithHavingFiltersUsersByOrderCountOnDameng() {
        // 连表 mps_order，按用户分组，HAVING count(order.id) >= 2
        // 只有 Alice（2单）满足
        List<UserStatsDto> stats = userService.listGroupJoin(
                join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
                group -> group.groupBy(MysqlUserDo::getId)
                        .groupBy(MysqlUserDo::getUsername)
                        .having(where -> where.geFunc(
                                func -> func.count(MysqlOrderDo::getId),
                                func -> func.value(2L))),
                where -> where.isNotNull(MysqlUserDo::getId),
                order -> order.orderAsc(MysqlUserDo::getId),
                10,
                select -> select
                        .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                        .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                        .selectFunc(func -> func.count(MysqlOrderDo::getId), UserStatsDto::getOrderCount)
                        .selectFunc(func -> func.sum(MysqlOrderDo::getAmount), UserStatsDto::getTotalAmount),
                UserStatsDto.class
        );

        assertEquals(1, stats.size());
        assertEquals("Alice", stats.get(0).getUsername());
        assertEquals(2L, stats.get(0).getOrderCount());
        assertEquals(0, new BigDecimal("160.00").compareTo(stats.get(0).getTotalAmount()));
        assertSqlContains("having");
        assertSqlContains("left join mps_order");
        assertSqlContains("group by mps_user.id");
    }

    // =====================================================================
    // 九、边界场景：分组结果为空时 listGroupValues 返回空列表（达梦版）
    // =====================================================================

    @Test
    void listGroupValuesReturnsEmptyListWhenNoRowsMatchPredicateOnDameng() {
        // 过滤条件命中零行，分组聚合应返回空列表
        List<BigDecimal> result = userService.listGroupValues(
                group -> group.groupBy(MysqlUserDo::getRoleCode),
                where -> where.eq(MysqlUserDo::getRoleCode, "nonexistent"),
                func -> func.sum(MysqlUserDo::getBalance)
        );

        assertTrue(result.isEmpty(), "predicate 无匹配行时 listGroupValues 应返回空列表");
    }

    // =====================================================================
    // 十、多维聚合：按用户+订单状态二维分组（达梦版）
    // =====================================================================

    @Test
    void listGroupJoinByUserIdAndOrderStatusReturnsTwoDimensionalStatsOnDameng() {
        // 连表后按 user_id + order.status 二维分组，统计各组金额总和
        // 结果应包含：(Alice,paid)=160, (Bob,new)=35.5, (Carol,paid)=220, (Dave,cancelled)=15
        List<UserStatsDto> stats = userService.listGroupJoin(
                join -> join.innerJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
                group -> group.groupBy(MysqlUserDo::getId).groupBy(MysqlOrderDo::getStatus),
                where -> where.isNotNull(MysqlUserDo::getId),
                order -> order.orderAsc(MysqlUserDo::getId),
                10,
                select -> select
                        .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                        .select(MysqlOrderDo::getStatus, UserStatsDto::getRoleCode)
                        .selectFunc(func -> func.sum(MysqlOrderDo::getAmount), UserStatsDto::getTotalAmount),
                UserStatsDto.class
        );

        // 4 个用户各有订单，共 4 个二维分组（Alice 有 2 单但都是 paid 状态，合并为 1 桶）
        assertEquals(4, stats.size());
        UserStatsDto aliceRow = stats.stream().filter(r -> r.getUserId() == 1L).findFirst().orElseThrow();
        assertEquals("paid", aliceRow.getRoleCode());
        assertEquals(0, new BigDecimal("160.00").compareTo(aliceRow.getTotalAmount()));
        // group by 子句应包含 mps_user.id 和 mps_order.status（以逗号分隔在同一 GROUP BY 里）
        assertSqlContains("group by mps_user.id");
        assertSqlContains("mps_order.status");
    }
}
