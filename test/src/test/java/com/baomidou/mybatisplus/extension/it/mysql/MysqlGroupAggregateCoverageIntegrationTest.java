package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.bo.key.BiMapKey;
import com.baomidou.mybatisplus.extension.bo.key.MapKey3;
import com.baomidou.mybatisplus.extension.bo.key.MapKey4;
import com.baomidou.mybatisplus.extension.bo.key.MapKey5;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GroupAggregate 场景桶：分组/聚合/Map 全覆盖
 *
 * 覆盖场景：
 *   - group/having
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
class MysqlGroupAggregateCoverageIntegrationTest extends MysqlIntegrationTestBase {

    // =====================================================================
    // 一、toMapCount / toMapSum / toMapAvg / toMapMax / toMapMin
    // =====================================================================

    @Test
    void toMapCountGroupsByRoleCodeAndReturnsCorrectCounts() {
        // 种子数据：admin=Alice(1), user=Bob+Carol(2), auditor=Dave(1); Eve 逻辑删除不算
        Map<String, Long> countByRole = userService.stream().toMapCount(MysqlUserDo::getRoleCode);

        assertEquals(1L, countByRole.get("admin"));
        assertEquals(2L, countByRole.get("user"));
        assertEquals(1L, countByRole.get("auditor"));
        // 逻辑删除行不应出现
        assertNull(countByRole.get(null));
        assertSqlContains("count(*)");
        assertSqlContains("group by role_code");
    }

    @Test
    void toMapSumGroupsBalanceByRoleCodeCorrectly() {
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
    void toMapAvgGroupsCreditScoreByRoleCodeCorrectly() {
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
    void toMapMaxAndMinGroupCreditScoreByRoleCodeCorrectly() {
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
    void toMapReturnsIdToUsernameMapping() {
        // 使用 filter 只取 active 用户，验证 toMap 投影两列
        Map<Long, String> idToName = userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getActive, true))
                .toMap(MysqlUserDo::getId, MysqlUserDo::getUsername);

        assertEquals("Alice", idToName.get(1L));
        assertEquals("Bob", idToName.get(2L));
        assertEquals("Dave", idToName.get(4L));
        assertFalse(idToName.containsKey(3L));   // Carol active=false
        assertFalse(idToName.containsKey(5L));   // Eve 逻辑删除
        assertSqlContains("where");
        assertSqlContains("active");
    }

    @Test
    void toSetReturnsDistinctRoleCodes() {
        // 种子数据共 3 种 role：admin, user, auditor（Eve 逻辑删除不算）
        java.util.Set<String> roles = userService.stream().toSet(MysqlUserDo::getRoleCode);

        assertEquals(3, roles.size());
        assertTrue(roles.contains("admin"));
        assertTrue(roles.contains("user"));
        assertTrue(roles.contains("auditor"));
    }

    // =====================================================================
    // 三、groupingBy（应用层，全量加载后分组）
    // =====================================================================

    @Test
    void groupingByRoleCodeReturnsCorrectBuckets() {
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
    void listGroupByRoleCodeReturnsAggregatedStatsPerRole() {
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

        // 按 role_code 升序：admin < auditor < user
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
    void listGroupWithHavingFiltersGroupsCorrectly() {
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
    void listGroupValuesReturnsListOfSumBalancePerRoleOrdered() {
        // 按 role_code 分组 sum(balance)，升序排列，验证 BigDecimal 列表
        List<BigDecimal> totals = userService.listGroupValues(
                group -> group.groupBy(MysqlUserDo::getRoleCode),
                where -> where.isNotNull(MysqlUserDo::getId),
                order -> order.orderAsc(MysqlUserDo::getRoleCode),
                10,
                func -> func.sum(MysqlUserDo::getBalance)
        );

        // 升序 role_code: admin(1200.50), auditor(90.00), user(1080.25)
        assertEquals(3, totals.size());
        assertEquals(0, new BigDecimal("1200.50").compareTo(totals.get(0)));
        assertEquals(0, new BigDecimal("90.00").compareTo(totals.get(1)));
        assertEquals(0, new BigDecimal("1080.25").compareTo(totals.get(2)));
        assertSqlContains("sum(mps_user.balance)");
        assertSqlContains("group by mps_user.role_code");
        assertSqlContains("order by mps_user.role_code asc");
        // M-02(part2) Gap3 回归：分组场景不应叠加多余 DISTINCT（GROUP BY 已保证每组一行）
        assertSqlNotContains("distinct");
    }

    @Test
    void listGroupValuesWithoutOrderReturnsAllGroupSums() {
        // 无 order + limit 版本，仅验证 size 和存在特定值
        List<BigDecimal> totals = userService.listGroupValues(
                group -> group.groupBy(MysqlUserDo::getRoleCode),
                where -> where.isNotNull(MysqlUserDo::getId),
                func -> func.sum(MysqlUserDo::getBalance)
        );

        assertEquals(3, totals.size());
        assertTrue(totals.stream().anyMatch(v -> v.compareTo(new BigDecimal("1200.50")) == 0));
        assertTrue(totals.stream().anyMatch(v -> v.compareTo(new BigDecimal("1080.25")) == 0));
        // M-02(part2) Gap3 回归：无 order+limit 版本的分组场景也不应叠加多余 DISTINCT
        assertSqlNotContains("distinct");
    }

    // =====================================================================
    // 五、多列分组 MapKey2..5（BiMapKey / MapKey3 / MapKey4 / MapKey5）
    // =====================================================================

    @Test
    void biMapKeyTwoColumnGroupingReturnsCorrectBuckets() {
        // 按 (role_code, active) 二元组分组计数
        // 种子数据可见：admin+true=1, user+true=1(Bob), user+false=1(Carol), auditor+true=1(Dave)
        List<BiMapKey<String, Boolean>> keys = userService.stream()
                .filter(where -> where.isNotNull(MysqlUserDo::getRoleCode))
                .sorted(order -> order.orderAsc(MysqlUserDo::getRoleCode).orderAsc(MysqlUserDo::getActive))
                .mapToColumn(MysqlUserDo::getRoleCode)
                .appendMap()
                .mapToColumn(MysqlUserDo::getActive)
                .collect(Collectors.toList());

        // 应有 4 行（admin-true, auditor-true, user-false, user-true）排序后
        assertEquals(4, keys.size());
        // 验证存在 admin+active 组合
        assertTrue(keys.stream().anyMatch(k -> "admin".equals(k.getKey1()) && Boolean.TRUE.equals(k.getKey2())));
        // 验证 user 出现两次（active=true 和 false）
        long userCount = keys.stream().filter(k -> "user".equals(k.getKey1())).count();
        assertEquals(2L, userCount);
    }

    @Test
    void mapKey3ThreeColumnGroupingDistinguishesAllCombinations() {
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
        MapKey3<String, Boolean, Long> bob = keys.get(1);
        assertEquals("user", bob.getKey1());
        assertTrue(bob.getKey2());
        assertEquals(1L, bob.getKey3());
    }

    @Test
    void mapKey4FourColumnGroupingDistinguishesAllRows() {
        // 按 (id, role_code, active, age) 四列分组，每行应该落入不同桶
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

        // 4 个可见用户，每条记录组合唯一
        assertEquals(4, keys.size());
        // 验证所有 key 都不同（4 行应该全部互不相等）
        long distinctCount = keys.stream().distinct().count();
        assertEquals(4L, distinctCount);
    }

    /**
     * M15 回归：MapKey5 五键分组时，仅第 5 个 key 不同的两行必须分到不同桶。
     * 验证 MapKey5.hashCode() 和 equals() 正确包含了 key5。
     *
     * 策略：插入两行额外数据，前 4 个维度（id 等）完全不同，但用 role_code/active/age/creditScore 可构造
     * 使得 key1..4 相同、key5(balance) 不同的情况；最终验证 HashMap 中桶数正确且 key5 能区分。
     */
    @Test
    void mapKey5FiveColumnHashCodeIncludesKey5AndSeparatesDistinctRows() {
        // 插入 id=10,11 两行：使 (role_code, age, creditScore, active) 四维均相同，balance 不同
        // 以此构造"仅 key5 不同"的场景
        userService.saveBatchWithoutId(List.of(
                MysqlUserDo.of(10L, "X1", "vip", 20, 50, "111.00", true, "t", 1L, T0, 0),
                MysqlUserDo.of(11L, "X2", "vip", 20, 50, "222.00", true, "t", 1L, T0, 0)
        ));

        // 查询这两行：key1=role_code, key2=age, key3=creditScore, key4=active, key5=balance
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

        // 两行 key1..4 完全相同，key5 不同
        assertEquals(2, keys.size());
        MapKey5<String, Integer, Integer, Boolean, BigDecimal> k1 = keys.get(0);
        MapKey5<String, Integer, Integer, Boolean, BigDecimal> k2 = keys.get(1);

        // key1-4 相同
        assertEquals(k1.getKey1(), k2.getKey1());
        assertEquals(k1.getKey2(), k2.getKey2());
        assertEquals(k1.getKey3(), k2.getKey3());
        assertEquals(k1.getKey4(), k2.getKey4());

        // key5 不同
        assertNotEquals(0, k1.getKey5().compareTo(k2.getKey5()));

        // M15 核心验证：两个 key 的 hashCode 应不同，equals 应返回 false
        assertNotEquals(k1, k2);
        assertNotEquals(k1.hashCode(), k2.hashCode(),
                "M15 回归：MapKey5.hashCode() 必须包含 key5，否则两个不同 key 哈希碰撞");

        // 放入 Map 后也应该是两个独立桶
        Map<MapKey5<String, Integer, Integer, Boolean, BigDecimal>, Integer> map = new java.util.HashMap<>();
        map.put(k1, 1);
        map.put(k2, 2);
        assertEquals(2, map.size(), "M15 回归：仅 key5 不同的两行在 Map 里必须是两个独立 Entry");
    }

    // =====================================================================
    // 六、M7 回归：pageGroup 的 total 在含聚合函数的 SELECT 时应正确
    // =====================================================================

    @Test
    void m7PageGroupTotalIsCorrectWhenSelectContainsAggregateFunction() {
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
    // 七、M10 回归：同一 stream 实例先后两次 toMapXxx 不应 GROUP BY 叠加
    // =====================================================================

    @Test
    void m10SameStreamInstanceReusedForTwoToMapCallsDoesNotAccumulateGroupBy() {
        // 使用同一个 stream() 实例先做 toMapCount，再做 toMapSum
        // 如果 GROUP BY 未被清除，第二次查询会携带两个 GROUP BY 子句，结果错乱
        var stream = userService.stream();

        Map<String, Long> countByRole = stream.toMapCount(MysqlUserDo::getRoleCode);
        // 注意：toMapCount 内部会 reset() + 清 groupBy；stream 实例可以继续使用
        Map<String, BigDecimal> sumByRole = stream.toMapSum(MysqlUserDo::getRoleCode, MysqlUserDo::getBalance);

        // 第一次结果验证
        assertEquals(1L, countByRole.get("admin"));
        assertEquals(2L, countByRole.get("user"));

        // 第二次结果验证：如果 GROUP BY 叠加，admin 的 sum 会错误
        assertEquals(0, new BigDecimal("1200.50").compareTo(sumByRole.get("admin")),
                "M10 回归：第二次 toMapSum 若 GROUP BY 叠加，admin 的 sum 会错误");
        assertEquals(0, new BigDecimal("1080.25").compareTo(sumByRole.get("user")));

        // SQL 断言：第二次 toMapSum 的 SQL 里不应出现重复的 group by
        // （由于 CapturedSql 会包含两次查询，我们只断言聚合列存在即可）
        assertSqlContains("sum(balance)");
        assertSqlContains("group by role_code");
    }

    // =====================================================================
    // 八、listGroupJoin + having（连表分组过滤）
    // =====================================================================

    @Test
    void listGroupJoinWithHavingFiltersUsersByOrderCount() {
        // 连表 mps_order，按用户分组，HAVING count(order.id) >= 2
        // 只有 Alice（2单）和 Carol（但 Carol active=false）、Bob（1单）、Dave（1单）
        // 没有 HAVING active 过滤，Carol 也会进分组：Alice 有 2 单满足
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

        // 只有 Alice（2单）满足 HAVING >= 2
        assertEquals(1, stats.size());
        assertEquals("Alice", stats.get(0).getUsername());
        assertEquals(2L, stats.get(0).getOrderCount());
        assertEquals(0, new BigDecimal("160.00").compareTo(stats.get(0).getTotalAmount()));
        assertSqlContains("having");
        assertSqlContains("left join mps_order");
        assertSqlContains("group by mps_user.id");
    }

    // =====================================================================
    // 九、多维聚合：按用户+订单状态二维分组
    // =====================================================================

    @Test
    void listGroupJoinByUserIdAndOrderStatusReturnsTwoDimensionalStats() {
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

    // =====================================================================
    // 十、边界场景：分组结果为空时 listGroupValues 返回空列表
    // =====================================================================

    @Test
    void listGroupValuesReturnsEmptyListWhenNoRowsMatchPredicate() {
        // 过滤条件命中零行，分组聚合应返回空列表
        List<BigDecimal> result = userService.listGroupValues(
                group -> group.groupBy(MysqlUserDo::getRoleCode),
                where -> where.eq(MysqlUserDo::getRoleCode, "nonexistent"),
                func -> func.sum(MysqlUserDo::getBalance)
        );

        assertTrue(result.isEmpty(), "predicate 无匹配行时 listGroupValues 应返回空列表");
    }
}
