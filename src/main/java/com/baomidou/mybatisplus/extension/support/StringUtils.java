package com.baomidou.mybatisplus.extension.support;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 小明同学
 * @date 2024年04月25日 15:58
 */
public class StringUtils extends StrUtil {

    /**
     * Pattern 缓存：4.0 起按 regex 字符串缓存编译好的 Pattern，避免热路径每次 {@code Pattern.compile()}。
     * SQL 拼装中相同的正则表达式（如类名解析、字段名提取）通常只有几个固定值，缓存命中率极高。
     */
    private static final ConcurrentMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    private static Pattern compile(String regex) {
        return PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
    }

    /**
     * 正则匹配
     *
     * @param regex  正则表达式
     * @param source 字符串
     * @return 匹配字符串
     */
    public static String regexMatcher(String regex, String source) {
        Matcher matcher = compile(regex).matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 正则匹配
     *
     * @param regex  正则表达式
     * @param source 字符串
     * @return 匹配的字符串集合
     */
    public static String[] regexMatcherAll(String regex, String source) {
        Matcher matcher = compile(regex).matcher(source);
        List<String> match = new ArrayList<>();
        while (matcher.find()) {
            match.add(matcher.group(1));
        }
        return match.toArray(new String[0]);
    }

}
