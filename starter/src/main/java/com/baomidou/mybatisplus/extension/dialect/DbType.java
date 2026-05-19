package com.baomidou.mybatisplus.extension.dialect;

/**
 * 数据库类型枚举。
 *
 * <p>4.0 起本库内置 MySQL、PostgreSQL、达梦三种方言；其他方言用户可实现 {@link SqlDialect}
 * 接口并通过 {@link java.util.ServiceLoader} 注册（{@code CUSTOM} 占位常量也可用作 token，
 * 但实际通过 {@code dialect.dbType()} 返回值识别）。
 *
 * <p>命名借鉴 MyBatis-Plus 的 {@code com.baomidou.mybatisplus.annotation.DbType}，
 * 但本枚举仅供本扩展库内部 SPI 使用，与 MP 上游枚举无强绑定。
 */
public enum DbType {

    /** MySQL / MariaDB（默认）*/
    MYSQL,

    /** PostgreSQL（4.0.1 起内置）*/
    POSTGRESQL,

    /** 达梦 DM 8（4.0.1 起内置，建议 MySQL 兼容模式或 Oracle 兼容模式自行选）*/
    DAMENG,

    /** 用户自定义方言占位，实际 {@code dbType()} 应返回真实值 */
    CUSTOM
}
