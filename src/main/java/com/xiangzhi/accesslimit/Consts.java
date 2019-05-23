package com.xiangzhi.accesslimit;

import java.time.format.DateTimeFormatter;

/**
 * 常量定义
 * @author itcamel
 */
public class Consts {
    public static final String REDIS_KEY_FORBIDDEN_USER = "al:forbidden_user:";
    public static final String REDIS_KEY_WARNING_USER = "al:warning_user:";
    public static final String REDIS_KEY_WARNING_COUNTERS = "al:warning_counters:";
    public static final String REDIS_KEY_RATE_COUNTER = "al:rate:";
    public static final String REDIS_KEY_LOCK = "al:lock:";

    public static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
