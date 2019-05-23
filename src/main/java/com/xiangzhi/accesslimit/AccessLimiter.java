package com.xiangzhi.accesslimit;

import org.apache.commons.lang.StringUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 访问频次控制器。
 * 可以根据每分钟，每小时，每天的访问量对请求进行判断是否合法。
 * 采用redis存储，支持分布式环境部署。
 *
 * @author itcamel
 */
public class AccessLimiter {
    /**
     * 每分钟/每小时/每天的请求次数限制
     */
    private int limitOfOneMinute = 10, limitOfOneHour = 180, limitOfOneDay = 1800;
    /**
     * 警告最大次数
     */
    private int maxWarningTimes = 2;
    /**
     * 警告时长：10分钟，秒计
     */
    private long warningSeconds = 10 * 60;
    /**
     * 禁用时长：1天，秒计
     */
    private long forbiddenSeconds = 24 * 3600;

    /**
     * redis连接池
     */
    private JedisPool jedisPool;
    /**
     * 资源类型
     */
    private String resource;

    private String redisKeyWarningCounters;
    private String redisKeyWarningUser;
    private String redisKeyForbiddenUser;
    private String redisKeyRateCounter;
    private String redisKeyLock;

    public AccessLimiter(JedisPool jedisPool, String resource) {
        this.jedisPool = jedisPool;
        this.resource = resource;

        redisKeyWarningCounters = Consts.REDIS_KEY_WARNING_COUNTERS + resource;
        redisKeyWarningUser = Consts.REDIS_KEY_WARNING_USER + resource + ":";
        redisKeyForbiddenUser = Consts.REDIS_KEY_FORBIDDEN_USER + resource + ":";
        redisKeyRateCounter = Consts.REDIS_KEY_RATE_COUNTER + resource + ":";
        redisKeyLock = Consts.REDIS_KEY_LOCK + resource + ":";
    }

    public void inc(String userIdentify) throws AccessLimitException, LockTimeoutException {
        assert jedisPool != null : "jedisPool not allowed null";

        try (Jedis jedis = jedisPool.getResource()) {
            AccessLimitStats stats = getAccessLimitStats(jedis, userIdentify, true, false);
            if (stats.getLimitType() != LimitType.NOT_LIMIT) {
                throw new AccessLimitException("limit at " + stats.getLimitStart(), stats);
            }

            //判断是否超过访问限制
            if (stats.getCountPerMinute() >= limitOfOneMinute) {
                stats.setLimitReason("overOneMinuteLimit");
            } else if (stats.getCountPerHour() >= limitOfOneHour) {
                stats.setLimitReason("overOneHourLimit");
            } else if (stats.getCountPerDay() >= limitOfOneDay) {
                stats.setLimitReason("overOneDayLimit");
            }
            if (stats.getLimitReason() != null) {
                String lockPerUser = redisKeyLock + userIdentify;
                RedisLocker redisLocker = new RedisLocker(jedis);
                try {
                    //用户级分布式锁
                    boolean getLock = redisLocker.tryLock(lockPerUser, Long.valueOf(5));
                    if (getLock) {
                        //再次查看用户是否处于禁止或者警告期间
                        handAccessLimit(jedis, stats);
                        if (stats.getLimitType() != LimitType.NOT_LIMIT) {
                            stats.clearCounts();
                            throw new AccessLimitException("limit at " + stats.getLimitStart(), stats);
                        } else {
                            LocalDateTime now = LocalDateTime.now();
                            String curTime = Consts.DATE_TIME_FORMATTER.format(now);

                            stats.setNewLimit(true);
                            stats.setLimitStart(toDate(now));
                            long warningsCount = jedis.hincrBy(redisKeyWarningCounters, userIdentify, 1);
                            stats.setWarningsCount((int) warningsCount);
                            if (warningsCount <= maxWarningTimes) {
                                stats.setLimitType(LimitType.WARNING);
                                //NX是不存在时才set， XX是存在时才set， EX是秒，PX是毫秒
                                jedis.set(redisKeyWarningUser + userIdentify, curTime, "NX", "EX", warningSeconds);
                            } else {
                                stats.setLimitType(LimitType.FORBIDDEN);
                                jedis.set(redisKeyForbiddenUser + userIdentify, curTime, "NX", "EX", forbiddenSeconds);
                            }

                            throw new AccessLimitException(stats.getLimitReason(), stats);
                        }
                    } else {
                        throw new LockTimeoutException("lock failed. userIdentify=" + userIdentify);
                    }
                } finally {
                    redisLocker.realseLock(lockPerUser);
                }
            }
        }
    }

    public void clearLimit(String userIdentify) {
        assert jedisPool != null : "jedisPool not allowed null";
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(redisKeyWarningUser + userIdentify);
            jedis.del(redisKeyForbiddenUser + userIdentify);
            jedis.hdel(redisKeyWarningCounters, userIdentify);

            new RedisRateCounter(jedis).resetCount(redisKeyRateCounter + userIdentify);
        }
    }

    public AccessLimitStats getAccessLimitStats(String userIdentify) {
        assert jedisPool != null : "jedisPool not allowed null";

        try (Jedis jedis = jedisPool.getResource()) {
            return getAccessLimitStats(jedis, userIdentify, false, true);
        }
    }

    /**
     * 优先判断是否已处于禁止/警告期间，只有未被限制访问的情况下，inc=true时当前访问次数才会记录
     *
     * @param jedis
     * @param userIdentify
     * @param inc
     * @return
     */
    private AccessLimitStats getAccessLimitStats(Jedis jedis, String userIdentify, boolean inc, boolean force) {
        AccessLimitStats stats = new AccessLimitStats();
        stats.setUserIdentify(userIdentify);

        handAccessLimit(jedis, stats);
        if (stats.getLimitType() == LimitType.NOT_LIMIT || force) {
            String warningsCount = jedis.hget(redisKeyWarningCounters, userIdentify);
            if (StringUtils.isNotEmpty(warningsCount)) {
                stats.setWarningsCount(Integer.parseInt(warningsCount));
            } else {
                stats.setWarningsCount(0);
            }

            RedisRateCounter rateCounter = new RedisRateCounter(jedis);
            String rateKey = redisKeyRateCounter + userIdentify;
            stats.setCountPerMinute(rateCounter.count(rateKey, TimeUnit.MINUTES, inc));
            stats.setCountPerHour(rateCounter.count(rateKey, TimeUnit.HOURS, inc));
            stats.setCountPerDay(rateCounter.count(rateKey, TimeUnit.DAYS, inc));
        }
        return stats;
    }

    private void handAccessLimit(Jedis jedis, AccessLimitStats stats) {
        String limitTime = jedis.get(redisKeyForbiddenUser + stats.getUserIdentify());
        if (StringUtils.isNotEmpty(limitTime)) {
            stats.setLimitStart(parseDate(limitTime));
            stats.setLimitType(LimitType.FORBIDDEN);
        } else if (StringUtils.isNotEmpty(limitTime = jedis.get(redisKeyWarningUser + stats.getUserIdentify()))) {
            stats.setLimitStart(parseDate(limitTime));
            stats.setLimitType(LimitType.WARNING);
        }
    }

    public Date toDate(LocalDateTime localDateTime) {
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDateTime.atZone(zone).toInstant();
        return Date.from(instant);
    }

    public Date parseDate(String dateStr) {
        return toDate(LocalDateTime.parse(dateStr, Consts.DATE_TIME_FORMATTER));
    }

    public int getLimitOfOneMinute() {
        return limitOfOneMinute;
    }

    public void setLimitOfOneMinute(int limitOfOneMinute) {
        this.limitOfOneMinute = limitOfOneMinute;
    }

    public int getLimitOfOneHour() {
        return limitOfOneHour;
    }

    public void setLimitOfOneHour(int limitOfOneHour) {
        this.limitOfOneHour = limitOfOneHour;
    }

    public int getLimitOfOneDay() {
        return limitOfOneDay;
    }

    public void setLimitOfOneDay(int limitOfOneDay) {
        this.limitOfOneDay = limitOfOneDay;
    }

    public int getMaxWarningTimes() {
        return maxWarningTimes;
    }

    public void setMaxWarningTimes(int maxWarningTimes) {
        this.maxWarningTimes = maxWarningTimes;
    }

    public long getWarningSeconds() {
        return warningSeconds;
    }

    public void setWarningSeconds(long warningSeconds) {
        this.warningSeconds = warningSeconds;
    }

    public long getForbiddenSeconds() {
        return forbiddenSeconds;
    }

    public void setForbiddenSeconds(long forbiddenSeconds) {
        this.forbiddenSeconds = forbiddenSeconds;
    }
}
