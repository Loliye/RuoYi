package com.mikufans.common.util;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;

import java.util.regex.Pattern;

public class BeanUtils extends org.springframework.beans.BeanUtils
{
    /**
     * Bean方法名中属性名开始的下标
     */
    private static final int BEAN_METHOD_PROP_INDEX = 3;

    /**
     * 匹配getter方法的正则表达式
     */
    private static final Pattern GET_PATTERN = Pattern.compile("get(\\p{javaUpperCase}\\w*)");

    /**
     * 匹配setter方法的正则表达式
     */
    private static final Pattern SET_PATTERN = Pattern.compile("set(\\p{javaUpperCase}\\w*)");


    /**
     * bean 属性复制工具方法
     * @param dest 目标对象
     * @param src 源对象
     */
    public static void copyBeanProp(Object dest, Object src)
    {
        copyProperties(src,dest);
    }
}
