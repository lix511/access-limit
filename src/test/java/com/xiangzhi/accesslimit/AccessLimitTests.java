package com.xiangzhi.accesslimit;

import org.junit.Test;
import redis.clients.jedis.JedisPool;

public class AccessLimitTests {

    @Test
    public void testListener() {
        JedisPool jedisPool = JedisConfig.getJedisPool();

        String userIdentify = "001";
        AccessLimiter accessLimiter = new AccessLimiter(jedisPool, "lawsView");
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
}
