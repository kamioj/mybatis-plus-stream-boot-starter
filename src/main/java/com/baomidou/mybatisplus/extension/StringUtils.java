package com.baomidou.mybatisplus.extension;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 小明同学
 * @date 2024年04月25日 15:58
 */
public class StringUtils extends StrUtil {

    /**
     * 正则匹配
     *
     * @param regex  正则表达式
     * @param source 字符串
     * @return 匹配字符串
     */
    public static String regexMatcher(String regex, String source) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
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
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source);
        List<String> match = new ArrayList<>();
        while (matcher.find()) {
            match.add(matcher.group(1));
        }
        return match.toArray(new String[0]);
    }

}
