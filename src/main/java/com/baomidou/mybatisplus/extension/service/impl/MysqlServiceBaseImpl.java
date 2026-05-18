package com.baomidou.mybatisplus.extension.service.impl;

import com.baomidou.mybatisplus.extension.mapper.StreamBaseMapper;

/**
 * @deprecated 4.0 起改名为 {@link StreamServiceImpl}（与项目名/产品定位"多方言流式构建器"对齐）。
 * 本类仅作为兼容空壳保留至 4.1，届时移除。
 *
 * <pre>{@code
 * // 旧
 * public class UserServiceImpl extends MysqlServiceBaseImpl<UserMapper, User> implements UserService { ... }
 * // 新
 * public class UserServiceImpl extends StreamServiceImpl<UserMapper, User> implements UserService { ... }
 * }</pre>
 */
@Deprecated(since = "4.0", forRemoval = true)
public abstract class MysqlServiceBaseImpl<M extends StreamBaseMapper<T>, T> extends StreamServiceImpl<M, T> {
}
