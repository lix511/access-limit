package com.xiangzhi.accesslimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

/**
 * 使用Redis实现的分布式锁
 * @author itcamel
 */
public class RedisLocker {
    private static final Logger logger = LoggerFactory.getLogger(RedisLocker.class);

    private static final Integer LOCK_TIMEOUT = 3;
    private Jedis jedis;

    public RedisLocker(Jedis jedis) {
        this.jedis = jedis;
    }

    /**
     * 外部调用加锁的方法
     * @param lockKey 锁的名字
     * @param timeout 超时时间（放置时间长度(秒)，如：5L）
     * @return 是否成功
     */
    public boolean tryLock(String lockKey, Long timeout) {
        try {
            //开始加锁的时间
            Long currentTime = System.currentTimeMillis();
            boolean result = false;

            while (true) {
                //当前时间超过了设定的超时时间
                if ((System.currentTimeMillis() - currentTime) / 1000 > timeout) {
                    logger.info("tryLock Timeout.");
                    break;
                } else {
                    result = innerTryLock(lockKey);
                    if (result) {
                        break;
                    } else {
                        logger.info("Try to get the Lock, and wait 100 millisecond....");
                        Thread.sleep(100);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("failed to getLock.", e);
            return false;
        }
    }

    /**
     * 释放锁
     * @param lockKey 锁的名字
     */
    public void realseLock(String lockKey) {
//        if(!checkIfLockTimeout(System.currentTimeMillis(), lockKey)){
            jedis.del(lockKey);
//        }
    }

    /**
     * 内部获取锁的实现方法
     * @param lockKey 锁的名字
     * @return 是否成功
     */
    private boolean innerTryLock(String lockKey) {
        //当前时间
        long currentTime = System.currentTimeMillis();
        //锁的持续时间
        String lockTimeDuration = String.valueOf(currentTime + LOCK_TIMEOUT + 1);
        Long result = jedis.setnx(lockKey, lockTimeDuration);

        if (result == 1) {
            return true;
        } else {
            if (checkIfLockTimeout(currentTime, lockKey)) {
                String preLockTimeDuration = jedis.getSet(lockKey, lockTimeDuration);
                if (currentTime > Long.valueOf(preLockTimeDuration)) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * 判断加锁是否超时
     * @param currentTime 当前时间
     * @param lockKey 锁的名字
     * @return 是否超时
     */
    private boolean checkIfLockTimeout(Long currentTime, String lockKey) {
        //当前时间超过锁的持续时间
        if (currentTime > Long.valueOf(jedis.get(lockKey))) {
            return true;
        } else {
            return false;
        }
    }
}
