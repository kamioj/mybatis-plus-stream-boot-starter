package com.baomidou.mybatisplus.extension.service;

/**
 * @deprecated 4.0 起改名为 {@link IStreamService}（与项目名/产品定位"多方言流式构建器"对齐）。
 * 本接口仅作为兼容空壳保留至 4.1，届时移除。
 *
 * <pre>{@code
 * // 旧
 * public interface UserService extends IMysqlServiceBase<User> { ... }
 * // 新
 * public interface UserService extends IStreamService<User> { ... }
 * }</pre>
 */
@Deprecated(since = "4.0", forRemoval = true)
public interface IMysqlServiceBase<T> extends IStreamService<T> {
}
