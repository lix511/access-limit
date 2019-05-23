package com.xiangzhi.accesslimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * 废弃
 * @author itcamel.
 */
public class AccessLimitRedisListener {
    private static final Logger logger = LoggerFactory.getLogger(AccessLimitRedisListener.class);

    private JedisPool jedisPool;
    private ListenerRunner listenerRunner;

    public AccessLimitRedisListener(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public synchronized void start() {
        if (listenerRunner == null) {
            listenerRunner = new ListenerRunner();
        }
        listenerRunner.start();
    }

    private class ListenerRunner extends Thread {
        private final JedisPubSub sub = new LimitExpireSubscribe(jedisPool);

        @Override
        public void run() {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                jedis.subscribe(sub, "__keyevent@0__:expired");
            } catch (Exception e) {
                logger.error("subsrcibe channel error", e);
            } finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }
    }
}
