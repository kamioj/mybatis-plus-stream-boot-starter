package com.baomidou.mybatisplus.extension;

/**
 * description: 数据库函数中Type类型, 如CONVERT、CAST
 * <p>
 * Date: 2022/5/11 11:25
 * <p>
 * Author: 小明同学
 */
public enum MysqlDataType {

    //  将值转换为DATE。格式:"YYYY-MM-DD"
    DATE,
    // 将值转换为DATETIME。格式:"YYYY-MM-DD HH:MM:SS"
    DATETIME,
    // 将值转换为TIME。格式:"HH:MM:SS"
    TIME,
    // 将值转换为CHAR（固定长度的字符串）
    CHAR,
    // 将值转换为SIGNED（带符号的64位整数）
    SIGNED,
    // 将值转换为UNSIGNED（无符号的64位整数）
    UNSIGNED,
    // 将值转换为BINARY（二进制字符串）
    BINARY

}
