package com.xiangzhi.accesslimit;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * 订阅消息如果程序没有收到（重启等原因），会导致过期数据没有做后续处理。放弃该方式。
 * @author itcamel
 */
public class LimitExpireSubscribe extends JedisPubSub {

    private JedisPool jedisPool;

    public LimitExpireSubscribe(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public void onMessage(String channel, String message) {
        System.out.println(String.format("receive redis published message, channel %s, message %s", channel, message));
    }
}
