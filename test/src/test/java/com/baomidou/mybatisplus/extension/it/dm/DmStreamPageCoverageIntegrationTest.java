package com.baomidou.mybatisplus.extension.it.dm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.it.mysql.CapturedSql;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlOrderDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserDo;
import com.baomidou.mybatisplus.extension.it.mysql.UserStatsDto;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stream / 分页 全覆盖集成测试（达梦 DAMENG 方言）
 * 场景与 MysqlStreamPageCoverageIntegrationTest 对称，主要差异：
 *   - 达梦 skip+limit 语法为 LIMIT n OFFSET m（而 MySQL 为 LIMIT m, n）
 *   - 达梦 FOR UPDATE 语法一致
 *   - 达梦方言由基类 @BeforeAll 已通过 DialectRegistry.use(DbType.DAMENG) 设置
 *   - 种子数据 tags 字段：Alice="java,dm,admin"，Carol="dm,report"（DM 版差异）
 */
class DmStreamPageCoverageIntegrationTest extends DmIntegrationTestBase {

    // -------------------------------------------------------------------------
    // stream() filter + findFirst
    // -------------------------------------------------------------------------

    @Test
    void streamFilterFindFirstReturnsSingleMatchedRowOnDameng() {
        // filter + sorted + findFirst，验证达梦方言下 WHERE/ORDER BY/LIMIT 均正确生成
        Optional<MysqlUserDo> first = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .sorted(order -> order.orderDesc(MysqlUserDo::getCreditScore))
            .findFirst();

        assertTrue(first.isPresent(), "应能找到 active=true 的用户");
        assertEquals("Alice", first.get().getUsername(), "credit_score 最高的活跃用户是 Alice");

        assertSqlContains("active");
        assertSqlContains("order by mps_user.credit_score desc");
        assertSqlContains("limit 1");
    }

    @Test
    void streamFilterFindFirstOnEmptyResultReturnsEmptyOnDameng() {
        // 不存在的用户，findFirst 应返回 Optional.empty()
        Optional<MysqlUserDo> result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "NoSuchUser"))
            .findFirst();

        assertFalse(result.isPresent(), "不存在的用户应返回 Optional.empty()");
    }

    // -------------------------------------------------------------------------
    // stream() sorted（ORDER BY 推到 SQL）
    // -------------------------------------------------------------------------

    @Test
    void streamSortedPushesOrderByToDamengSql() {
        // 按 age ASC 排序，active 用户：Bob(25) < Alice(30) < Dave(35)
        List<MysqlUserDo> sorted = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .sorted(order -> order.orderAsc(MysqlUserDo::getAge))
            .collect(Collectors.toList());

        assertEquals(3, sorted.size());
        assertEquals("Bob", sorted.get(0).getUsername(), "年龄最小应是 Bob(25)");
        assertEquals("Dave", sorted.get(2).getUsername(), "年龄最大应是 Dave(35)");

        assertSqlContains("order by mps_user.age asc");
    }

    @Test
    void streamSortedDescPushesCorrectOrderByClauseOnDameng() {
        // id DESC 排序，取前 2 条
        List<MysqlUserDo> users = userService.stream()
            .sorted(order -> order.orderDesc(MysqlUserDo::getId))
            .limit(2)
            .collect(Collectors.toList());

        assertEquals(2, users.size());
        assertEquals(4L, users.get(0).getId(), "id 最大的可见用户是 Dave(4)");

        assertSqlContains("order by mps_user.id desc");
        assertSqlContains("limit 2");
    }

    // -------------------------------------------------------------------------
    // stream() limit / skip（达梦：LIMIT n OFFSET m）
    // -------------------------------------------------------------------------

    @Test
    void streamLimitPushedToSqlOnDameng() {
        // limit(2) 应生成 LIMIT 2
        List<MysqlUserDo> users = userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .limit(2)
            .collect(Collectors.toList());

        assertEquals(2, users.size());
        assertSqlContains("limit 2");
    }

    @Test
    void streamSkipAndLimitGeneratesDamengOffsetSyntax() {
        // 达梦方言：skip(1) + limit(2) → LIMIT 2 OFFSET 1（与 MySQL 的 LIMIT 1,2 不同）
        List<MysqlUserDo> users = userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .skip(1)
            .limit(2)
            .collect(Collectors.toList());

        assertEquals(2, users.size());
        // 跳过第 1 行(Alice)，应取 Bob(2) 和 Carol(3)
        assertEquals("Bob", users.get(0).getUsername());
        assertEquals("Carol", users.get(1).getUsername());

        // 达梦方言使用 LIMIT n OFFSET m 形式
        assertSqlContains("limit 2 offset 1");
    }

    // -------------------------------------------------------------------------
    // stream() distinct
    // -------------------------------------------------------------------------

    @Test
    void streamDistinctDeduplicatesOnDameng() {
        // mapToColumn + distinct + sorted 取不重复角色
        List<String> roles = userService.stream()
            .mapToColumn(MysqlUserDo::getRoleCode)
            .distinct()
            .sorted(order -> order.orderAsc(MysqlUserDo::getRoleCode))
            .collect(Collectors.toList());

        assertEquals(3, roles.size(), "角色去重后应有 3 种");
        assertTrue(roles.contains("admin"));
        assertTrue(roles.contains("user"));
        assertTrue(roles.contains("auditor"));

        assertSqlContains("select distinct");
    }

    // -------------------------------------------------------------------------
    // stream() forEach / collect / toList
    // -------------------------------------------------------------------------

    @Test
    void streamForEachIteratesAllVisibleRowsOnDameng() {
        // forEach 遍历所有可见用户，Eve(deleted=1) 不可见
        List<String> names = new ArrayList<>();
        userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .forEach(user -> names.add(user.getUsername()));

        assertEquals(4, names.size(), "逻辑删除的 Eve 不应出现");
        assertFalse(names.contains("Eve"), "Eve 已逻辑删除");
        assertEquals("Alice", names.get(0));
    }

    @Test
    void streamCollectToListReturnsAllVisibleRowsOnDameng() {
        // 全量 collect，验证行数
        List<MysqlUserDo> all = userService.stream()
            .collect(Collectors.toList());

        assertEquals(4, all.size(), "可见用户应为 4 条");
    }

    // -------------------------------------------------------------------------
    // stream() toSet / toMap
    // -------------------------------------------------------------------------

    @Test
    void streamToSetExtractsColumnAsSetOnDameng() {
        // toSet 提取角色集合
        Set<String> roleSet = userService.stream()
            .toSet(MysqlUserDo::getRoleCode);

        assertEquals(Set.of("admin", "user", "auditor"), roleSet);
    }

    @Test
    void streamToMapBuildKeyValueMapOnDameng() {
        // toMap(id -> username) 映射
        Map<Long, String> idToName = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .toMap(MysqlUserDo::getId, MysqlUserDo::getUsername);

        assertEquals(3, idToName.size(), "active 用户 3 个");
        assertEquals("Alice", idToName.get(1L));
        assertEquals("Bob", idToName.get(2L));
        assertEquals("Dave", idToName.get(4L));
    }

    // -------------------------------------------------------------------------
    // stream() forUpdate（达梦也支持 FOR UPDATE）
    // -------------------------------------------------------------------------

    @Test
    void streamForUpdateAppendsForUpdateToDamengSql() {
        // 达梦支持 FOR UPDATE 语法
        MysqlUserDo locked = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Bob"))
            .forUpdate()
            .findFirst()
            .orElseThrow();

        assertEquals("Bob", locked.getUsername());
        assertSqlContains("for update");
    }

    // -------------------------------------------------------------------------
    // stream().map（投影到 DTO）
    // -------------------------------------------------------------------------

    @Test
    void streamMapProjectsToDtoWithSelectedColumnsOnDameng() {
        // stream().map(select -> ...) 投影到 UserStatsDto，验证列选择下推
        List<UserStatsDto> rows = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .map(select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode),
                UserStatsDto.class)
            .collect(Collectors.toList());

        assertEquals(3, rows.size());
        assertEquals("Alice", rows.get(0).getUsername());
        assertEquals("admin", rows.get(0).getRoleCode());
        assertEquals(1L, rows.get(0).getUserId());

        assertSqlContains("mps_user.id");
        assertSqlContains("mps_user.username");
    }

    // -------------------------------------------------------------------------
    // 回归 M8：同一 stream 工厂调用两次 map 到不同类型，互不污染（达梦方言）
    // -------------------------------------------------------------------------

    @Test
    void twoIndependentStreamMapsDoNotPollutateEachOtherOnDameng() {
        // 第一次 stream().map → 仅选 userId + username
        List<UserStatsDto> dtoList = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername),
                UserStatsDto.class)
            .collect(Collectors.toList());

        // 第二次独立 stream().map → 仅选 roleCode
        List<UserStatsDto> dtoList2 = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode),
                UserStatsDto.class)
            .collect(Collectors.toList());

        // 两次结果互不干扰
        assertEquals(1, dtoList.size());
        assertEquals("Alice", dtoList.get(0).getUsername(), "第一次 map 应有 username");
        assertNull(dtoList.get(0).getRoleCode(), "第一次 map 未选 roleCode，应为 null");

        assertEquals(1, dtoList2.size());
        assertNull(dtoList2.get(0).getUsername(), "第二次 map 未选 username，应为 null");
        assertEquals("admin", dtoList2.get(0).getRoleCode(), "第二次 map 应有 roleCode");
    }

    // -------------------------------------------------------------------------
    // page（基础分页）
    // -------------------------------------------------------------------------

    @Test
    void pageReturnsTotalCountAndCurrentPageRecordsOnDameng() {
        // active 用户 3 条，第 1 页每页 2 条
        IPage<MysqlUserDo> result = userService.page(
            new Page<>(1, 2),
            where -> where.eq(MysqlUserDo::getActive, true)
        );

        assertEquals(3L, result.getTotal(), "active 用户共 3 条");
        assertEquals(2, result.getRecords().size(), "第 1 页应有 2 条");
    }

    @Test
    void pageSecondPageReturnsRemainingRecordOnDameng() {
        // 第 2 页应只有 1 条剩余
        IPage<MysqlUserDo> page2 = userService.page(
            new Page<>(2, 2),
            where -> where.eq(MysqlUserDo::getActive, true)
        );

        assertEquals(3L, page2.getTotal());
        assertEquals(1, page2.getRecords().size(), "第 2 页只剩 1 条");
    }

    @Test
    void pageWithProjectionMapsToDto_Dameng() {
        // page + select 投影到 UserStatsDto
        IPage<UserStatsDto> result = userService.page(
            new Page<>(1, 3),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode),
            UserStatsDto.class
        );

        assertEquals(4L, result.getTotal(), "可见用户共 4 条");
        assertEquals(3, result.getRecords().size());
        assertTrue(result.getRecords().stream().allMatch(r -> r.getUsername() != null));

        assertSqlContains("limit 3");
    }

    // -------------------------------------------------------------------------
    // 分页排序：合法列名正确下推到 ORDER BY（H1 正常路径）
    // -------------------------------------------------------------------------

    @Test
    void pageWithSortedPushesOrderByToDamengSql() {
        // 合法列名 credit_score 通过 list + order 推到 ORDER BY
        List<MysqlUserDo> paged = userService.list(
            where -> where.isNotNull(MysqlUserDo::getId),
            order -> order.orderDesc(MysqlUserDo::getCreditScore),
            2
        );

        assertEquals(2, paged.size());
        assertEquals("Alice", paged.get(0).getUsername(), "信用分最高应是 Alice(98)");
        assertEquals("Carol", paged.get(1).getUsername(), "信用分第二应是 Carol(88)");

        assertSqlContains("order by mps_user.credit_score desc");
        assertSqlContains("limit 2");
    }

    @Test
    void streamSortedByLegalColumnNamePushesItToOrderBySqlOnDameng() {
        // H1 回归正常路径：合法列名 age 通过 stream().sorted 推到 ORDER BY
        userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getAge))
            .collect(Collectors.toList());

        assertSqlContains("order by mps_user.age asc");
    }

    // -------------------------------------------------------------------------
    // 回归 H1：非法列名注入应抛 IllegalArgumentException（达梦方言）
    // -------------------------------------------------------------------------

    @Test
    void illegalColumnNameWithSemicolonShouldThrowOnDameng() {
        // H1 回归：OrderLambdaQueryWrapper 没有 orderByRaw 方法；
        // 列名注入校验由 MybatisUtil.buildPage(PageVo) 在 SortVo.key 白名单处拦截（^[A-Za-z0-9_]+$ 校验）。
        // 构造含非法 key 的 PageVo 传入 buildPage，验证抛 IllegalArgumentException（达梦与 MySQL 逻辑相同）。
        com.baomidou.mybatisplus.extension.bo.PageVo pageVo = new com.baomidou.mybatisplus.extension.bo.PageVo();
        pageVo.setPageNum(1);
        pageVo.setPageSize(10);
        com.baomidou.mybatisplus.extension.bo.SortVo sortVo = new com.baomidou.mybatisplus.extension.bo.SortVo();
        sortVo.setKey("id;DROP TABLE mps_user");  // 含分号的非法列名
        sortVo.setAsc(true);
        pageVo.setOrder(java.util.Collections.singletonList(sortVo));

        assertThrows(IllegalArgumentException.class,
            () -> com.baomidou.mybatisplus.toolkit.MybatisUtil.buildPage(pageVo),
            "H1：含分号的非法列名应在 buildPage 白名单校验处抛 IllegalArgumentException"
        );
    }

    @Test
    void sqlDoesNotContainInjectionCharsWhenSortedByLegalColumnOnDameng() {
        // 通过 lambda 合法字段引用排序，验证 SQL 无注入特征字符
        userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getCreditScore))
            .limit(1)
            .collect(Collectors.toList());

        String sql = CapturedSql.joined();
        assertFalse(sql.contains(";"), "SQL 中不应含分号注入字符");
        assertFalse(sql.toLowerCase().contains("drop"), "SQL 中不应含 DROP 关键字");
        assertSqlContains("order by mps_user.credit_score asc");
    }

    // -------------------------------------------------------------------------
    // pageGroup（分组分页，达梦方言）
    // -------------------------------------------------------------------------

    @Test
    void pageGroupReturnsTotalGroupCountOnDameng() {
        // 按 role_code 分组：3 组，第 1 页取 2 组
        IPage<UserStatsDto> result = userService.pageGroup(
            new Page<>(1, 2),
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.count(), UserStatsDto::getOrderCount),
            UserStatsDto.class
        );

        assertEquals(3L, result.getTotal(), "角色共 3 组");
        assertEquals(2, result.getRecords().size(), "第 1 页返回 2 组");
        assertTrue(result.getRecords().stream().allMatch(r -> r.getRoleCode() != null));

        assertSqlContains("group by mps_user.role_code");
        assertSqlContains("count(*)");
        assertSqlContains("limit 2");
    }

    @Test
    void pageGroupSecondPageReturnsLastGroupOnDameng() {
        // 第 2 页应只有 1 组
        IPage<UserStatsDto> page2 = userService.pageGroup(
            new Page<>(2, 2),
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.count(), UserStatsDto::getOrderCount),
            UserStatsDto.class
        );

        assertEquals(3L, page2.getTotal());
        assertEquals(1, page2.getRecords().size(), "第 2 页应只剩 1 组");
    }

    @Test
    void pageGroupWithActiveFilterReducesGroupCountOnDameng() {
        // active=true 过滤后：3 组各 1 人，user 组 count=1
        IPage<UserStatsDto> result = userService.pageGroup(
            new Page<>(1, 10),
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.count(), UserStatsDto::getOrderCount),
            UserStatsDto.class
        );

        assertEquals(3L, result.getTotal());
        Map<String, Long> byRole = result.getRecords().stream()
            .collect(Collectors.toMap(UserStatsDto::getRoleCode, UserStatsDto::getOrderCount));
        assertEquals(1L, byRole.get("user"), "active user 只有 Bob 1 个");
    }

    // -------------------------------------------------------------------------
    // pageJoin（连表分页，达梦方言）
    // -------------------------------------------------------------------------

    @Test
    void pageJoinReturnsTotalJoinedRowsOnDameng() {
        // LEFT JOIN order，active 用户：Alice 2 orders + Bob 1 + Dave 1 = 4 行
        IPage<UserStatsDto> result = userService.pageJoin(
            new Page<>(1, 2),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getStatus, UserStatsDto::getRoleCode),
            UserStatsDto.class
        );

        assertEquals(4L, result.getTotal(), "active 用户 LEFT JOIN 订单共 4 行");
        assertEquals(2, result.getRecords().size(), "第 1 页 2 条");

        assertSqlContains("left join mps_order");
        assertSqlContains("limit 2");
    }

    @Test
    void pageJoinSecondPageReturnsRemainingRowsOnDameng() {
        // 第 2 页应有 2 条
        IPage<UserStatsDto> page2 = userService.pageJoin(
            new Page<>(2, 2),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getStatus, UserStatsDto::getRoleCode),
            UserStatsDto.class
        );

        assertEquals(4L, page2.getTotal());
        assertEquals(2, page2.getRecords().size(), "第 2 页应还有 2 行");
    }

    @Test
    void pageJoinAllUsersIncludesAllVisibleOrdersOnDameng() {
        // 不过滤 active，所有可见用户 LEFT JOIN orders：Alice 2 + Bob 1 + Carol 1 + Dave 1 = 5 行
        IPage<UserStatsDto> result = userService.pageJoin(
            new Page<>(1, 10),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getStatus, UserStatsDto::getRoleCode),
            UserStatsDto.class
        );

        assertEquals(5L, result.getTotal(), "所有可见用户 LEFT JOIN 订单共 5 行");
    }

    // -------------------------------------------------------------------------
    // stream 分页排序综合场景（达梦）
    // -------------------------------------------------------------------------

    @Test
    void streamSortedThenLimitSimulatesPageOneSortedByBalanceOnDameng() {
        // 按余额 DESC 取前 2 条：Alice(1200.50) > Carol(780.25)
        List<MysqlUserDo> topTwo = userService.stream()
            .sorted(order -> order.orderDesc(MysqlUserDo::getBalance))
            .limit(2)
            .collect(Collectors.toList());

        assertEquals(2, topTwo.size());
        assertEquals("Alice", topTwo.get(0).getUsername(), "余额最高是 Alice");
        assertEquals("Carol", topTwo.get(1).getUsername(), "余额第二是 Carol");

        assertSqlContains("order by mps_user.balance desc");
        assertSqlContains("limit 2");
    }

    @Test
    void pageGroupJoinCombinesJoinGroupAndPaginationOnDameng() {
        // pageGroupJoin：LEFT JOIN orders + GROUP BY role_code + 分页
        IPage<UserStatsDto> result = userService.pageGroupJoin(
            new Page<>(1, 2),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.count(MysqlOrderDo::getId), UserStatsDto::getOrderCount)
                .selectFunc(func -> func.sum(MysqlOrderDo::getAmount), UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );

        // active 用户 3 组
        assertEquals(3L, result.getTotal(), "按角色分组共 3 组");
        assertEquals(2, result.getRecords().size(), "第 1 页 2 条");
        assertTrue(result.getRecords().stream().allMatch(r -> r.getRoleCode() != null));

        assertSqlContains("left join mps_order");
        assertSqlContains("group by mps_user.role_code");
        assertSqlContains("limit 2");
    }

    // -------------------------------------------------------------------------
    // 复审回归：M-01（TypeReference.getRawType）/ L-08（mapToColumn 达梦 TINYINT→Boolean）
    // -------------------------------------------------------------------------

    @Test
    void mapWithIbatisTypeReferenceResolvesRawTypeNotNullOnDameng() {
        // M-01：TypeReference 路径经 getRawType() 解析类型，不再因无参反射调带参方法返回 null 而 NPE。
        org.apache.ibatis.type.TypeReference<MysqlUserDo> ref = new org.apache.ibatis.type.TypeReference<MysqlUserDo>() {};
        List<MysqlUserDo> users = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .map(select -> select.selectAll(MysqlUserDo.class), ref)
            .collect(Collectors.toList());
        assertFalse(users.isEmpty(), "TypeReference 映射应返回行而非 NPE");
        assertEquals(MysqlUserDo.class, users.get(0).getClass(), "应解析为 MysqlUserDo 而非 null");
    }

    @Test
    void mapToColumnBooleanReturnsBooleanNotByteOnDameng() {
        // L-08（关键）：达梦 TINYINT 返回 Byte，mapToColumn 经 valueTypeOf+coerceValue 须转回 Boolean，
        // 否则 Stream1 单值路径会取到 Byte（旧行为），与 MySQL 不一致。
        List<Boolean> actives = userService.stream()
            .mapToColumn(MysqlUserDo::getActive)
            .collect(Collectors.toList());
        assertFalse(actives.isEmpty());
        Object first = actives.stream().filter(java.util.Objects::nonNull).findFirst().orElseThrow();
        assertEquals(Boolean.class, first.getClass(), "达梦 mapToColumn(active) 应返回 Boolean 而非 Byte");
    }

    @Test
    void pageWithIPageOrdersSortsByResolvedColumnOnDameng_L05() {
        // L-05 回归（达梦）：IPage.getOrders() 排序经预处理 + keySet 精确匹配应真正下推 ORDER BY。
        // creditScore 升序 → Dave(65) 首位；若排序丢失则为默认序的 Alice。
        Page<MysqlUserDo> page = new Page<>(1, 10);
        page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.asc("creditScore"));
        IPage<MysqlUserDo> result = userService.page(page, where -> where.isNotNull(MysqlUserDo::getId));
        List<MysqlUserDo> records = result.getRecords();
        assertFalse(records.isEmpty(), "分页应返回记录");
        assertEquals("Dave", records.get(0).getUsername(),
            "按 creditScore 升序 Dave(65) 应居首；若仍是 Alice 说明 IPage 排序被丢弃（L-05 未修）");
        assertSqlContains("order by");
    }

    // -------------------------------------------------------------------------
    // 回归 L-13：SortVo.asc==null 默认升序，不应 NPE（达梦方言）
    // -------------------------------------------------------------------------

    @Test
    void sortVoWithNullAscDefaultsToAscendingAndDoesNotNpeOnDameng() {
        // L-13（达梦）：SortVo.asc 是装箱 Boolean，前端可能不传（null）。
        // MybatisUtil.buildPage 实现：asc == null || asc → true（默认升序）。
        // 本测试验证：含 asc=null 的 SortVo 构造 PageVo 后调用 buildPage 不抛 NPE，
        // 且生成的 IPage orders 首项 isAsc()==true（默认升序语义正确）。
        // buildPage 逻辑与方言无关（纯 Java，不访问数据库），达梦/MySQL 结果应一致。
        com.baomidou.mybatisplus.extension.bo.PageVo pageVo = new com.baomidou.mybatisplus.extension.bo.PageVo();
        pageVo.setPageNum(1);
        pageVo.setPageSize(10);
        com.baomidou.mybatisplus.extension.bo.SortVo sortVo = new com.baomidou.mybatisplus.extension.bo.SortVo();
        // key 合法（仅字母/数字/下划线），asc 故意不设置（默认为 null）
        sortVo.setKey("id");
        // sortVo.setAsc() 故意省略，保持 null
        pageVo.setOrder(java.util.Collections.singletonList(sortVo));

        // 不应抛 NullPointerException
        IPage<MysqlUserDo> built = assertDoesNotThrow(
            () -> com.baomidou.mybatisplus.toolkit.MybatisUtil.buildPage(pageVo),
            "L-13：asc=null 时 buildPage 不应抛 NPE（达梦方言下）"
        );

        // orders 应含 1 项，且 isAsc()==true（null 默认升序）
        List<com.baomidou.mybatisplus.core.metadata.OrderItem> orders = built.orders();
        assertEquals(1, orders.size(), "应生成 1 个排序项");
        assertTrue(orders.get(0).isAsc(), "L-13：asc=null 应默认升序（isAsc()==true）");
    }
}
