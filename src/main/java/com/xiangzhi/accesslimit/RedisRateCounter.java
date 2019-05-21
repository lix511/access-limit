package com.xiangzhi.accesslimit;

import org.apache.commons.lang.RandomStringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 频次计算
 */
public class RedisRateCounter {

    private Jedis jedis;

    /**
     * 供EVAL函数使用，使用内置的 Lua 解释器，可以对 Lua 脚本进行求值。
     */
    private static final String LuaSecondsScript = " local current; "
            + " current = redis.call('incr',KEYS[1]); "
            + " if tonumber(current) == 1 then "
            + " redis.call('expire',KEYS[1],ARGV[1]) "
            + " end ";
    private static final String LuaPeriodScript = " local current;"
            + " redis.call('zadd',KEYS[1],ARGV[1],ARGV[2]);"
            + "current = redis.call('zcount', KEYS[1], '-inf', '+inf');"
            + " if tonumber(current) == 1 then "
            + " redis.call('expire',KEYS[1],ARGV[3]) "
            + " end ";

    public RedisRateCounter(Jedis jedis) {
        this.jedis = jedis;
    }

    /**
     * 传入key值进行验证
     * @param key
     * @return
     */
    public int count(String key, TimeUnit timeUnit, boolean inc) {
        int rtv = 0;
        if (timeUnit == TimeUnit.SECONDS) {
            //秒级别
            String keyName = getKeyName(jedis, key, timeUnit);
            String current = jedis.get(keyName);
            if (current != null) {
                rtv = Integer.parseInt(current);
            }

            if (inc) {
                List<String> keys = new ArrayList<String>();
                keys.add(keyName);
                List<String> argvs = new ArrayList<String>();
                argvs.add(getExpire(timeUnit) + "");
                jedis.eval(LuaSecondsScript, keys, argvs);
                rtv++;
            }
        } else {
            rtv = periodCount(jedis, key, timeUnit, inc);
        }

        return rtv;
    }

    public void resetCount(String key) {
        ScanResult<String> scan = jedis.scan("0", new ScanParams().match(key + ":*"));
        for (String t: scan.getResult()) {
            jedis.del(t);
        }
    }

    /**
     * redis查询数据，写入数据和判断是否超标
     * @param jedis
     * @param key
     * @param timeUnit
     * @return
     */
    private int periodCount(Jedis jedis, String key, TimeUnit timeUnit, boolean inc) {
        int period = toSeconds(timeUnit);
        String[] keyNames = getKeyNames(jedis, key, timeUnit);
        //返回2个，第1个是秒计数10位，第2个是微秒6位
        List<String> jedisTime = jedis.time();
        String currentSecondIndex = jedisTime.get(0);
        String previousSecondIndex = (Long.parseLong(currentSecondIndex) - period) + "";

        long currentCount = jedis.zcount(keyNames[0], previousSecondIndex, currentSecondIndex)
                + jedis.zcount(keyNames[1], previousSecondIndex, currentSecondIndex);

        if (inc) {
            // 记录访问的总次数
            List<String> keys = new ArrayList<String>();
            keys.add(keyNames[1]);
            List<String> argvs = new ArrayList<String>();
            argvs.add(currentSecondIndex);
            //不用UUID是因为UUID是36个字符比较长，下面方法只有20位，而且冲突可能性已很少
            argvs.add(jedisTime.get(0) + jedisTime.get(1) + RandomStringUtils.randomAlphanumeric(4));
            argvs.add(getExpire(timeUnit) + "");
            jedis.eval(LuaPeriodScript, keys, argvs);
            currentCount++;
        }

        return (int)currentCount;
    }

    private int toSeconds(TimeUnit timeUnit) {
        int seconds = 0;
        switch (timeUnit) {
            case SECONDS:
                seconds = 1;
            case MINUTES:
                seconds = 60;
                break;
            case HOURS:
                seconds = 3600;
                break;
            case DAYS:
                seconds = 86400;
                break;
        }
        return seconds;
    }

    /**
     * 获取当前键值对
     * @param jedis
     * @param key
     * @return
     */
    private String getKeyName(Jedis jedis, String key, TimeUnit timeUnit) {
        String keyName = null;
        if (timeUnit == TimeUnit.SECONDS) {
            keyName = key + ":" + jedis.time().get(0);
        } else if (timeUnit == TimeUnit.MINUTES) {
            keyName = key + ":" + Long.parseLong(jedis.time().get(0)) / 60;
        } else if (timeUnit == TimeUnit.HOURS) {
            keyName = key + ":" + Long.parseLong(jedis.time().get(0)) / 3600;
        } else if (timeUnit == TimeUnit.DAYS) {
            keyName = key + ":" + Long.parseLong(jedis.time().get(0)) / (3600 * 24);
        } else {
            throw new IllegalArgumentException("Not support the TimeUnit: " + timeUnit);
        }
        return keyName;
    }

    private String[] getKeyNames(Jedis jedis, String key, TimeUnit timeUnit) {
        String[] keyNames = null;
        if (timeUnit == TimeUnit.MINUTES) {
            long index = Long.parseLong(jedis.time().get(0)) / 60;
            String keyName1 = key + ":" + (index - 1);
            String keyName2 = key + ":" + index;
            keyNames = new String[] { keyName1, keyName2 };
        } else if (timeUnit == TimeUnit.HOURS) {
            long index = Long.parseLong(jedis.time().get(0)) / 3600;
            String keyName1 = key + ":" + (index - 1);
            String keyName2 = key + ":" + index;
            keyNames = new String[] { keyName1, keyName2 };
        } else if (timeUnit == TimeUnit.DAYS) {
            long index = Long.parseLong(jedis.time().get(0)) / (3600 * 24);
            String keyName1 = key + ":" + (index - 1);
            String keyName2 = key + ":" + index;
            keyNames = new String[] { keyName1, keyName2 };
        } else {
            throw new IllegalArgumentException("Not support the TimeUnit: " + timeUnit);
        }
        return keyNames;
    }

    private int getExpire(TimeUnit timeUnit) {
        int expire = 0;
        if (timeUnit == TimeUnit.SECONDS) {
            expire = 10;
        } else if (timeUnit == TimeUnit.MINUTES) {
            expire = 2 * 60 + 10;
        } else if (timeUnit == TimeUnit.HOURS) {
            expire = 2 * 3600 + 10;
        } else if (timeUnit == TimeUnit.DAYS) {
            expire = 2 * 3600 * 24 + 10;
        } else {
            throw new IllegalArgumentException("Not support the TimeUnit: " + timeUnit);
        }
        return expire;
    }
}
