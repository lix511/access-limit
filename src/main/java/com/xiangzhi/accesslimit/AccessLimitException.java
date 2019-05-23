package com.xiangzhi.accesslimit;

/**
 * 请求超过频次限制异常
 * @author itcamel
 */
public class AccessLimitException extends Exception {
    private AccessLimitStats stats;

    public AccessLimitException(String message, AccessLimitStats stats) {
        super(message);
        this.stats = stats;
    }

    public AccessLimitStats getStats() {
        return stats;
    }
}
