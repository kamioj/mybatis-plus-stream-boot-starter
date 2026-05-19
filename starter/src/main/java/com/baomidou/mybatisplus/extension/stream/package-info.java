/**
 * 流式 API 家族——把数据库查询包装成 Java Stream 风格的链式调用。
 *
 * <p><b>用户入口</b>：通过 {@code IStreamService#stream()} 或
 * {@code IStreamService#executableStream()} 获取，不直接 new。
 *
 * <p><b>类层级</b>：
 * <ul>
 *   <li>{@link com.baomidou.mybatisplus.extension.stream.MybatisStream} —— 顶层抽象，
 *       提供 join / filter / sorted / limit 等链式方法</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.stream.MybatisQueryableStream} ——
 *       可投影流抽象，附加 distinct / group / page / forUpdate</li>
 *   <li>{@code MybatisQueryableStream1..5} —— 按投影列数模板化的具体类
 *       （1 列返回 R，2 列返回 BiMapKey&lt;R1,R2&gt;，3-5 列返回 MapKey3..5）</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.stream.MybatisQueryableStreamMany}
 *       —— 列数不定时的 Object[] 投影</li>
 *   <li>{@link com.baomidou.mybatisplus.extension.stream.MybatisExecutableStream} ——
 *       执行流（UPDATE / DELETE），不做投影</li>
 * </ul>
 *
 * <p><b>arity-N 维护提醒</b>：MybatisQueryableStream1..5 是按"投影列数"复制的模板类，
 * 修改一个方法签名时通常要同步改 6 处（含 Many）。新增列数（如 6/7/8）按相同模板复制。
 * 详见 {@code dev-docs/ARITY-TEMPLATE.md}。
 */
package com.baomidou.mybatisplus.extension.stream;
