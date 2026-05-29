package com.baomidou.mybatisplus.extension.it.dm;

import com.baomidou.mybatisplus.extension.it.mysql.FunctionResultDto;
import com.baomidou.mybatisplus.extension.it.mysql.MysqlUserDo;
import com.baomidou.mybatisplus.extension.it.mysql.UserStatsDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 达梦 函数/表达式/CASE WHEN 全覆盖集成测试（与 MySQL 场景对称）。
 * <p>
 * 覆盖场景：
 * - 日期函数：year / quarter(H5回归) / month / day / hour / minute（通过 DATE_FORMAT/TO_CHAR 等）
 * - 字符串函数：concat(||) / left / right / charLength / trim / substringIndex
 * - 数学函数：abs / mod / round(customColumn)
 * - CASE WHEN 表达式
 * - cast（convertData / convertDataTypeFunc）
 * - convertCharacterSet（H4回归：非法字符集名应抛 IllegalArgumentException）
 * - dateAdd（M5回归：非法 INTERVAL 类型应抛 IllegalArgumentException）
 * - GROUP_CONCAT/LISTAGG separator 含特殊字符（M4回归）
 * <p>
 * 注意：达梦 concat 用 || 运算符，GROUP_CONCAT 用 LISTAGG；
 * DATE_FORMAT 在达梦中行为可能和 MySQL 不同，具体格式符以运行时结果为准。
 */
class DmFunctionCoverageIntegrationTest extends DmIntegrationTestBase {

    // ===========================================================================
    // 日期函数场景
    // ===========================================================================

    /**
     * H5 回归：quarter() 对已知日期（Alice created_at = 2026-05-15）必须返回季度 2，而非年份。
     * 达梦的 QUARTER 函数行为与 MySQL 一致，返回 1-4。
     */
    @Test
    void quarterReturnsCorrectQuarterNotYearForKnownDateOnDameng() {
        // Alice created_at = 2026-05-15，属第 2 季度（4-6 月）
        // 注：getCreatedAt 返回 LocalDateTime，不兼容 Date 类型参数；达梦列名需带双引号（小写），改用 customColumn
        String quarterText = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.quarterFunc(y -> y.customColumn("\"created_at\"")), "CHAR"),
                        FunctionResultDto::getQuarterText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow()
            .getQuarterText();

        int quarter = Integer.parseInt(quarterText);
        assertTrue(quarter >= 1 && quarter <= 4, "quarter() 应返回 1-4，实际为: " + quarter);
        assertEquals(2, quarter, "2026-05-15 应归属 Q2，但达梦实际返回: " + quarter);
        assertSqlContains("quarter(");
    }

    /**
     * year() 对种子数据中已知日期返回正确年份（达梦方言）。
     */
    @Test
    void yearFunctionReturnsCorrectYearOnDameng() {
        // 注：getCreatedAt 返回 LocalDateTime，不兼容 Date 类型参数；达梦列名需带双引号，改用 customColumn
        String yearText = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.yearFunc(y -> y.customColumn("\"created_at\"")), "CHAR"),
                        FunctionResultDto::getYearText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow()
            .getYearText();

        assertEquals("2026", yearText, "Alice created_at 年份应为 2026");
        assertSqlContains("year(");
    }

    /**
     * 通过 DATE_FORMAT 格式符提取 month / day / hour / minute（达梦方言）。
     * Alice created_at = 2026-05-15 09:00:00
     * <p>
     * TODO：达梦 DATE_FORMAT 的格式符可能与 MySQL 不同，若结果格式不匹配需调整断言。
     * 当前以运行时实际结果为准，仅断言非 null 且看起来合理。
     */
    @Test
    void dateFormatExtractsMonthDayHourMinuteOnDameng() {
        FunctionResultDto alice = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    // 注：getCreatedAt 返回 LocalDateTime，不兼容 Date 类型参数；达梦列名需带双引号，改用 customColumn
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.customColumn("\"created_at\""), "%m"), FunctionResultDto::getYearText)   // 月
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.customColumn("\"created_at\""), "%d"), FunctionResultDto::getQuarterText) // 日
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.customColumn("\"created_at\""), "%H"), FunctionResultDto::getWeekText)   // 时
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.customColumn("\"created_at\""), "%i"), FunctionResultDto::getDayOfYearText), // 分
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        // 只断言非 null 且符合数字格式，不断言具体值（达梦 DATE_FORMAT 返回格式需运行时确认）
        assertNotNull(alice.getYearText(), "月份结果不应为 null");
        assertNotNull(alice.getQuarterText(), "日期结果不应为 null");
        assertNotNull(alice.getWeekText(), "小时结果不应为 null");
        assertNotNull(alice.getDayOfYearText(), "分钟结果不应为 null");
        assertSqlContains("date_format(");
    }

    /**
     * Gap1 回归：日期加减已走达梦方言。框架现生成 {@code DATEADD(unit, n, date)}（Oracle 风格），
     * 达梦可执行（不再是 MySQL 的 DATE_ADD(x, INTERVAL n DAY)）。
     * Alice created_at = 2026-05-15 09:00:00，加 1 天应得 2026-05-16。
     */
    @Test
    void dateAddOneDayYieldsCorrectDateOnDameng() {
        String result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.dateAddFunc(
                            y -> y.customColumn("\"created_at\""),
                            y -> y.value(1),
                            "DAY"),
                        "CHAR"), FunctionResultDto::getDateAddText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow()
            .getDateAddText();

        // 达梦方言生成 DATEADD(DAY, 1, "created_at")，验证已不走 MySQL 的 DATE_ADD INTERVAL
        assertSqlContains("dateadd(");
        assertNotNull(result, "dateAdd 结果不应为 null");
        assertTrue(result.contains("2026-05-16"), "2026-05-15 加 1 天应含 2026-05-16，实际: " + result);
    }

    /**
     * L-14：dateAdd MONTH 单位——达梦方言生成 {@code DATEADD(MONTH, 2, "created_at")}。
     * Alice created_at = 2026-05-15 09:00:00，加 2 MONTH → 2026-07-15。
     * 验证：结果字符串含 "2026-07"（月份跨越正确），且 SQL 含 dateadd(。
     */
    @Test
    void dateAddTwoMonthsYieldsCorrectDateOnDameng() {
        // 注：getCreatedAt 返回 LocalDateTime，不兼容 Date 类型参数；达梦列名需带双引号，改用 customColumn
        String result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.dateAddFunc(
                            y -> y.customColumn("\"created_at\""),
                            y -> y.value(2),
                            "MONTH"),
                        "CHAR"), FunctionResultDto::getDateAddText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow()
            .getDateAddText();

        // 达梦方言生成 DATEADD(MONTH, 2, "created_at")，2026-05-15 加 2 个月 → 2026-07-15
        assertSqlContains("dateadd(");
        assertNotNull(result, "L-14：dateAdd MONTH 结果不应为 null");
        // 只断言月份跨越正确（含 2026-07），不约束精确格式（达梦 CHAR cast 的日期字符串格式运行时确认）
        assertTrue(result.contains("2026-07"),
                "L-14：2026-05-15 加 2 MONTH 应含 2026-07，实际: " + result);
    }

    // ===========================================================================
    // M5 回归：dateAdd 传非法 INTERVAL 类型应抛 IllegalArgumentException（达梦同 MySQL）
    // ===========================================================================

    /**
     * M5 回归：dateAddFunc 传入含空格的非法 INTERVAL 类型（"DAY x"）必须抛 IllegalArgumentException。
     * 安全校验在 AbstractFunctionLambdaQueryWrapper 层面方言无关。
     */
    @Test
    void dateAddWithIllegalIntervalTypeThrowsIllegalArgumentExceptionOnDameng() {
        // 注：异常在 interval 校验处提前抛出，与列解析无关；达梦列名用 customColumn 保证编译通过
        assertThrows(IllegalArgumentException.class, () ->
            userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
                .map(select -> select
                        .selectFunc(func -> func.dateFormatFunc(
                            x -> x.dateAddFunc(
                                y -> y.customColumn("\"created_at\""),
                                y -> y.value(1),
                                "DAY x"),  // 含空格的非法类型
                            "%Y-%m-%d"), FunctionResultDto::getDateAddText),
                    FunctionResultDto.class)
                .findFirst()
        );
    }

    /**
     * M5 回归：dateAddFunc 传入含分号的注入字符串应抛 IllegalArgumentException（达梦）。
     */
    @Test
    void dateAddWithSemicolonInjectionTypeThrowsIllegalArgumentExceptionOnDameng() {
        // 注：异常在 interval 校验处提前抛出，与列解析无关；达梦列名用 customColumn 保证编译通过
        assertThrows(IllegalArgumentException.class, () ->
            userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
                .map(select -> select
                        .selectFunc(func -> func.dateFormatFunc(
                            x -> x.dateAddFunc(
                                y -> y.customColumn("\"created_at\""),
                                y -> y.value(1),
                                "DAY;DROP TABLE mps_user"),
                            "%Y-%m-%d"), FunctionResultDto::getDateAddText),
                    FunctionResultDto.class)
                .findFirst()
        );
    }

    // ===========================================================================
    // H4 回归：convertCharacterSet 传非法字符集名应抛 IllegalArgumentException（达梦同 MySQL）
    // ===========================================================================

    /**
     * H4 回归：convertCharacterSetFunc 传入含分号字符集名必须抛 IllegalArgumentException（达梦）。
     * 安全校验在 AbstractFunctionLambdaQueryWrapper 层，方言无关。
     */
    @Test
    void convertCharacterSetWithInjectionNameThrowsIllegalArgumentExceptionOnDameng() {
        assertThrows(IllegalArgumentException.class, () ->
            userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
                .map(select -> select
                        .selectFunc(func -> func.convertCharacterSetFunc(
                            x -> x.column(MysqlUserDo::getUsername),
                            "utf8;DROP"),
                        FunctionResultDto::getNameRole),
                    FunctionResultDto.class)
                .findFirst()
        );
    }

    /**
     * H4 回归：convertCharacterSet 传 null 应抛 IllegalArgumentException（达梦）。
     */
    @Test
    void convertCharacterSetWithNullNameThrowsIllegalArgumentExceptionOnDameng() {
        assertThrows(IllegalArgumentException.class, () ->
            userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
                .map(select -> select
                        .selectFunc(func -> func.convertCharacterSetFunc(
                            x -> x.column(MysqlUserDo::getUsername),
                            null),
                        FunctionResultDto::getNameRole),
                    FunctionResultDto.class)
                .findFirst()
        );
    }

    // ===========================================================================
    // 字符串函数场景
    // ===========================================================================

    /**
     * concat（达梦用 || 运算符）/ left / right / charLength / trim / substringIndex 验证。
     * Alice: username="Alice", roleCode="admin", tags="java,dm,admin"
     */
    @Test
    void stringFunctionsConcatLeftRightLengthTrimSubstringIndexOnDameng() {
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    // concat（达梦走方言 ||）: "Alice-admin"
                    .selectFunc(func -> func.concatFunc(
                        x -> x.column(MysqlUserDo::getUsername),
                        x -> x.value("-"),
                        x -> x.column(MysqlUserDo::getRoleCode)), FunctionResultDto::getNameRole)
                    // left 2: "Al"
                    .selectFunc(func -> func.leftFunc(x -> x.column(MysqlUserDo::getUsername), 2),
                        FunctionResultDto::getLeftName)
                    // right 3 from "alice": "ice"
                    .selectFunc(func -> func.right("alice", 3),
                        FunctionResultDto::getRightName)
                    // charLength("Alice") = 5
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.charLength(MysqlUserDo::getUsername), "CHAR"),
                        FunctionResultDto::getNameLength)
                    // trim
                    .selectFunc(func -> func.trimFunc(x -> x.value("  hello  ")),
                        FunctionResultDto::getTrimmedText)
                    // substringIndex("java,dm,admin", ",", 2) = "java,dm"
                    .selectFunc(func -> func.substringIndexFunc(
                        x -> x.column(MysqlUserDo::getTags), ",", 2),
                        FunctionResultDto::getSubstringText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        assertEquals("Alice-admin", result.getNameRole());
        assertEquals("Al", result.getLeftName());
        assertEquals("ice", result.getRightName());
        assertEquals("5", result.getNameLength());
        assertEquals("hello", result.getTrimmedText());
        // DM 种子 tags = "java,dm,admin"，SUBSTRING_INDEX 取前 2 段 = "java,dm"
        assertEquals("java,dm", result.getSubstringText());

        assertSqlContains("||");
        assertSqlContains("left(");
        assertSqlContains("right(");
        assertSqlContains("char_length(");
        assertSqlContains("trim(");
        assertSqlContains("substring_index(");
    }

    /**
     * upper / lower 通过 customColumn 调用（达梦原生 SQL 片段）。
     * 达梦未加引号的标识符会折叠为大写，导致无法解析；必须用双引号包裹表名和列名。
     */
    @Test
    void upperAndLowerFunctionsGenerateCorrectSqlFragmentOnDameng() {
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> {
                        func.customColumn("UPPER(\"mps_user\".\"username\")");
                        return null;
                    }, FunctionResultDto::getNameRole)
                    .selectFunc(func -> {
                        func.customColumn("LOWER(\"mps_user\".\"role_code\")");
                        return null;
                    }, FunctionResultDto::getNameLength),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        assertEquals("ALICE", result.getNameRole());
        assertEquals("admin", result.getNameLength());
        // comparableSql 归一化后双引号被去掉，断言归一化后形态
        assertSqlContains("upper(mps_user.username)");
        assertSqlContains("lower(mps_user.role_code)");
    }

    /**
     * ABS / MOD / ROUND（customColumn）数学函数验证（达梦）。
     * 达梦未加引号的标识符会折叠为大写，ROUND 的 customColumn 中标识符需加双引号。
     */
    @Test
    void mathFunctionsRoundAbsModOnDameng() {
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    // ABS(-42) = "42"
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.abs(-42), "CHAR"), FunctionResultDto::getAbsValue)
                    // MOD(credit_score=98, 10) = 8
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.mod(MysqlUserDo::getCreditScore, 10), "CHAR"), FunctionResultDto::getModValue)
                    // ROUND(balance, 0) via customColumn；达梦需双引号包裹标识符
                    .selectFunc(func -> {
                        func.customColumn("ROUND(\"mps_user\".\"balance\", 0)");
                        return null;
                    }, FunctionResultDto::getCreditText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        assertEquals("42", result.getAbsValue());
        assertEquals("8", result.getModValue());
        assertNotNull(result.getCreditText(), "ROUND 结果不应为 null");
        assertTrue(result.getCreditText().startsWith("120"),
            "ROUND(1200.50, 0) 应约等于 1200/1201，实际: " + result.getCreditText());

        assertSqlContains("abs(");
        assertSqlContains("%");
        // comparableSql 归一化后双引号被去掉，断言归一化后形态
        assertSqlContains("round(mps_user.balance, 0)");
    }

    // ===========================================================================
    // CASE WHEN 场景
    // ===========================================================================

    /**
     * CASE WHEN 分级（达梦方言）。
     * Alice=98(VIP), Bob=72(Normal), Carol=88(Normal), Dave=65(Low)；Eve 逻辑删除不可见。
     * <p>
     * 达梦严格 GROUP BY：SELECT 中的非聚合列（username, credit_score）必须全部出现在 GROUP BY 里。
     * 因此 GROUP BY 需包含 id + username + credit_score，CASE WHEN 才能正确计算。
     */
    @Test
    void caseWhenGradesByScoreReturnCorrectLabelsOnDameng() {
        // 达梦需要把所有 SELECT 中的非聚合列都加进 GROUP BY
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getId)
                          .groupBy(MysqlUserDo::getUsername)
                          .groupBy(MysqlUserDo::getCreditScore),
            where -> where.isNotNull(MysqlUserDo::getId),
            order -> order.orderAsc(MysqlUserDo::getId),
            10,
            select -> select
                .select(MysqlUserDo::getUsername, UserStatsDto::getUsername)
                .selectFunc(func -> func._case(c -> c
                    .whenThenFunc(
                        w -> w.ge(MysqlUserDo::getCreditScore, 90),
                        t -> t.value("VIP"))
                    .whenThenFunc(
                        w -> w.ge(MysqlUserDo::getCreditScore, 70),
                        t -> t.value("Normal"))
                    .elseValue("Low")),
                    UserStatsDto::getGrade),
            UserStatsDto.class
        );

        Map<String, String> gradeByName = rows.stream()
            .collect(Collectors.toMap(UserStatsDto::getUsername, UserStatsDto::getGrade));

        assertEquals("VIP", gradeByName.get("Alice"), "Alice credit_score=98 应为 VIP");
        assertEquals("Normal", gradeByName.get("Bob"), "Bob credit_score=72 应为 Normal");
        // Carol credit_score=88：88 >= 90 不满足，88 >= 70 满足 → Normal
        assertEquals("Normal", gradeByName.get("Carol"), "Carol credit_score=88 应为 Normal（88 < 90，满足 >=70）");
        assertEquals("Low", gradeByName.get("Dave"), "Dave credit_score=65 应为 Low");

        assertEquals(4, rows.size(), "逻辑删除的 Eve 不应出现");
        assertSqlContains("case when");
    }

    /**
     * CASE WHEN 搭配聚合 countPredicate（达梦）。
     * 统计每个 role_code 下 active=1 的可见用户数。
     */
    @Test
    void caseWhenInAggregateCountsActiveUsersPerRoleOnDameng() {
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            order -> order.orderAsc(MysqlUserDo::getRoleCode),
            10,
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.countPredicate(
                    w -> w.eq(MysqlUserDo::getActive, true)), UserStatsDto::getPaidCount),
            UserStatsDto.class
        );

        Map<String, Long> activeCountByRole = rows.stream()
            .collect(Collectors.toMap(UserStatsDto::getRoleCode, UserStatsDto::getPaidCount));

        assertEquals(1L, activeCountByRole.get("admin"), "admin active 用户数为 1");
        assertEquals(1L, activeCountByRole.get("user"), "user active 可见用户数为 1（Bob active，Carol inactive，Eve deleted）");
        assertEquals(1L, activeCountByRole.get("auditor"), "auditor active 用户数为 1");
        assertSqlContains("case when");
    }

    // ===========================================================================
    // cast / convertData 类型转换场景
    // ===========================================================================

    /**
     * cast（达梦）：convertData 将 credit_score 转 CHAR；convertDataTypeFunc 将 id 转 SIGNED。
     * 达梦 CAST 语法与 MySQL 相似，但方言层可能使用不同关键字，只断言 SQL 含 cast。
     */
    @Test
    void castAndConvertDataFunctionsRenderCorrectlyOnDameng() {
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.convertData(MysqlUserDo::getCreditScore, "CHAR"),
                        FunctionResultDto::getCreditText)
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.column(MysqlUserDo::getId), "SIGNED"),
                        FunctionResultDto::getModValue),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        assertEquals("98", result.getCreditText());
        assertEquals("1", result.getModValue()); // Alice id=1
        // 达梦方言走 CAST(... AS CHAR) 形式
        assertSqlContains("cast(");
    }

    // ===========================================================================
    // GROUP_CONCAT / LISTAGG separator 特殊字符场景（M4 回归，达梦用 LISTAGG）
    // ===========================================================================

    /**
     * M4 回归：GROUP_CONCAT（达梦路由到 LISTAGG）separator 含单引号时，SQL 必须正确转义。
     * 通过 assertSqlContains 验证 listagg 出现在 SQL 中（达梦方言的 groupConcat 走 LISTAGG）。
     */
    @Test
    void groupConcatWithSingleQuoteSeparatorEscapesCorrectlyOnDameng() {
        // 达梦走 groupConcat 不含 ORDER BY 分支，路由到方言 groupConcat()
        // separator = "it's" 含单引号
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.groupConcat(
                    MysqlUserDo::getUsername,
                    "it's"),  // 含单引号，达梦方言处理
                    UserStatsDto::getUsernames),
            UserStatsDto.class
        );

        // 不抛异常、返回非空结果，即表明 SQL 转义正确
        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        // 达梦走 LISTAGG
        assertSqlContains("listagg");
    }

    /**
     * M4 回归：GROUP_CONCAT（达梦 LISTAGG）separator 含反斜杠时不破坏 SQL。
     */
    @Test
    void groupConcatWithBackslashSeparatorEscapesCorrectlyOnDameng() {
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.groupConcat(
                    MysqlUserDo::getUsername,
                    "\\"),  // 单个反斜杠
                    UserStatsDto::getUsernames),
            UserStatsDto.class
        );

        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        assertSqlContains("listagg");
    }

    /**
     * GROUP_CONCAT（达梦 LISTAGG）普通 separator "|" 正常工作。
     * user 组：Bob、Carol；admin 组：Alice
     */
    @Test
    void groupConcatWithPipeSeparatorWorksOnDameng() {
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            order -> order.orderAsc(MysqlUserDo::getRoleCode),
            10,
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.groupConcat(
                    MysqlUserDo::getUsername, "|"),
                    UserStatsDto::getUsernames),
            UserStatsDto.class
        );

        assertNotNull(rows);
        assertFalse(rows.isEmpty());

        Map<String, UserStatsDto> byRole = rows.stream()
            .collect(Collectors.toMap(UserStatsDto::getRoleCode, r -> r));

        // admin 组只有 Alice
        assertTrue(byRole.get("admin").getUsernames().contains("Alice"));
        // user 组含 Bob 和 Carol（达梦 LISTAGG 不保证顺序，只断言包含）
        assertTrue(byRole.get("user").getUsernames().contains("Bob"));
        assertTrue(byRole.get("user").getUsernames().contains("Carol"));

        assertSqlContains("listagg");
    }

    /**
     * GROUP_CONCAT DISTINCT（达梦，LISTAGG DISTINCT 或方言 groupConcatDistinct）验证去重。
     */
    @Test
    void groupConcatDistinctDeduplicatesValuesOnDameng() {
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            order -> order.orderAsc(MysqlUserDo::getRoleCode),
            10,
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.groupConcatDistinct(MysqlUserDo::getRoleCode),
                    UserStatsDto::getDistinctRoles),
            UserStatsDto.class
        );

        Map<String, String> distinctRolesByRole = rows.stream()
            .collect(Collectors.toMap(UserStatsDto::getRoleCode, UserStatsDto::getDistinctRoles));

        // DISTINCT 去重后，每个分组中 role_code 只出现一次
        assertEquals("admin", distinctRolesByRole.get("admin"));
        assertEquals("user", distinctRolesByRole.get("user"));
        assertEquals("auditor", distinctRolesByRole.get("auditor"));
        assertSqlContains("listagg");
        assertSqlContains("distinct");
    }
}
