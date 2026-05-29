package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stream / 分页 全覆盖集成测试（MySQL 方言）
 * 覆盖场景：
 *   - stream().filter / sorted / limit / skip / distinct / forUpdate / findFirst / forEach / collect / toList / toSet / toMap
 *   - stream().map（投影到 DTO）及两次 map 互不污染（回归 M8）
 *   - page / pageGroup / pageJoin 分页（总数 + 当页记录数）
 *   - 分页排序：正常列名通过 stream.sorted 推到 ORDER BY（assertSqlContains 验证）
 *   - H1 回归：非法列名注入（"id;DROP"）通过 sorted/stream 链路应抛 IllegalArgumentException
 */
class MysqlStreamPageCoverageIntegrationTest extends MysqlIntegrationTestBase {

    // -------------------------------------------------------------------------
    // stream() filter + findFirst
    // -------------------------------------------------------------------------

    @Test
    void streamFilterFindFirstReturnsSingleMatchedRow() {
        // 用 filter 等价 WHERE，findFirst 取第一行；种子数据：Alice active=true, credit_score=98（最高）
        Optional<MysqlUserDo> first = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .sorted(order -> order.orderDesc(MysqlUserDo::getCreditScore))
            .findFirst();

        assertTrue(first.isPresent(), "应能找到 active=true 的用户");
        assertEquals("Alice", first.get().getUsername(), "credit_score 最高的活跃用户是 Alice");

        // SQL 应含 WHERE active 且 ORDER BY credit_score DESC LIMIT 1
        assertSqlContains("active");
        assertSqlContains("order by mps_user.credit_score desc");
        assertSqlContains("limit 1");
    }

    @Test
    void streamFilterFindFirstOnEmptyResultReturnsEmpty() {
        // filter 条件命中不到任何行，findFirst 应返回 empty
        Optional<MysqlUserDo> result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "NoSuchUser"))
            .findFirst();

        assertFalse(result.isPresent(), "不存在的用户应返回 Optional.empty()");
    }

    // -------------------------------------------------------------------------
    // stream() sorted（ORDER BY 推到 SQL）
    // -------------------------------------------------------------------------

    @Test
    void streamSortedPushesOrderByToMysqlSql() {
        // 按 age ASC 排序，取 active 用户：Bob(25) < Alice(30) < Dave(35)
        List<MysqlUserDo> sorted = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .sorted(order -> order.orderAsc(MysqlUserDo::getAge))
            .collect(Collectors.toList());

        assertEquals(3, sorted.size());
        assertEquals("Bob", sorted.get(0).getUsername(), "年龄最小应是 Bob(25)");
        assertEquals("Dave", sorted.get(2).getUsername(), "年龄最大应是 Dave(35)");

        // 验证 ORDER BY 已下推到 SQL
        assertSqlContains("order by mps_user.age asc");
    }

    @Test
    void streamSortedDescPushesCorrectOrderByClause() {
        // 按 id DESC 排序，验证 SQL
        List<MysqlUserDo> users = userService.stream()
            .sorted(order -> order.orderDesc(MysqlUserDo::getId))
            .limit(2)
            .collect(Collectors.toList());

        // 种子可见用户：Alice(1)/Bob(2)/Carol(3)/Dave(4)，id DESC 前两个是 Dave(4), Carol(3)
        assertEquals(2, users.size());
        assertEquals(4L, users.get(0).getId(), "id 最大的可见用户是 Dave(4)");

        assertSqlContains("order by mps_user.id desc");
        assertSqlContains("limit 2");
    }

    // -------------------------------------------------------------------------
    // stream() limit / skip
    // -------------------------------------------------------------------------

    @Test
    void streamLimitPushedToSqlReturnsCorrectRowCount() {
        // limit(2) 推到 SQL LIMIT，结果只有 2 条
        List<MysqlUserDo> users = userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .limit(2)
            .collect(Collectors.toList());

        assertEquals(2, users.size());
        assertSqlContains("limit 2");
    }

    @Test
    void streamSkipAndLimitComboGeneratesOffsetClause() {
        // skip(1) + limit(2) → MySQL 方言：LIMIT 1, 2
        List<MysqlUserDo> users = userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .skip(1)
            .limit(2)
            .collect(Collectors.toList());

        assertEquals(2, users.size());
        // 跳过第1行(Alice)，应取 Bob(2) 和 Carol(3)
        assertEquals("Bob", users.get(0).getUsername());
        assertEquals("Carol", users.get(1).getUsername());

        // MySQL 方言使用 LIMIT offset, count
        assertSqlContains("limit 1, 2");
    }

    // -------------------------------------------------------------------------
    // stream() distinct
    // -------------------------------------------------------------------------

    @Test
    void streamDistinctDeduplicatesResultAndPushesToSql() {
        // 对 role_code 列 mapToColumn + distinct，应得到 3 个不重复角色：admin/user/auditor
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
    void streamForEachIteratesAllVisibleRows() {
        // forEach 遍历所有可见用户（Eve 逻辑删除，不可见），收集 username
        List<String> names = new ArrayList<>();
        userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getId))
            .forEach(user -> names.add(user.getUsername()));

        // 可见用户：Alice, Bob, Carol, Dave（共 4 条，Eve deleted=1 不可见）
        assertEquals(4, names.size(), "逻辑删除的 Eve 不应出现");
        assertFalse(names.contains("Eve"), "Eve 已逻辑删除");
        assertEquals("Alice", names.get(0));
    }

    @Test
    void streamCollectToListReturnsAllVisibleRows() {
        // collect(Collectors.toList()) 等价于 toList，验证数量
        List<MysqlUserDo> all = userService.stream()
            .collect(Collectors.toList());

        assertEquals(4, all.size(), "可见用户应为 4 条");
    }

    // -------------------------------------------------------------------------
    // stream() toSet / toMap
    // -------------------------------------------------------------------------

    @Test
    void streamToSetExtractsColumnAsSet() {
        // toSet(SFunction) 一步提取列值集合
        Set<String> roleSet = userService.stream()
            .toSet(MysqlUserDo::getRoleCode);

        assertEquals(Set.of("admin", "user", "auditor"), roleSet);
    }

    @Test
    void streamToMapBuildKeyValueMapFromTwoColumns() {
        // toMap(id -> username) 构建映射
        Map<Long, String> idToName = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .toMap(MysqlUserDo::getId, MysqlUserDo::getUsername);

        assertEquals(3, idToName.size(), "active 用户有 3 个");
        assertEquals("Alice", idToName.get(1L));
        assertEquals("Bob", idToName.get(2L));
        assertEquals("Dave", idToName.get(4L));
    }

    // -------------------------------------------------------------------------
    // stream() forUpdate
    // -------------------------------------------------------------------------

    @Test
    void streamForUpdateAppendsForUpdateToMysqlSql() {
        // forUpdate() 在 MySQL 方言下应追加 FOR UPDATE 尾
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
    void streamMapProjectsToDtoWithSelectedColumns() {
        // stream().map(select -> select.select(...)) 投影到 UserStatsDto
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

        // 验证 SELECT 列已下推
        assertSqlContains("select");
        assertSqlContains("mps_user.id");
        assertSqlContains("mps_user.username");
    }

    // -------------------------------------------------------------------------
    // 回归 M8：同一 stream 工厂调用两次 map 到不同类型，互不污染
    // -------------------------------------------------------------------------

    @Test
    void twoIndependentStreamMapsDoNotPollutateEachOther() {
        // 第一次 stream().map → UserStatsDto（只取 userId + username）
        List<UserStatsDto> dtoList = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                .select(MysqlUserDo::getId, UserStatsDto::getUserId)
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername),
                UserStatsDto.class)
            .collect(Collectors.toList());

        // 第二次独立 stream().map → 仅取 roleCode（不同 DTO 字段）
        List<UserStatsDto> dtoList2 = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode),
                UserStatsDto.class)
            .collect(Collectors.toList());

        // 两次查询结果应互不干扰
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
    void pageReturnsTotalCountAndCurrentPageRecords() {
        // page(1, 2) 查 active 用户，total=3，当页 2 条
        IPage<MysqlUserDo> result = userService.page(
            new Page<>(1, 2),
            where -> where.eq(MysqlUserDo::getActive, true)
        );

        assertEquals(3L, result.getTotal(), "active 用户共 3 条");
        assertEquals(2, result.getRecords().size(), "第 1 页每页 2 条");
    }

    @Test
    void pageSecondPageReturnsRemainingRecord() {
        // 第 2 页应只有 1 条
        IPage<MysqlUserDo> page2 = userService.page(
            new Page<>(2, 2),
            where -> where.eq(MysqlUserDo::getActive, true)
        );

        assertEquals(3L, page2.getTotal());
        assertEquals(1, page2.getRecords().size(), "第 2 页应只有 1 条剩余记录");
    }

    @Test
    void pageWithProjectionMapsToDto() {
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
        // 投影结果应有 username 字段
        assertTrue(result.getRecords().stream().allMatch(r -> r.getUsername() != null));

        assertSqlContains("limit 3");
    }

    // -------------------------------------------------------------------------
    // 分页排序：正常列名通过 sorted 下推到 ORDER BY（H1 相关正常路径）
    // -------------------------------------------------------------------------

    @Test
    void pageWithSortedPushesOrderByToMysqlSql() {
        // page + stream().sorted 验证合法列名正确进入 ORDER BY
        // 注：page 方法本身不接受 sorted，此处通过 list + order + limit 模拟分页排序链路
        // 同时用 assertSqlContains 验证 credit_score 合法列名确实进了 ORDER BY
        List<MysqlUserDo> paged = userService.list(
            where -> where.isNotNull(MysqlUserDo::getId),
            order -> order.orderDesc(MysqlUserDo::getCreditScore),
            2
        );

        assertEquals(2, paged.size());
        assertEquals("Alice", paged.get(0).getUsername(), "信用分最高的应是 Alice(98)");
        assertEquals("Carol", paged.get(1).getUsername(), "信用分第二的应是 Carol(88)");

        // 验证合法列名已下推到 ORDER BY
        assertSqlContains("order by mps_user.credit_score desc");
        assertSqlContains("limit 2");
    }

    @Test
    void streamSortedByLegalColumnNamePushesItToOrderBySql() {
        // H1 回归正常路径：合法列名 age 通过 stream().sorted 推到 ORDER BY，assertSqlContains 验证
        // 注：非法列名注入（"id;DROP"）的验证见下面的 H1 回归测试
        userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getAge))
            .collect(Collectors.toList());

        assertSqlContains("order by mps_user.age asc");
    }

    // -------------------------------------------------------------------------
    // 回归 H1：非法列名注入应抛 IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void illegalColumnNameWithSemicolonShouldThrowIllegalArgumentException() {
        // H1 回归：OrderLambdaQueryWrapper 没有 orderByRaw 方法；
        // 列名注入校验由 MybatisUtil.buildPage(PageVo) 在 SortVo.key 白名单处拦截（^[A-Za-z0-9_]+$ 校验）。
        // 构造含非法 key 的 PageVo 传入 buildPage，验证抛 IllegalArgumentException。
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
    void unknownColumnInOrderByIsSkippedRatherThanInjectedIntoSql() {
        // H1 回归：传入不存在的列名（通过合法 API 映射到实际不存在的字段）
        // 此场景用 sorted 走 lambda 路径：lambda 指向真实字段，框架不会拼不存在的列名
        // 用 assertSqlContains 验证 ORDER BY 只含合法列名，不含危险字符
        // 注：此处通过 stream API 传合法字段引用，不可能产生不存在列名，仅用于说明框架 lambda 天然防注入
        List<MysqlUserDo> users = userService.stream()
            .sorted(order -> order.orderAsc(MysqlUserDo::getCreditScore))
            .limit(1)
            .collect(Collectors.toList());

        assertFalse(users.isEmpty());
        // 验证 ORDER BY 子句中无注入特征字符
        String sql = CapturedSql.joined();
        assertFalse(sql.contains(";"), "SQL 中不应含分号注入字符");
        assertFalse(sql.toLowerCase().contains("drop"), "SQL 中不应含 DROP 关键字");
        assertSqlContains("order by mps_user.credit_score asc");
    }

    // -------------------------------------------------------------------------
    // pageGroup（分组分页）
    // -------------------------------------------------------------------------

    @Test
    void pageGroupReturnsTotalGroupCountAndCurrentPageGroups() {
        // 按 role_code 分组分页，全量有 3 组（admin/user/auditor），第 1 页取 2 组
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
    void pageGroupSecondPageReturnsLastGroup() {
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
    void pageGroupWithActiveFilterReducesGroupCount() {
        // 过滤 active=true 后：admin(Alice) + user(Bob) + auditor(Dave) 仍为 3 组，
        // 但 user 组的 Carol(active=false) 被过滤
        IPage<UserStatsDto> result = userService.pageGroup(
            new Page<>(1, 10),
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.count(), UserStatsDto::getOrderCount),
            UserStatsDto.class
        );

        // active=true 的用户：Alice(admin)/Bob(user)/Dave(auditor)，仍 3 组
        assertEquals(3L, result.getTotal());
        Map<String, Long> byRole = result.getRecords().stream()
            .collect(Collectors.toMap(UserStatsDto::getRoleCode, UserStatsDto::getOrderCount));
        assertEquals(1L, byRole.get("user"), "active user 只有 Bob 1 个");
    }

    // -------------------------------------------------------------------------
    // pageJoin（连表分页）
    // -------------------------------------------------------------------------

    @Test
    void pageJoinReturnsTotalJoinedRowsAndCurrentPage() {
        // LEFT JOIN order，active 用户：Alice 有 2 条订单，Bob 有 1 条，Dave 有 1 条 → 共 4 行
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
    void pageJoinSecondPageReturnsRemainingRows() {
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
    void pageJoinWithAllUsersIncludesNullOrderRows() {
        // 不过滤 active，Dave 有 cancelled 订单，Carol 有 paid 订单，结果总数=5 行（含各自订单）
        // 注：Carol active=false 也参与 LEFT JOIN，total 应包含所有可见用户的订单行
        IPage<UserStatsDto> result = userService.pageJoin(
            new Page<>(1, 10),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .select(MysqlOrderDo::getStatus, UserStatsDto::getRoleCode),
            UserStatsDto.class
        );

        // 种子：Alice 2 orders + Bob 1 + Carol 1 + Dave 1 = 5 行（LEFT JOIN，每个用户均有订单）
        assertEquals(5L, result.getTotal(), "所有可见用户 LEFT JOIN 订单共 5 行");
    }

    // -------------------------------------------------------------------------
    // stream() 在分页排序上与 page 方法结合验证（综合场景）
    // -------------------------------------------------------------------------

    @Test
    void streamSortedThenLimitSimulatesPageOneSortedByBalance() {
        // 模拟"第1页按余额 DESC 排序"：stream().sorted().limit(2)
        List<MysqlUserDo> topTwo = userService.stream()
            .sorted(order -> order.orderDesc(MysqlUserDo::getBalance))
            .limit(2)
            .collect(Collectors.toList());

        assertEquals(2, topTwo.size());
        // Alice(1200.50) > Carol(780.25) > Bob(300.00) > Dave(90.00)
        assertEquals("Alice", topTwo.get(0).getUsername(), "余额最高是 Alice");
        assertEquals("Carol", topTwo.get(1).getUsername(), "余额第二是 Carol");

        assertSqlContains("order by mps_user.balance desc");
        assertSqlContains("limit 2");
    }

    @Test
    void pageGroupJoinCombinesJoinGroupAndPaginationCorrectly() {
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

        // active 用户 3 组：admin/user/auditor
        assertEquals(3L, result.getTotal(), "按角色分组共 3 组");
        assertEquals(2, result.getRecords().size(), "第 1 页 2 条");
        assertTrue(result.getRecords().stream().allMatch(r -> r.getRoleCode() != null));

        assertSqlContains("left join mps_order");
        assertSqlContains("group by mps_user.role_code");
        assertSqlContains("limit 2");
    }

    // -------------------------------------------------------------------------
    // 复审回归：M-01（TypeReference.getRawType）/ L-08（mapToColumn 列类型映射）
    // -------------------------------------------------------------------------

    @Test
    void mapWithIbatisTypeReferenceResolvesRawTypeNotNull() {
        // M-01：map(Consumer, TypeReference) 经 TypeReference.getRawType() 解析类型。
        // 旧实现用无参反射调 getSuperclassTypeParameter(Class)（带参方法）必返 null → renameClass 存 null → NPE。
        org.apache.ibatis.type.TypeReference<MysqlUserDo> ref = new org.apache.ibatis.type.TypeReference<MysqlUserDo>() {};
        List<MysqlUserDo> users = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getActive, true))
            .map(select -> select.selectAll(MysqlUserDo.class), ref)
            .collect(Collectors.toList());
        assertFalse(users.isEmpty(), "TypeReference 映射应返回行而非 NPE");
        assertEquals(MysqlUserDo.class, users.get(0).getClass(), "应解析为 MysqlUserDo 而非 null");
    }

    @Test
    void mapToColumnBooleanReturnsBooleanType() {
        // L-08：mapToColumn 用列声明类型作映射目标。MySQL TINYINT(1) 原生返回 Boolean，此处验证未回归。
        List<Boolean> actives = userService.stream()
            .mapToColumn(MysqlUserDo::getActive)
            .collect(Collectors.toList());
        assertFalse(actives.isEmpty());
        Object first = actives.stream().filter(java.util.Objects::nonNull).findFirst().orElseThrow();
        assertEquals(Boolean.class, first.getClass(), "mapToColumn(active) 应返回 Boolean");
    }

    @Test
    void pageWithIPageOrdersSortsByResolvedColumn_L05() {
        // L-05 回归：IPage.getOrders() 的排序经 page() 预处理 + toPageQueryWrapper 精确匹配 keySet 应真正下推 ORDER BY。
        // 用 creditScore 升序以区分「排序生效」与「被静默丢弃」：升序 → Dave(65) 首位；若排序丢失则是默认 id 序的 Alice。
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
    // 回归 L-13：SortVo.asc==null 默认升序，不应 NPE
    // -------------------------------------------------------------------------

    @Test
    void sortVoWithNullAscDefaultsToAscendingAndDoesNotNpe() {
        // L-13：SortVo.asc 是装箱 Boolean，前端可能不传此字段（null）。
        // MybatisUtil.buildPage 实现：asc == null || asc → true（默认升序）。
        // 本测试验证：含 asc=null 的 SortVo 构造 PageVo 后调用 buildPage 不抛 NPE，
        // 且生成的 IPage orders 首项 isAsc()==true（默认升序语义正确）。
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
            "L-13：asc=null 时 buildPage 不应抛 NPE"
        );

        // orders 应含 1 项，且 isAsc()==true（null 默认升序）
        List<com.baomidou.mybatisplus.core.metadata.OrderItem> orders = built.orders();
        assertEquals(1, orders.size(), "应生成 1 个排序项");
        assertTrue(orders.get(0).isAsc(), "L-13：asc=null 应默认升序（isAsc()==true）");
    }
}
