package com.baomidou.mybatisplus.extension.mapper;

/**
 * @deprecated 4.0 起改名为 {@link StreamBaseMapper}（与项目名/产品定位"多方言流式构建器"对齐）。
 * 本接口仅作为兼容空壳保留至 4.1，届时移除。请尽快迁移：
 *
 * <pre>{@code
 * // 旧
 * public interface UserMapper extends MysqlBaseMapper<User> { ... }
 * // 新
 * public interface UserMapper extends StreamBaseMapper<User> { ... }
 * }</pre>
 *
 * <p>IDE 一键 Refactor → Rename 即可完成迁移。
 */
@Deprecated(since = "4.0", forRemoval = true)
public interface MysqlBaseMapper<T> extends StreamBaseMapper<T> {
}
