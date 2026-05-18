package com.baomidou.mybatisplus.extension.dialect;

/**
 * 行锁模式。对应 SELECT ... FOR UPDATE [...] 的细粒度选项。
 *
 * <p>各方言支持情况：
 * <ul>
 *   <li>MySQL 8.0+：完整支持 FOR UPDATE / NOWAIT / SKIP LOCKED；不支持 WAIT n</li>
 *   <li>PostgreSQL：完整支持 FOR UPDATE / NOWAIT / SKIP LOCKED；不支持 WAIT n</li>
 *   <li>DM 8：支持 FOR UPDATE / NOWAIT / WAIT n；SKIP LOCKED 支持取决于兼容模式</li>
 * </ul>
 *
 * <p>当方言不支持时，dialect 实现可降级（如 SKIP LOCKED 降级为 NOWAIT）或抛出
 * {@link UnsupportedOperationException}（推荐 fail-fast）。
 */
public enum LockMode {

    /** SELECT ... FOR UPDATE */
    FOR_UPDATE,

    /** SELECT ... FOR UPDATE NOWAIT —— 行被占用时立即抛错，不等待 */
    NOWAIT,

    /** SELECT ... FOR UPDATE SKIP LOCKED —— 行被占用时跳过该行 */
    SKIP_LOCKED,

    /**
     * SELECT ... FOR UPDATE WAIT n —— 等待 n 秒后超时抛错。
     * 实际等待秒数通过 dialect 的额外参数传递（本枚举仅作为模式标识）。
     */
    WAIT
}
