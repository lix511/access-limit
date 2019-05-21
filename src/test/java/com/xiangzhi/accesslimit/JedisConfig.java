package com.xiangzhi.accesslimit;

import redis.clients.jedis.JedisPool;

public class JedisConfig {
    public static JedisPool getJedisPool() {
        // localhost:6379
        return new JedisPool();
    }
}
