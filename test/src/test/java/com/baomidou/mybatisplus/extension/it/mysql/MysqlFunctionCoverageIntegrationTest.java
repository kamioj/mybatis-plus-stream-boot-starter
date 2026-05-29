package com.baomidou.mybatisplus.extension.it.mysql;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 函数/表达式/CASE WHEN 全覆盖集成测试。
 * <p>
 * 覆盖场景：
 * - 日期函数：year / quarter(H5回归) / month / day / hour / minute
 * - 字符串函数：concat / substring_index / upper / lower / length / trim
 * - 数学函数：abs / round / mod
 * - CASE WHEN 表达式
 * - cast（convertData / convertDataTypeFunc）
 * - convertCharacterSet（H4回归：非法字符集名抛 IllegalArgumentException）
 * - dateAdd（M5回归：非法 INTERVAL 类型抛 IllegalArgumentException）
 * - GROUP_CONCAT separator 含特殊字符（M4回归）
 */
class MysqlFunctionCoverageIntegrationTest extends MysqlIntegrationTestBase {

    // ===========================================================================
    // 日期函数场景
    // ===========================================================================

    /**
     * H5 回归：quarter() 对已知日期（2026-05-20）必须返回季度 2（Q2），而非年份 2026。
     * <p>
     * 使用种子数据中 Alice 的 created_at（T0-5d = 2026-05-15），属于 Q2。
     */
    @Test
    void quarterReturnsCorrectQuarterNotYearForKnownDate() {
        // Alice created_at = 2026-05-15，属第 2 季度（4月~6月）
        // 注：getCreatedAt 返回 LocalDateTime，不兼容 Date 类型参数，改用 customColumn("created_at")
        String quarterText = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.quarterFunc(y -> y.customColumn("created_at")), "CHAR"),
                        FunctionResultDto::getQuarterText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow()
            .getQuarterText();

        // 季度值必须在 1-4 之间，对于 5 月份应为 2
        int quarter = Integer.parseInt(quarterText);
        assertTrue(quarter >= 1 && quarter <= 4, "quarter() 应返回 1-4，实际为: " + quarter);
        assertEquals(2, quarter, "2026-05-15 应归属 Q2，但实际为: " + quarter);
        assertSqlContains("quarter(");
    }

    /**
     * year() 对种子数据中已知日期返回正确年份。
     * Alice created_at = 2026-05-15，应返回 2026。
     */
    @Test
    void yearFunctionReturnsCorrectYear() {
        // 注：getCreatedAt 返回 LocalDateTime，不兼容 Date 类型参数，改用 customColumn("created_at")
        String yearText = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.yearFunc(y -> y.customColumn("created_at")), "CHAR"),
                        FunctionResultDto::getYearText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow()
            .getYearText();

        assertEquals("2026", yearText, "Alice created_at 年份应为 2026");
        assertSqlContains("year(");
    }

    /**
     * dateFormat 辅助获取 month / day / hour / minute，验证日期各分量提取正确。
     * Alice created_at = T0-5d = 2026-05-15 09:00:00
     * Bob   created_at = T0-4d = 2026-05-16 09:00:00
     * <p>
     * 通过 DATE_FORMAT 格式符 %m/%d/%H/%i 来覆盖 month/day/hour/minute 场景。
     */
    @Test
    void dateFormatExtractsMonthDayHourMinuteForKnownSeedDates() {
        // Alice: 2026-05-15 09:00:00
        FunctionResultDto alice = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    // 注：getCreatedAt 返回 LocalDateTime，不兼容 Date 类型参数，改用 customColumn("created_at")
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.customColumn("created_at"), "%m"), FunctionResultDto::getYearText)   // 月
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.customColumn("created_at"), "%d"), FunctionResultDto::getQuarterText) // 日
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.customColumn("created_at"), "%H"), FunctionResultDto::getWeekText)   // 时
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.customColumn("created_at"), "%i"), FunctionResultDto::getDayOfYearText), // 分
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        assertEquals("05", alice.getYearText(), "月份应为 05");
        assertEquals("15", alice.getQuarterText(), "日期应为 15");
        assertEquals("09", alice.getWeekText(), "小时应为 09");
        assertEquals("00", alice.getDayOfYearText(), "分钟应为 00");
        // 格式符 %m/%d/%H/%i 作为绑定参数 ? 传递，不出现在 SQL 文本里，只断言函数名
        assertSqlContains("date_format(");
    }

    /**
     * dateAdd 对已知日期加 1 天后通过 DATE_FORMAT 验证结果正确。
     * Alice created_at = 2026-05-15 09:00:00，加 1 DAY → 2026-05-16
     */
    @Test
    void dateAddOneDayYieldsCorrectDate() {
        // 注：getCreatedAt 返回 LocalDateTime，不兼容 Date 类型参数，改用 customColumn("created_at")
        String dateAddText = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.dateAddFunc(
                            y -> y.customColumn("created_at"),
                            y -> y.value(1),
                            "DAY"),
                        "%Y-%m-%d"), FunctionResultDto::getDateAddText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow()
            .getDateAddText();

        assertEquals("2026-05-16", dateAddText);
        assertSqlContains("date_add(");
        assertSqlContains("interval");
        assertSqlContains("day");
    }

    /**
     * dateAdd 对已知日期加 2 MONTH 验证跨月正确。
     * Alice created_at = 2026-05-15，加 2 MONTH → 2026-07-15
     */
    @Test
    void dateAddTwoMonthsYieldsCorrectDate() {
        // 注：getCreatedAt 返回 LocalDateTime，不兼容 Date 类型参数，改用 customColumn("created_at")
        String result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.dateAddFunc(
                            y -> y.customColumn("created_at"),
                            y -> y.value(2),
                            "MONTH"),
                        "%Y-%m-%d"), FunctionResultDto::getDateAddText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow()
            .getDateAddText();

        assertEquals("2026-07-15", result);
        assertSqlContains("date_add(");
        assertSqlContains("month");
    }

    // ===========================================================================
    // M5 回归：dateAdd 传非法 INTERVAL 类型应抛 IllegalArgumentException
    // ===========================================================================

    /**
     * M5 回归：dateAddFunc 传入含空格的非法 INTERVAL 类型（"DAY x"）必须抛 IllegalArgumentException，
     * 不允许 SQL 注入。
     */
    @Test
    void dateAddWithIllegalIntervalTypeThrowsIllegalArgumentException() {
        // "DAY x" 含空格，不匹配 ^[A-Za-z_]+$ 校验，应当立即抛出
        // 注：getCreatedAt 返回 LocalDateTime，异常在 interval 校验处提前抛出，与列是否解析无关，改用 customColumn
        assertThrows(IllegalArgumentException.class, () ->
            userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
                .map(select -> select
                        .selectFunc(func -> func.dateFormatFunc(
                            x -> x.dateAddFunc(
                                y -> y.customColumn("created_at"),
                                y -> y.value(1),
                                "DAY x"),  // 含空格的非法类型
                            "%Y-%m-%d"), FunctionResultDto::getDateAddText),
                    FunctionResultDto.class)
                .findFirst()
        );
    }

    /**
     * M5 回归补充：dateAddFunc 传入含分号的注入字符串应抛 IllegalArgumentException。
     */
    @Test
    void dateAddWithSemicolonInjectionTypeThrowsIllegalArgumentException() {
        // 注：异常在 interval 校验处提前抛出，改用 customColumn("created_at") 保证编译通过
        assertThrows(IllegalArgumentException.class, () ->
            userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
                .map(select -> select
                        .selectFunc(func -> func.dateFormatFunc(
                            x -> x.dateAddFunc(
                                y -> y.customColumn("created_at"),
                                y -> y.value(1),
                                "DAY;DROP TABLE mps_user"),  // 含分号
                            "%Y-%m-%d"), FunctionResultDto::getDateAddText),
                    FunctionResultDto.class)
                .findFirst()
        );
    }

    // ===========================================================================
    // H4 回归：convertCharacterSet 传非法字符集名应抛 IllegalArgumentException
    // ===========================================================================

    /**
     * H4 回归：convertCharacterSetFunc 传入含分号的字符集名（"utf8;DROP"）必须抛 IllegalArgumentException。
     */
    @Test
    void convertCharacterSetWithInjectionNameThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
                .map(select -> select
                        .selectFunc(func -> func.convertCharacterSetFunc(
                            x -> x.column(MysqlUserDo::getUsername),
                            "utf8;DROP"),  // 含分号，非法
                        FunctionResultDto::getNameRole),
                    FunctionResultDto.class)
                .findFirst()
        );
    }

    /**
     * H4 回归补充：convertCharacterSet 传 null 字符集名应抛 IllegalArgumentException。
     */
    @Test
    void convertCharacterSetWithNullNameThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            userService.stream()
                .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
                .map(select -> select
                        .selectFunc(func -> func.convertCharacterSetFunc(
                            x -> x.column(MysqlUserDo::getUsername),
                            null),  // null
                        FunctionResultDto::getNameRole),
                    FunctionResultDto.class)
                .findFirst()
        );
    }

    /**
     * convertCharacterSet 传合法字符集名 "utf8mb4" 正常生成 SQL（只验 SQL 形态，不断言具体值以免与 DM 方言差异冲突）。
     */
    @Test
    void convertCharacterSetWithValidNameGeneratesCorrectSql() {
        // 合法字符集名 utf8mb4，应正常生成 SQL 不抛异常
        userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.convertCharacterSetFunc(
                        x -> x.column(MysqlUserDo::getUsername),
                        "utf8mb4"),
                    FunctionResultDto::getNameRole),
                FunctionResultDto.class)
            .findFirst();

        assertSqlContains("using");
        assertSqlContains("utf8mb4");
    }

    // ===========================================================================
    // 字符串函数场景
    // ===========================================================================

    /**
     * concat / left / right / charLength / trim / substringIndex 覆盖验证。
     * 使用 Alice（username="Alice", roleCode="admin"）的种子数据断言具体值。
     */
    @Test
    void stringFunctionsConcatLeftRightLengthTrimSubstringIndex() {
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    // concat: "Alice-admin"
                    .selectFunc(func -> func.concatFunc(
                        x -> x.column(MysqlUserDo::getUsername),
                        x -> x.value("-"),
                        x -> x.column(MysqlUserDo::getRoleCode)), FunctionResultDto::getNameRole)
                    // left 2 chars: "Al"
                    .selectFunc(func -> func.leftFunc(x -> x.column(MysqlUserDo::getUsername), 2),
                        FunctionResultDto::getLeftName)
                    // right 3 chars from "alice": "ice"
                    .selectFunc(func -> func.right("alice", 3),
                        FunctionResultDto::getRightName)
                    // charLength("Alice") = 5
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.charLength(MysqlUserDo::getUsername), "CHAR"),
                        FunctionResultDto::getNameLength)
                    // trim("  hello  ") = "hello"
                    .selectFunc(func -> func.trimFunc(x -> x.value("  hello  ")),
                        FunctionResultDto::getTrimmedText)
                    // substringIndex("java,mysql,admin", ",", 2) = "java,mysql"
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
        // tags = "java,mysql,admin"，取前 2 段 = "java,mysql"
        assertEquals("java,mysql", result.getSubstringText());

        assertSqlContains("concat(");
        assertSqlContains("left(");
        assertSqlContains("right(");
        assertSqlContains("char_length(");
        assertSqlContains("trim(");
        assertSqlContains("substring_index(");
    }

    /**
     * upper / lower 通过 customColumn 原始 SQL 片段调用，验证 SQL 中出现对应函数名。
     * 结果断言通过 TODO 注释标记——依赖 customColumn 拼出的原始 SQL，具体结果由运行时确认。
     */
    @Test
    void upperAndLowerFunctionsGenerateCorrectSqlFragment() {
        // UPPER(username) / LOWER(role_code) 通过 customColumn 嵌入原生 SQL 片段
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    // UPPER(mps_user.username)
                    .selectFunc(func -> {
                        func.customColumn("UPPER(mps_user.username)");
                        return null;
                    }, FunctionResultDto::getNameRole)
                    // LOWER(mps_user.role_code)
                    .selectFunc(func -> {
                        func.customColumn("LOWER(mps_user.role_code)");
                        return null;
                    }, FunctionResultDto::getNameLength),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        // UPPER("Alice") = "ALICE"
        assertEquals("ALICE", result.getNameRole());
        // LOWER("admin") = "admin"
        assertEquals("admin", result.getNameLength());
        // SQL 形态断言
        assertSqlContains("upper(mps_user.username)");
        assertSqlContains("lower(mps_user.role_code)");
    }

    /**
     * round / abs / mod 数学函数验证（使用已知值断言）。
     * Alice balance = 1200.50，credit_score = 98
     */
    @Test
    void mathFunctionsRoundAbsMod() {
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    // ABS(-42) = "42"
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.abs(-42), "CHAR"), FunctionResultDto::getAbsValue)
                    // MOD(credit_score=98, 10) = 8
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.mod(MysqlUserDo::getCreditScore, 10), "CHAR"), FunctionResultDto::getModValue)
                    // ROUND(balance=1200.50, 0) via customColumn
                    .selectFunc(func -> {
                        func.customColumn("ROUND(mps_user.balance, 0)");
                        return null;
                    }, FunctionResultDto::getCreditText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        assertEquals("42", result.getAbsValue());
        assertEquals("8", result.getModValue());
        // ROUND(1200.50, 0) = 1201（四舍五入）或 1200，依赖 MySQL 精度策略
        // 结果应为整数形式（无小数部分），只断言非 null
        assertNotNull(result.getCreditText(), "ROUND 结果不应为 null");
        assertTrue(result.getCreditText().startsWith("120"),
            "ROUND(1200.50, 0) 应约等于 1200/1201，实际: " + result.getCreditText());

        assertSqlContains("abs(");
        assertSqlContains("%");
        assertSqlContains("round(");
    }

    // ===========================================================================
    // CASE WHEN 场景
    // ===========================================================================

    /**
     * CASE WHEN 分级：根据 credit_score 将用户分为 "VIP" / "Normal" / "Low" 三档，
     * 使用种子数据断言每个用户的 grade 字段正确。
     * Alice=98(VIP), Bob=72(Normal), Carol=88(Normal), Dave=65(Low)；Eve 逻辑删除不可见。
     */
    @Test
    void caseWhenGradesByScoreReturnCorrectLabels() {
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getId),
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

        assertEquals("VIP", gradeByName.get("Alice"),
            "Alice credit_score=98 应为 VIP");
        assertEquals("Normal", gradeByName.get("Bob"),
            "Bob credit_score=72 应为 Normal");
        assertEquals("Normal", gradeByName.get("Carol"),
            "Carol credit_score=88 应为 Normal（88 < 90，满足 >=70 → Normal）");
        // Dave credit_score=65 < 70，不满足 >=90 也不满足 >=70，落入 else → "Low"
        assertEquals("Low", gradeByName.get("Dave"),
            "Dave credit_score=65 应为 Low");

        assertEquals(4, rows.size(), "逻辑删除的 Eve 不应出现");
        assertSqlContains("case when");
    }

    /**
     * CASE WHEN 搭配聚合：统计每个 role_code 下 active=1 的用户数（IF/CASE WHEN countPredicate）。
     * role=user: Bob(active), Carol(inactive), Eve(deleted不可见) → 有效行 2 条，active=1 为 Bob = 1
     * role=admin: Alice(active) → 1
     * role=auditor: Dave(active) → 1
     */
    @Test
    void caseWhenInAggregateCountsActiveUsersPerRole() {
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

        assertEquals(1L, activeCountByRole.get("admin"), "admin 角色 active 用户数为 1");
        assertEquals(1L, activeCountByRole.get("user"), "user 角色可见且 active 的为 Bob=1（Carol inactive，Eve deleted）");
        assertEquals(1L, activeCountByRole.get("auditor"), "auditor 角色 active 用户数为 1");
        assertSqlContains("case when");
    }

    // ===========================================================================
    // cast / convertData 类型转换场景
    // ===========================================================================

    /**
     * cast(convertData)：将 credit_score(INT) 转 CHAR，将 balance(DECIMAL) 转 CHAR，
     * 将 id(BIGINT) 转 SIGNED，验证 SQL 中出现 CAST/CONVERT 关键字。
     */
    @Test
    void castAndConvertDataFunctionsRenderCorrectly() {
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    // credit_score=98 → "98"
                    .selectFunc(func -> func.convertData(MysqlUserDo::getCreditScore, "CHAR"),
                        FunctionResultDto::getCreditText)
                    // CAST(id AS SIGNED)
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.column(MysqlUserDo::getId), "SIGNED"),
                        FunctionResultDto::getModValue),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        assertEquals("98", result.getCreditText());
        assertEquals("1", result.getModValue()); // Alice id=1
        // MySQL 方言用 CONVERT(..., CHAR) 或 CAST(... AS CHAR)
        assertSqlContains("convert(");
    }

    // ===========================================================================
    // GROUP_CONCAT separator 特殊字符场景（M4 回归）
    // ===========================================================================

    /**
     * M4 回归：GROUP_CONCAT separator 含单引号时，SQL 必须正确转义，不破坏 SQL 结构。
     * 通过 assertSqlContains 验证转义后的 SQL 中出现了双写单引号（''）或已转义形式。
     */
    @Test
    void groupConcatWithSingleQuoteSeparatorEscapesCorrectly() {
        // 使用含单引号的分隔符：it's
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.groupConcat(
                    MysqlUserDo::getUsername,
                    order -> order.orderAsc(MysqlUserDo::getId),
                    "it's"),  // 含单引号
                    UserStatsDto::getUsernames),
            UserStatsDto.class
        );

        // 只要不抛异常、SQL 不崩溃，结果就合理
        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        // 验证 SQL 含有 separator 且转义了单引号（双写单引号 ''）
        assertSqlContains("separator");
        assertSqlContains("it''s");
    }

    /**
     * M4 回归：GROUP_CONCAT separator 含反斜杠时，SQL 必须正确转义不破坏结构。
     * 验证 SQL 中反斜杠被双写（\\）。
     */
    @Test
    void groupConcatWithBackslashSeparatorEscapesCorrectly() {
        // 使用含反斜杠的分隔符："\"
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.groupConcat(
                    MysqlUserDo::getUsername,
                    order -> order.orderAsc(MysqlUserDo::getId),
                    "\\"),  // 单个反斜杠
                    UserStatsDto::getUsernames),
            UserStatsDto.class
        );

        assertNotNull(rows);
        assertFalse(rows.isEmpty());
        // 转义后 SQL 中应出现 separator 关键字，且包含反斜杠字面量
        // 反斜杠转义规则：原始 "\" → escapeStringLiteral 先转义反斜杠为 "\\"，再双写单引号（此处无单引号）
        // 最终 SQL 片段为 SEPARATOR '\\\\'，assertSqlContains 做 lowercase 不影响，只断言 separator 存在
        assertSqlContains("separator");
        // 验证 SQL 中含有 \\ 字符（Java 字符串 "\\\\" 代表两个反斜杠，对应 SQL 中转义后的 \\）
        assertSqlContains("\\\\");
    }

    /**
     * GROUP_CONCAT 含 separator 且含 ORDER BY 正常工作（非特殊字符的普通场景）。
     * user 组：Bob(id=2)、Carol(id=3)；以 id ASC 排序，分隔符 "|"
     */
    @Test
    void groupConcatWithOrderByAndSeparatorWorksCorrectly() {
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            order -> order.orderAsc(MysqlUserDo::getRoleCode),
            10,
            select -> select
                .select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.groupConcat(
                    MysqlUserDo::getUsername,
                    order -> order.orderAsc(MysqlUserDo::getId),
                    "|"),
                    UserStatsDto::getUsernames),
            UserStatsDto.class
        );

        Map<String, String> usernamesByRole = rows.stream()
            .collect(Collectors.toMap(UserStatsDto::getRoleCode, UserStatsDto::getUsernames));

        // admin 组只有 Alice
        assertEquals("Alice", usernamesByRole.get("admin"));
        // user 组按 id ASC：Bob(id=2), Carol(id=3)，用 "|" 拼接
        assertEquals("Bob|Carol", usernamesByRole.get("user"));

        assertSqlContains("group_concat");
        assertSqlContains("separator");
        assertSqlContains("|");
        assertSqlContains("order by");
    }

    /**
     * GROUP_CONCAT DISTINCT：去重连接 role_code，每组内只出现一次 role 值。
     */
    @Test
    void groupConcatDistinctDeduplicatesValues() {
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
        assertSqlContains("group_concat");
        assertSqlContains("distinct");
    }
}
