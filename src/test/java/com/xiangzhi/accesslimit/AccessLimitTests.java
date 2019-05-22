package com.xiangzhi.accesslimit;

import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

public class AccessLimitTests {

    JedisPool jedisPool = JedisConfig.getJedisPool();
    String userIdentify = "001";
    AccessLimiter accessLimiter = new AccessLimiter(jedisPool, "lawsView");

    @Test
    public void testLimit() {
        accessLimiter.setWarningSeconds(60);
        for (int i = 0; i < 11; i++) {
            try {
                accessLimiter.inc(userIdentify);
            } catch (AccessLimitException e) {
                e.printStackTrace();
            } catch (LockTimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testClearLimit() {
        accessLimiter.clearLimit(userIdentify);
    }

    @Test
    public void getStats() {
        AccessLimitStats stats = accessLimiter.getAccessLimitStats(userIdentify);
        Assert.assertNotNull(stats);
    }
}
