package com.xiangzhi.accesslimit;

/**
 * 获取锁超时异常
 * @author itcamel
 */
public class LockTimeoutException extends Exception {
    public LockTimeoutException() {
    }

    public LockTimeoutException(String message) {
        super(message);
    }
}
