package com.baomidou.mybatisplus.extension.service.impl;

import com.baomidou.mybatisplus.extension.mapper.StreamBaseMapper;
import com.baomidou.mybatisplus.extension.metadata.ProcedureParam;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * L-12 单元测试：{@code callProcedureForList} 的存储过程名白名单（{@code ^[A-Za-z_][A-Za-z0-9_.]*$}）。
 *
 * <p>该方法是 {@link StreamServiceImpl} 的 {@code protected} 实例方法，集成测试类跨包无法直接调用，
 * 故在 service.impl 同包下用一个 test-only 子类桩暴露它。白名单校验位于方法首行、在任何
 * {@code getBaseMapper()} / 数据库访问之前抛出，因此无需真实数据源即可验证（纯单元测试）。
 *
 * <p>仅覆盖「拒绝非法过程名」路径：合法名会继续走到 {@code getBaseMapper()}（此处为 null）从而 NPE，
 * 不属本测试关注点（白名单已放行即达目的）。
 */
class StreamServiceImplProcedureWhitelistTest {

    /** 仅为在同包内暴露 protected 的 callProcedureForList。不接真实 mapper/数据源。 */
    static class TestService extends StreamServiceImpl<StreamBaseMapper<Object>, Object> {
        <R> List<R> call(String procedureName, Class<R> renameClass, ProcedureParam... params) {
            return callProcedureForList(procedureName, renameClass, params);
        }
    }

    private final TestService service = new TestService();

    @Test
    void rejectsProcedureNameWithSemicolonInjection() {
        assertThrows(IllegalArgumentException.class,
            () -> service.call("proc;DROP TABLE mps_user", Object.class),
            "含分号的过程名应被白名单拒绝");
    }

    @Test
    void rejectsProcedureNameWithSpaceAndParen() {
        assertThrows(IllegalArgumentException.class,
            () -> service.call("proc() UNION SELECT 1", Object.class),
            "含空格/括号的过程名应被白名单拒绝");
    }

    @Test
    void rejectsNullProcedureName() {
        assertThrows(IllegalArgumentException.class,
            () -> service.call(null, Object.class),
            "null 过程名应被拒绝");
    }

    @Test
    void rejectsProcedureNameStartingWithDigit() {
        // 白名单要求首字符为字母或下划线
        assertThrows(IllegalArgumentException.class,
            () -> service.call("1proc", Object.class),
            "数字开头的过程名应被白名单拒绝");
    }

    @Test
    void rejectsProcedureNameWithBacktickInjection() {
        assertThrows(IllegalArgumentException.class,
            () -> service.call("proc`--", Object.class),
            "含反引号的过程名应被白名单拒绝");
    }
}
