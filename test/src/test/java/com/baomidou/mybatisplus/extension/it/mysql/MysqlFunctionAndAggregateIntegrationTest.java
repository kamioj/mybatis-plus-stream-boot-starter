package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MysqlFunctionAndAggregateIntegrationTest extends MysqlIntegrationTestBase {

    @Test
    void groupedMapAggregatesArePushedToMysqlSql() {
        Map<String, BigDecimal> sumByRole = userService.stream()
            .toMapSum(MysqlUserDo::getRoleCode, MysqlUserDo::getBalance);
        Map<String, Double> avgScoreByRole = userService.stream()
            .toMapAvg(MysqlUserDo::getRoleCode, MysqlUserDo::getCreditScore);
        Map<String, Integer> maxScoreByRole = userService.stream()
            .toMapMax(MysqlUserDo::getRoleCode, MysqlUserDo::getCreditScore);
        Map<String, Integer> minScoreByRole = userService.stream()
            .toMapMin(MysqlUserDo::getRoleCode, MysqlUserDo::getCreditScore);

        assertEquals(0, new BigDecimal("1080.25").compareTo(sumByRole.get("user")));
        assertEquals(80.0, avgScoreByRole.get("user"));
        assertEquals(88, maxScoreByRole.get("user"));
        assertEquals(72, minScoreByRole.get("user"));
        assertEquals(0, new BigDecimal("1200.50").compareTo(sumByRole.get("admin")));
        assertSqlContains("sum(balance)");
        assertSqlContains("avg(credit_score)");
        assertSqlContains("max(credit_score)");
        assertSqlContains("min(credit_score)");
        assertSqlContains("group by role_code");
    }

    @Test
    void listGroupValuesAndListJoinValuesCoverSingleValueAggregateDocs() {
        List<BigDecimal> roleBalanceTotals = userService.listGroupValues(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            order -> order.orderAsc(MysqlUserDo::getRoleCode),
            10,
            func -> func.sum(MysqlUserDo::getBalance)
        );
        assertEquals(List.of(new BigDecimal("1200.50"), new BigDecimal("90.00"), new BigDecimal("1080.25")),
            roleBalanceTotals);

        List<BigDecimal> paidOrderTotal = userService.listJoinValues(
            join -> join.innerJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            where -> where.eq(MysqlOrderDo::getStatus, "paid"),
            order -> order.orderAsc(MysqlUserDo::getId),
            1,
            func -> func.sum(MysqlOrderDo::getAmount)
        );
        assertEquals(1, paidOrderTotal.size());
        assertEquals(0, new BigDecimal("380.00").compareTo(paidOrderTotal.get(0)));
        assertSqlContains("inner join mps_order");
        assertSqlContains("sum(mps_user.balance)");
        assertSqlContains("sum(mps_order.amount)");
    }

    @Test
    void pageGroupJoinPaginatesGroupedJoinResultsWithCountSql() {
        IPage<UserStatsDto> page = userService.pageGroupJoin(
            new Page<>(1, 2),
            join -> join.leftJoin(MysqlOrderDo.class, MysqlUserDo::getId, MysqlOrderDo::getUserId),
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.eq(MysqlUserDo::getActive, true),
            select -> select.select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.count(MysqlOrderDo::getId), UserStatsDto::getOrderCount)
                .selectFunc(func -> func.sum(MysqlOrderDo::getAmount), UserStatsDto::getTotalAmount),
            UserStatsDto.class
        );

        assertEquals(3, page.getTotal());
        assertEquals(2, page.getRecords().size());
        assertTrue(page.getRecords().stream().allMatch(row -> row.getRoleCode() != null));
        assertSqlContains("left join mps_order");
        assertSqlContains("group by mps_user.role_code");
        assertSqlContains("select count(*) as c from (select");
        assertSqlContains("limit 2");
    }

    @Test
    void groupConcatDistinctGroupFirstAndPredicateCountMapCorrectly() {
        List<UserStatsDto> rows = userService.listGroup(
            group -> group.groupBy(MysqlUserDo::getRoleCode),
            where -> where.isNotNull(MysqlUserDo::getId),
            order -> order.orderAsc(MysqlUserDo::getRoleCode),
            10,
            select -> select.select(MysqlUserDo::getRoleCode, UserStatsDto::getRoleCode)
                .selectFunc(func -> func.groupConcat(MysqlUserDo::getUsername,
                    order -> order.orderAsc(MysqlUserDo::getId), "|"), UserStatsDto::getUsernames)
                .selectFunc(func -> func.groupConcatDistinct(MysqlUserDo::getRoleCode), UserStatsDto::getDistinctRoles)
                .selectFunc(func -> func.groupFirst(MysqlUserDo::getUsername,
                    order -> order.orderAsc(MysqlUserDo::getId)), UserStatsDto::getFirstUsername)
                .selectFunc(func -> func.countPredicate(whereFunc -> whereFunc.eq(MysqlUserDo::getActive, true)),
                    UserStatsDto::getPaidCount),
            UserStatsDto.class
        );

        Map<String, UserStatsDto> byRole = rows.stream()
            .collect(Collectors.toMap(UserStatsDto::getRoleCode, row -> row));
        assertEquals("Alice", byRole.get("admin").getUsernames());
        assertEquals("Bob|Carol", byRole.get("user").getUsernames());
        assertEquals("user", byRole.get("user").getDistinctRoles());
        assertEquals("Bob", byRole.get("user").getFirstUsername());
        assertEquals(1L, byRole.get("user").getPaidCount());
        assertSqlContains("group_concat");
        assertSqlContains("distinct");
        assertSqlContains("substring_index");
        assertSqlContains("case when");
    }

    @Test
    void selectedMysqlStringMathDateAndConversionFunctionsRenderAndMap() {
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.concatFunc(
                        x -> x.column(MysqlUserDo::getUsername),
                        x -> x.value("-"),
                        x -> x.column(MysqlUserDo::getRoleCode)), FunctionResultDto::getNameRole)
                    .selectFunc(func -> func.convertDataTypeFunc(
                        x -> x.charLength(MysqlUserDo::getUsername), "CHAR"), FunctionResultDto::getNameLength)
                    .selectFunc(func -> func.leftFunc(x -> x.column(MysqlUserDo::getUsername), 2),
                        FunctionResultDto::getLeftName)
                    .selectFunc(func -> func.md5("test"), FunctionResultDto::getMd5Value)
                    .selectFunc(func -> func.ifnull(MysqlUserDo::getTags, "none"), FunctionResultDto::getTagsOrDefault)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.abs(-42), "CHAR"),
                        FunctionResultDto::getAbsValue)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.mod(MysqlUserDo::getCreditScore, 30), "CHAR"),
                        FunctionResultDto::getModValue)
                    .selectFunc(func -> func.convertData(MysqlUserDo::getCreditScore, "CHAR"),
                        FunctionResultDto::getCreditText)
                    .selectFunc(func -> func.dateFormatFunc(x -> x.now(), "%Y-%m-%d"),
                        FunctionResultDto::getTodayText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        assertEquals("Alice-admin", result.getNameRole());
        assertEquals("5", result.getNameLength());
        assertEquals("Al", result.getLeftName());
        assertEquals("098f6bcd4621d373cade4e832627b4f6", result.getMd5Value());
        assertEquals("java,mysql,admin", result.getTagsOrDefault());
        assertEquals("42", result.getAbsValue());
        assertEquals("8", result.getModValue());
        assertEquals("98", result.getCreditText());
        assertTrue(result.getTodayText().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertSqlContains("concat(");
        assertSqlContains("char_length(");
        assertSqlContains("left(");
        assertSqlContains("md5(");
        assertSqlContains("ifnull(");
        assertSqlContains("abs(");
        assertSqlContains("%");
        assertSqlContains("convert(");
        assertSqlContains("date_format(");
    }

    @Test
    void functionsMdMatrixCoversArithmeticStringDateMathConditionalAndBitwiseFunctions() {
        FunctionResultDto result = userService.stream()
            .filter(where -> where.eq(MysqlUserDo::getUsername, "Alice"))
            .map(select -> select
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.add(MysqlUserDo::getCreditScore, 10), "CHAR"),
                        FunctionResultDto::getAddValue)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.subtract(MysqlUserDo::getCreditScore, 50), "CHAR"),
                        FunctionResultDto::getSubtractValue)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.multiply(MysqlUserDo::getCreditScore, 2), "CHAR"),
                        FunctionResultDto::getMultiplyValue)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.divide(MysqlUserDo::getCreditScore, 10), "CHAR"),
                        FunctionResultDto::getDivideValue)
                    .selectFunc(func -> func.right("hello_world", 5), FunctionResultDto::getRightName)
                    .selectFunc(func -> func.trimFunc(x -> x.value("  hello  ")), FunctionResultDto::getTrimmedText)
                    .selectFunc(func -> func.substringIndexFunc(x -> x.value("a-b-c"), "-", 2),
                        FunctionResultDto::getSubstringText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.findInSetFunc(
                        y -> y.value("b"), y -> y.value("a,b,c")), "CHAR"), FunctionResultDto::getFindInSetValue)
                    .selectFunc(func -> func.hexNumber(255), FunctionResultDto::getHexNumberValue)
                    .selectFunc(func -> func.hexStr("Hello"), FunctionResultDto::getHexStringValue)
                    .selectFunc(func -> func._if(where -> where.eq(MysqlUserDo::getRoleCode, "admin"), "YES", "NO"),
                        FunctionResultDto::getIfValue)
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.strToDateFunc(y -> y.value("2026-01-15"), "%Y-%m-%d"), "%Y-%m-%d"),
                        FunctionResultDto::getParsedDateText)
                    .selectFunc(func -> func.dateFormatFunc(
                        x -> x.dateAddFunc(y -> y.now(), y -> y.value(1), "DAY"), "%Y-%m-%d"),
                        FunctionResultDto::getDateAddText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.yearFunc(y -> y.now()), "CHAR"),
                        FunctionResultDto::getYearText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.quarterFunc(y -> y.now()), "CHAR"),
                        FunctionResultDto::getQuarterText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.weekOfYearFunc(y -> y.now()), "CHAR"),
                        FunctionResultDto::getWeekText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.dayOfYearFunc(y -> y.now()), "CHAR"),
                        FunctionResultDto::getDayOfYearText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.toDaysFunc(y -> y.now()), "CHAR"),
                        FunctionResultDto::getToDaysText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.toSecondsFun(y -> y.now()), "CHAR"),
                        FunctionResultDto::getToSecondsText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.sqrtFunc(y -> y.value(144)), "CHAR"),
                        FunctionResultDto::getSqrtText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.pi(), "CHAR"), FunctionResultDto::getPiText)
                    .selectFunc(func -> func.conv(255, 10, 16), FunctionResultDto::getConvText)
                    .selectFunc(func -> func.convFunc(x -> x.value("1010"), 2, 10), FunctionResultDto::getConvFuncText)
                    .selectFunc(func -> func.elt(2, "alpha", "beta", "gamma"), FunctionResultDto::getEltText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.interval(23, 1, 5, 10, 20, 30), "CHAR"),
                        FunctionResultDto::getIntervalText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.bAndFunc(y -> y.value(12), y -> y.value(10)), "CHAR"),
                        FunctionResultDto::getBitAndText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.bOrFunc(y -> y.value(12), y -> y.value(10)), "CHAR"),
                        FunctionResultDto::getBitOrText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.bXorFunc(y -> y.value(12), y -> y.value(10)), "CHAR"),
                        FunctionResultDto::getBitXorText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.bShiftLeftFunc(y -> y.value(1), y -> y.value(4)), "CHAR"),
                        FunctionResultDto::getShiftLeftText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.bShiftRightFunc(y -> y.value(16), y -> y.value(2)), "CHAR"),
                        FunctionResultDto::getShiftRightText)
                    .selectFunc(func -> func.convertDataTypeFunc(x -> x.bNotFunc(y -> y.value(0)), "CHAR"),
                        FunctionResultDto::getBitNotText),
                FunctionResultDto.class)
            .findFirst()
            .orElseThrow();

        assertEquals("108", result.getAddValue());
        assertEquals("48", result.getSubtractValue());
        assertEquals("196", result.getMultiplyValue());
        assertTrue(result.getDivideValue().startsWith("9.8"));
        assertEquals("world", result.getRightName());
        assertEquals("hello", result.getTrimmedText());
        assertEquals("a-b", result.getSubstringText());
        assertEquals("2", result.getFindInSetValue());
        assertEquals("FF", result.getHexNumberValue());
        assertEquals("48656C6C6F", result.getHexStringValue());
        assertEquals("YES", result.getIfValue());
        assertEquals("2026-01-15", result.getParsedDateText());
        assertTrue(result.getDateAddText().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertTrue(Integer.parseInt(result.getYearText()) >= 2026);
        assertTrue(Integer.parseInt(result.getQuarterText()) >= 1);
        assertTrue(Integer.parseInt(result.getWeekText()) >= 1);
        assertTrue(Integer.parseInt(result.getDayOfYearText()) >= 1);
        assertTrue(Long.parseLong(result.getToDaysText()) > 0);
        assertTrue(Long.parseLong(result.getToSecondsText()) > 0);
        assertEquals("12", result.getSqrtText());
        assertTrue(result.getPiText().startsWith("3.14"));
        assertEquals("FF", result.getConvText());
        assertEquals("10", result.getConvFuncText());
        assertEquals("beta", result.getEltText());
        assertEquals("4", result.getIntervalText());
        assertEquals("8", result.getBitAndText());
        assertEquals("14", result.getBitOrText());
        assertEquals("6", result.getBitXorText());
        assertEquals("16", result.getShiftLeftText());
        assertEquals("4", result.getShiftRightText());
        assertNotNull(result.getBitNotText());
        assertSqlContains("+");
        assertSqlContains("-");
        assertSqlContains("*");
        assertSqlContains("/");
        assertSqlContains("right(");
        assertSqlContains("trim(");
        assertSqlContains("substring_index(");
        assertSqlContains("find_in_set(");
        assertSqlContains("hex(");
        assertSqlContains("if(");
        assertSqlContains("str_to_date(");
        assertSqlContains("date_add(");
        assertSqlContains("year(");
        assertSqlContains("quarter(");
        assertSqlContains("weekofyear(");
        assertSqlContains("dayofyear(");
        assertSqlContains("to_days(");
        assertSqlContains("to_seconds(");
        assertSqlContains("sqrt(");
        assertSqlContains("pi(");
        assertSqlContains("conv(");
        assertSqlContains("elt(");
        assertSqlContains("interval(");
        assertSqlContains("&");
        assertSqlContains("|");
        assertSqlContains("^");
        assertSqlContains("<<");
        assertSqlContains(">>");
        assertSqlContains("~");
    }
}
