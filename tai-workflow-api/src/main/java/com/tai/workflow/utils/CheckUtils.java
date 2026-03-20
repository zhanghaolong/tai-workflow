package com.tai.workflow.utils;

import java.util.Collection;
import java.util.Map;

/**
 * 参数校验工具类
 *
 * @author zhanghaolong1989@163.com
 */
public final class CheckUtils {

    private CheckUtils() {
        // Utility class
    }

    /**
     * 检查对象不为null
     *
     * @param obj       待检查对象
     * @param errorCode 错误码
     * @throws IllegalArgumentException 如果对象为null
     */
    public static void checkNotNull(Object obj, String errorCode) {
        if (obj == null) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    /**
     * 检查对象不为null
     *
     * @param obj       待检查对象
     * @param errorCode 错误码
     * @param message   错误信息
     * @throws IllegalArgumentException 如果对象为null
     */
    public static void checkNotNull(Object obj, String errorCode, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(errorCode + ": " + message);
        }
    }

    /**
     * 检查字符串不为空
     *
     * @param str       待检查字符串
     * @param errorCode 错误码
     * @throws IllegalArgumentException 如果字符串为null或空
     */
    public static void checkNotEmpty(String str, String errorCode) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    /**
     * 检查字符串不为空
     *
     * @param str       待检查字符串
     * @param errorCode 错误码
     * @param message   错误信息
     * @throws IllegalArgumentException 如果字符串为null或空
     */
    public static void checkNotEmpty(String str, String errorCode, String message) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(errorCode + ": " + message);
        }
    }

    /**
     * 检查集合不为空
     *
     * @param collection 待检查集合
     * @param errorCode  错误码
     * @throws IllegalArgumentException 如果集合为null或空
     */
    public static void checkNotEmpty(Collection<?> collection, String errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    /**
     * 检查集合不为空
     *
     * @param collection 待检查集合
     * @param errorCode  错误码
     * @param message    错误信息
     * @throws IllegalArgumentException 如果集合为null或空
     */
    public static void checkNotEmpty(Collection<?> collection, String errorCode, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(errorCode + ": " + message);
        }
    }

    /**
     * 检查Map不为空
     *
     * @param map       待检查Map
     * @param errorCode 错误码
     * @throws IllegalArgumentException 如果Map为null或空
     */
    public static void checkNotEmpty(Map<?, ?> map, String errorCode) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    /**
     * 检查Map不为空
     *
     * @param map       待检查Map
     * @param errorCode 错误码
     * @param message   错误信息
     * @throws IllegalArgumentException 如果Map为null或空
     */
    public static void checkNotEmpty(Map<?, ?> map, String errorCode, String message) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(errorCode + ": " + message);
        }
    }

    /**
     * 检查条件是否为true
     *
     * @param condition 条件表达式
     * @param errorCode 错误码
     * @throws IllegalArgumentException 如果条件为false
     */
    public static void checkCondition(boolean condition, String errorCode) {
        if (!condition) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    /**
     * 检查条件是否为true
     *
     * @param condition 条件表达式
     * @param errorCode 错误码
     * @param message   错误信息
     * @throws IllegalArgumentException 如果条件为false
     */
    public static void checkCondition(boolean condition, String errorCode, String message) {
        if (!condition) {
            throw new IllegalArgumentException(errorCode + ": " + message);
        }
    }

    /**
     * 检查数值在指定范围内
     *
     * @param value     待检查数值
     * @param min       最小值(包含)
     * @param max       最大值(包含)
     * @param errorCode 错误码
     * @throws IllegalArgumentException 如果数值不在范围内
     */
    public static void checkRange(long value, long min, long max, String errorCode) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    /**
     * 检查数值为正数
     *
     * @param value     待检查数值
     * @param errorCode 错误码
     * @throws IllegalArgumentException 如果数值不为正数
     */
    public static void checkPositive(long value, String errorCode) {
        if (value <= 0) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    /**
     * 检查数值为非负数
     *
     * @param value     待检查数值
     * @param errorCode 错误码
     * @throws IllegalArgumentException 如果数值为负数
     */
    public static void checkNonNegative(long value, String errorCode) {
        if (value < 0) {
            throw new IllegalArgumentException(errorCode);
        }
    }
}
