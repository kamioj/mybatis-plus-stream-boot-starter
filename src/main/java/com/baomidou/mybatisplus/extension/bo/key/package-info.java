/**
 * 多列投影时的组合键容器。
 *
 * <p>当 stream 投影多列且需要把它们作为 {@code Map} 的 key 或
 * 等价判定时，用这些组合键代替元组：
 *
 * <ul>
 *   <li>{@link com.baomidou.mybatisplus.extension.bo.key.BiMapKey} —— 2 列组合键</li>
 *   <li>{@code MapKey3 / MapKey4 / MapKey5} —— 3 / 4 / 5 列组合键</li>
 * </ul>
 *
 * <p>它们都实现 {@code Serializable} 并正确重写 {@code equals/hashCode}，
 * 可安全用作 {@code HashMap} 的 key。
 */
package com.baomidou.mybatisplus.extension.bo.key;
