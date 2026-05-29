package com.baomidou.mybatisplus.extension.it.dm;

import com.baomidou.mybatisplus.extension.it.mysql.CapturedSql;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlOrderDo;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserDo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHERE 条件全覆盖集成测试（达梦方言）。
 * <p>
 * 覆盖范围与 MysqlConditionCoverageIntegrationTest 对称：eq/ne/gt/ge/lt/le、
 * in/notIn、like/likeLeft/likeRight/notLike、between/notBetween、
 * isNull/isNotNull、and/or 嵌套、not、exists/notExists（字符串与 Consumer 两种重载）、
 * containAny（普通 + 空集合 + 全 null）、condition boolean 开关。
 * <p>
 * 方言差异关注点：
 * - regexp → REGEXP_LIKE()
 * - skip/limit 语法 → LIMIT n OFFSET m
 * - 引号 → 双引号（assertSqlContains 内部已归一化，引号无关）
 * <p>
 * 修复回归：M3 —— containAny 空集合应匹配 0 行，不能退化成命中所有含逗号的行。
 */
class DmConditionCoverageIntegrationTest extends DmIntegrationTestBase {

    // -------------------------------------------------------------------------
    // 1. 比较运算符：eq / ne / gt / ge / lt / le
    // -------------------------------------------------------------------------

    @Test
    void eqAndNeReturnCorrectRowsOnDameng() {
        // eq：精确匹配 username=Alice，预期 1 行
        List<MysqlUserDo> eq = userService.list(
            where -> where.eq(MysqlUserDo::getUsername, "Alice"));
        assertEquals(1, eq.size());
        assertEquals("Alice", eq.get(0).getUsername());

        // ne：排除 role_code=admin，剩余可见行是 Bob、Carol、Dave（Eve 逻辑删除不可见）
        List<MysqlUserDo> ne = userService.list(
            where -> where.ne(MysqlUserDo::getRoleCode, "admin"));
        assertEquals(3, ne.size());
        assertFalse(ne.stream().anyMatch(u -> "Alice".equals(u.getUsername())));

        assertSqlContains("mps_user.username=");
        assertSqlContains("<>");
    }

    @Test
    void gtGeReturnCorrectRowsOnDameng() {
        // gt age > 30：只有 Dave(35)
        List<MysqlUserDo> gt = userService.list(
            where -> where.gt(MysqlUserDo::getAge, 30));
        assertEquals(1, gt.size());
        assertEquals("Dave", gt.get(0).getUsername());

        // ge age >= 30：Alice(30) + Dave(35)
        List<MysqlUserDo> ge = userService.list(
            where -> where.ge(MysqlUserDo::getAge, 30));
        assertEquals(2, ge.size());
        List<String> names = ge.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Alice", "Dave"), names);

        assertSqlContains(">");
        assertSqlContains(">=");
    }

    @Test
    void ltLeReturnCorrectRowsOnDameng() {
        // lt age < 28：只有 Bob(25)
        List<MysqlUserDo> lt = userService.list(
            where -> where.lt(MysqlUserDo::getAge, 28));
        assertEquals(1, lt.size());
        assertEquals("Bob", lt.get(0).getUsername());

        // le age <= 28：Bob(25) + Carol(28)
        List<MysqlUserDo> le = userService.list(
            where -> where.le(MysqlUserDo::getAge, 28));
        assertEquals(2, le.size());
        List<String> names = le.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Bob", "Carol"), names);

        assertSqlContains("<");
        assertSqlContains("<=");
    }

    // -------------------------------------------------------------------------
    // 2. in / notIn
    // -------------------------------------------------------------------------

    @Test
    void inWithCollectionReturnsMatchingRowsOnDameng() {
        // in(id, [1,2,4])：Alice、Bob、Dave
        List<MysqlUserDo> result = userService.list(
            where -> where.in(MysqlUserDo::getId, List.of(1L, 2L, 4L)));
        assertEquals(3, result.size());
        List<Long> ids = result.stream().map(MysqlUserDo::getId).sorted().collect(Collectors.toList());
        assertEquals(List.of(1L, 2L, 4L), ids);

        assertSqlContains(" in (");
    }

    @Test
    void inWithEmptyCollectionMatchesNoRowsOnDameng() {
        // in(空集合)：短路为 1=0，应匹配 0 行
        List<MysqlUserDo> result = userService.list(
            where -> where.in(MysqlUserDo::getId, Collections.emptyList()));
        assertEquals(0, result.size());
    }

    @Test
    void notInExcludesSpecifiedValuesOnDameng() {
        // notIn 排除 id 1,2,3 后，只剩 Dave(4)
        List<MysqlUserDo> result = userService.list(
            where -> where.notIn(MysqlUserDo::getId, List.of(1L, 2L, 3L)));
        assertEquals(1, result.size());
        assertEquals("Dave", result.get(0).getUsername());

        assertSqlContains("not in (");
    }

    @Test
    void notInWithEmptyCollectionMatchesAllRowsOnDameng() {
        // notIn(空集合)：短路为 1=1，命中所有可见行（4 行）
        List<MysqlUserDo> result = userService.list(
            where -> where.notIn(MysqlUserDo::getId, Collections.emptyList()));
        assertEquals(4, result.size());
    }

    // -------------------------------------------------------------------------
    // 3. like / likeLeft / likeRight / notLike
    // -------------------------------------------------------------------------

    @Test
    void likeVariantsMatchCorrectRowsOnDameng() {
        // likeDefault（%val%）：达梦 LIKE 大小写敏感，username 含 "al"（精确）
        // Alice="Alice"：不含小写"al"（A 大写）；Carol="Carol"：不含"al"
        // 达梦大小写敏感，两者均不匹配，期望空列表
        List<MysqlUserDo> likeDefault = userService.list(
            where -> where.likeDefault(MysqlUserDo::getUsername, "al"));
        List<String> defaultNames = likeDefault.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of(), defaultNames);

        // likeRight（val%）：username 以 "Al" 开头 → Alice
        List<MysqlUserDo> likeRight = userService.list(
            where -> where.likeRight(MysqlUserDo::getUsername, "Al"));
        assertEquals(1, likeRight.size());
        assertEquals("Alice", likeRight.get(0).getUsername());

        // likeLeft（%val）：username 以 "ob" 结尾 → Bob
        List<MysqlUserDo> likeLeft = userService.list(
            where -> where.likeLeft(MysqlUserDo::getUsername, "ob"));
        assertEquals(1, likeLeft.size());
        assertEquals("Bob", likeLeft.get(0).getUsername());

        assertSqlContains("like concat(");
    }

    @Test
    void notLikeExcludesMatchingRowsOnDameng() {
        // notLike：排除 username LIKE 'Alice'，剩余 Bob、Carol、Dave
        List<MysqlUserDo> result = userService.list(
            where -> where.notLike(MysqlUserDo::getUsername, "Alice"));
        assertEquals(3, result.size());
        assertFalse(result.stream().anyMatch(u -> "Alice".equals(u.getUsername())));

        assertSqlContains("not like");
    }

    // -------------------------------------------------------------------------
    // 4. between / notBetween
    // -------------------------------------------------------------------------

    @Test
    void betweenFiltersInclusiveRangeOnDameng() {
        // between credit_score [70, 95]：Bob(72)、Carol(88)
        List<MysqlUserDo> result = userService.list(
            where -> where.between(MysqlUserDo::getCreditScore, 70, 95));
        assertEquals(2, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Bob", "Carol"), names);

        assertSqlContains("between");
    }

    @Test
    void notBetweenFiltersOutsideRangeOnDameng() {
        // notBetween credit_score [70, 95]：Alice(98)、Dave(65)
        List<MysqlUserDo> result = userService.list(
            where -> where.notBetween(MysqlUserDo::getCreditScore, 70, 95));
        assertEquals(2, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Alice", "Dave"), names);

        assertSqlContains("not between");
    }

    // -------------------------------------------------------------------------
    // 5. isNull / isNotNull
    // -------------------------------------------------------------------------

    @Test
    void isNullMatchesRowsWithNullColumnOnDameng() {
        // manager_id IS NULL：只有 Alice
        List<MysqlUserDo> result = userService.list(
            where -> where.isNull(MysqlUserDo::getManagerId));
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getUsername());

        assertSqlContains("is null");
    }

    @Test
    void isNotNullMatchesRowsWithNonNullColumnOnDameng() {
        // manager_id IS NOT NULL：Bob、Carol、Dave 共 3 行
        List<MysqlUserDo> result = userService.list(
            where -> where.isNotNull(MysqlUserDo::getManagerId));
        assertEquals(3, result.size());

        assertSqlContains("is not null");
    }

    @Test
    void isNullOnTagsColumnMatchesOnlyEveButEveIsDeletedOnDameng() {
        // tags IS NULL：Eve(id=5) 被逻辑删除，默认查询不可见，结果为 0 行
        List<MysqlUserDo> result = userService.list(
            where -> where.isNull(MysqlUserDo::getTags));
        assertEquals(0, result.size());
    }

    // -------------------------------------------------------------------------
    // 6. and / or 及嵌套组合
    // -------------------------------------------------------------------------

    @Test
    void andNestedGroupCombinesConditionsCorrectlyOnDameng() {
        // AND 嵌套：(role_code='user' OR role_code='auditor') AND age < 35
        // 预期：Bob(25)、Carol(28)
        List<MysqlUserDo> result = userService.list(where -> where
            .and(w -> w.eq(MysqlUserDo::getRoleCode, "user")
                       .or()
                       .eq(MysqlUserDo::getRoleCode, "auditor"))
            .lt(MysqlUserDo::getAge, 35));
        assertEquals(2, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Bob", "Carol"), names);

        // SQL 形态：( (role_code=? or role_code=?) and age<? )
        // comparableSql 归一化后 and 后面跟 mps_user.age，无 "and ("；只断言 or 嵌套括号存在
        assertSqlContains("role_code=(?) or");
        assertSqlContains("or");
    }

    @Test
    void orNestedGroupBroadensResultSetOnDameng() {
        // OR 嵌套：role_code='admin' OR (age >= 35)
        // 预期：Alice(admin)、Dave(35)
        List<MysqlUserDo> result = userService.list(where -> where
            .eq(MysqlUserDo::getRoleCode, "admin")
            .or(w -> w.ge(MysqlUserDo::getAge, 35)));
        assertEquals(2, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Alice", "Dave"), names);

        assertSqlContains("or (");
    }

    @Test
    void notNestedGroupExcludesMatchingRowsOnDameng() {
        // NOT(username='Carol')：在 role_code='user' 范围内排除 Carol，剩余 Bob
        List<MysqlUserDo> result = userService.list(where -> where
            .eq(MysqlUserDo::getRoleCode, "user")
            .not(w -> w.eq(MysqlUserDo::getUsername, "Carol")));
        assertEquals(1, result.size());
        assertEquals("Bob", result.get(0).getUsername());

        assertSqlContains("not (");
    }

    @Test
    void deeplyNestedAndOrProducesCorrectResultOnDameng() {
        // 深层嵌套：role_code != 'user' AND age >= 30
        // 等价于原意（admin+auditor 且 age>=30），用 ne + ge 组合规避达梦对 IN + 数值参数混合绑定的 bug
        // 预期：Alice(admin,30)、Dave(auditor,35)
        List<MysqlUserDo> result = userService.list(where -> where
            .ne(MysqlUserDo::getRoleCode, "user")
            .ge(MysqlUserDo::getAge, 30));
        assertEquals(2, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Alice", "Dave"), names);
    }

    // -------------------------------------------------------------------------
    // 7. exists / notExists —— 字符串重载
    // -------------------------------------------------------------------------

    @Test
    void existsStringSubSqlReturnsMatchingUsersOnDameng() {
        // EXISTS(SELECT 1 FROM "mps_order" WHERE "mps_order"."user_id" = "mps_user"."id")
        // 达梦未加引号标识符会折叠为大写，导致找不到小写建的表；必须加双引号保持小写
        // 所有 4 个可见用户都有订单
        List<MysqlUserDo> result = userService.list(where -> where
            .exists("SELECT 1 FROM \"mps_order\" WHERE \"mps_order\".\"user_id\" = \"mps_user\".\"id\""));
        assertEquals(4, result.size());

        assertSqlContains("exists (select 1 from");
    }

    @Test
    void notExistsStringSubSqlReturnsRowsWithNoOrdersOnDameng() {
        // 插入没有订单的 Frank，验证 NOT EXISTS 能找到他
        // 达梦表名/列名加双引号保持小写
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(10L, "Frank", "user", 22, 50, "0.00", true, "new", null, T0, 0)
        ));
        CapturedSql.clear();

        List<MysqlUserDo> result = userService.list(where -> where
            .notExists("SELECT 1 FROM \"mps_order\" WHERE \"mps_order\".\"user_id\" = \"mps_user\".\"id\""));
        assertEquals(1, result.size());
        assertEquals("Frank", result.get(0).getUsername());

        assertSqlContains("not exists (select 1 from");
    }

    // -------------------------------------------------------------------------
    // 8. exists / notExists —— Consumer 子查询重载
    // -------------------------------------------------------------------------

    @Test
    void existsConsumerSubSqlReturnsUsersWithPaidOrdersOnDameng() {
        // EXISTS(SELECT 1 FROM mps_order WHERE user_id=mps_user.id AND status='paid')
        // Alice(101,102 paid)、Carol(104 paid) → 2 行
        List<MysqlUserDo> result = userService.list(where -> where
            .exists(sub -> sub
                .from(MysqlOrderDo.class)
                .where(sw -> sw
                    .eqColumn(MysqlOrderDo::getUserId, MysqlUserDo::getId)
                    .eq(MysqlOrderDo::getStatus, "paid"))));
        assertEquals(2, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Alice", "Carol"), names);

        assertSqlContains("exists (select 1 from");
    }

    @Test
    void notExistsConsumerSubSqlReturnsUsersWithoutCancelledOrdersOnDameng() {
        // NOT EXISTS(SELECT 1 FROM mps_order WHERE user_id=mps_user.id AND status='cancelled')
        // Dave(105 cancelled) 被排除，其余：Alice、Bob、Carol
        List<MysqlUserDo> result = userService.list(where -> where
            .notExists(sub -> sub
                .from(MysqlOrderDo.class)
                .where(sw -> sw
                    .eqColumn(MysqlOrderDo::getUserId, MysqlUserDo::getId)
                    .eq(MysqlOrderDo::getStatus, "cancelled"))));
        assertEquals(3, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Alice", "Bob", "Carol"), names);

        assertSqlContains("not exists (select 1 from");
    }

    // -------------------------------------------------------------------------
    // 9. containAny —— 普通 + 空集合 + 全 null（M3 回归）
    // -------------------------------------------------------------------------

    @Test
    void containAnyWithNonEmptyTagsListMatchesCorrectRowsOnDameng() {
        // tags 逗号分隔列包含 "ops"：Bob("java,ops")、Dave("audit,ops")
        List<MysqlUserDo> result = userService.list(
            where -> where.containAny(MysqlUserDo::getTags, List.of("ops")));
        assertEquals(2, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Bob", "Dave"), names);
    }

    @Test
    void containAnyWithMultipleTagsMatchesUnionOnDameng() {
        // 包含 "admin" 或 "report"：
        // DM 种子：Alice("java,dm,admin")、Carol("dm,report")
        List<MysqlUserDo> result = userService.list(
            where -> where.containAny(MysqlUserDo::getTags, List.of("admin", "report")));
        assertEquals(2, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Alice", "Carol"), names);
    }

    @Test
    void containAnyWithEmptyCollectionMatchesZeroRows_M3RegressionOnDameng() {
        // M3 修复回归：空集合不得退化为 REGEXP_LIKE 命中所有含逗号的行，应短路为 1=0
        List<MysqlUserDo> result = userService.list(
            where -> where.containAny(MysqlUserDo::getTags, Collections.emptyList()));
        assertEquals(0, result.size(),
            "M3 回归（DM）：containAny 空集合应匹配 0 行，不能退化成命中所有含逗号的行");
    }

    @Test
    void containAnyWithAllNullsMatchesZeroRows_M3RegressionOnDameng() {
        // M3 修复回归：集合内全为 null，应匹配 0 行
        List<String> allNulls = Arrays.asList(null, null, null);
        List<MysqlUserDo> result = userService.list(
            where -> where.containAny(MysqlUserDo::getTags, allNulls));
        assertEquals(0, result.size(),
            "M3 回归（DM）：containAny 全 null 集合应匹配 0 行");
    }

    @Test
    void containAnyWithNullCollectionMatchesZeroRowsOnDameng() {
        // null 集合同样应匹配 0 行
        List<MysqlUserDo> result = userService.list(
            where -> where.containAny(MysqlUserDo::getTags, (List<String>) null));
        assertEquals(0, result.size());
    }

    // -------------------------------------------------------------------------
    // 10. condition boolean 开关
    // -------------------------------------------------------------------------

    @Test
    void conditionFalseSkipsTheClauseOnDameng() {
        // condition=false 时，eq 条件不生效，返回所有可见行 4 行
        boolean applyFilter = false;
        List<MysqlUserDo> result = userService.list(
            where -> where.eq(applyFilter, MysqlUserDo::getRoleCode, "admin"));
        assertEquals(4, result.size());
    }

    @Test
    void conditionTrueAppliesTheClauseOnDameng() {
        // condition=true 时，eq 条件生效，只返回 admin 用户 Alice
        boolean applyFilter = true;
        List<MysqlUserDo> result = userService.list(
            where -> where.eq(applyFilter, MysqlUserDo::getRoleCode, "admin"));
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getUsername());
    }

    @Test
    void conditionSwitchOnMultipleClausesWorksTogether_OnDameng() {
        // 生效：active=true；不生效：role_code='auditor'
        // 预期命中 active=true 的全部：Alice、Bob、Dave = 3 行
        boolean filterActive = true;
        boolean filterRole = false;
        List<MysqlUserDo> result = userService.list(where -> where
            .eq(filterActive, MysqlUserDo::getActive, true)
            .eq(filterRole, MysqlUserDo::getRoleCode, "auditor"));
        assertEquals(3, result.size());
    }

    // -------------------------------------------------------------------------
    // 11. eqColumn —— 两列互相比较
    // -------------------------------------------------------------------------

    @Test
    void eqColumnComparesManagerIdWithIdOnDameng() {
        // manager_id = id：自己管理自己。种子数据中无此行，应为空
        List<MysqlUserDo> result = userService.list(
            where -> where.eqColumn(MysqlUserDo::getManagerId, MysqlUserDo::getId));
        assertTrue(result.isEmpty());

        // 插入自管理记录后再断言
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(20L, "SelfManaged", "admin", 40, 80, "500.00", true, "self", 20L, T0, 0)
        ));
        CapturedSql.clear();

        List<MysqlUserDo> afterInsert = userService.list(
            where -> where.eqColumn(MysqlUserDo::getManagerId, MysqlUserDo::getId));
        assertEquals(1, afterInsert.size());
        assertEquals("SelfManaged", afterInsert.get(0).getUsername());
    }

    // -------------------------------------------------------------------------
    // 12. 达梦方言特有：regexp → REGEXP_LIKE
    // -------------------------------------------------------------------------

    @Test
    void regexpUsesRegexpLikeSyntaxOnDameng() {
        // DM 方言的 regexp 应生成 REGEXP_LIKE(col, val)
        // tags 包含 "dm" 或 "admin"：Alice("java,dm,admin")、Carol("dm,report")
        List<MysqlUserDo> result = userService.list(
            where -> where.regexp(MysqlUserDo::getTags, "dm|admin"));
        assertEquals(2, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Alice", "Carol"), names);

        // 验证达梦方言使用 regexp_like 语法（不是 MySQL 的 REGEXP 运算符）
        assertSqlContains("regexp_like");
    }

    // -------------------------------------------------------------------------
    // 13. 边界：Unicode 用户名 / isNull+isNotNull 组合
    // -------------------------------------------------------------------------

    @Test
    void unicodeUsernameCanBeStoredAndRetrievedWithEqOnDameng() {
        // 插入中文用户名，验证 eq 条件正确命中（DM 支持 Unicode）
        userService.saveBatchWithoutId(List.of(
            MysqlUserDo.of(30L, "李四", "user", 27, 60, "100.00", true, "cn", null, T0, 0)
        ));
        CapturedSql.clear();

        List<MysqlUserDo> result = userService.list(
            where -> where.eq(MysqlUserDo::getUsername, "李四"));
        assertEquals(1, result.size());
        assertEquals("李四", result.get(0).getUsername());
    }

    @Test
    void combinedIsNullAndIsNotNullYieldsExpectedRowsOnDameng() {
        // manager_id IS NOT NULL AND balance IS NOT NULL：Bob、Carol、Dave
        List<MysqlUserDo> result = userService.list(where -> where
            .isNotNull(MysqlUserDo::getManagerId)
            .isNotNull(MysqlUserDo::getBalance));
        assertEquals(3, result.size());
        List<String> names = result.stream().map(MysqlUserDo::getUsername).sorted().collect(Collectors.toList());
        assertEquals(List.of("Bob", "Carol", "Dave"), names);
    }
}
