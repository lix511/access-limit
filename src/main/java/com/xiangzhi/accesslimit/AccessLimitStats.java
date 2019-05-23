package com.xiangzhi.accesslimit;

import java.util.Date;

/**
 * 如果 limitType != LimitType.NOT_LIMIT and isNewLimit = false 表示是本次请求之前就已被禁止/警告，并且还仍在禁止/警告期间；
 * 并且在这种情况下，countPerMinute，countPerHour，countPerDay，warningsCount，limitReason也都不会计算，直接返回null。
 * @author itcamel
 */
public class AccessLimitStats implements Comparable<AccessLimitStats> {
    private String userIdentify;
    private Integer countPerMinute;
    private Integer countPerHour;
    private Integer countPerDay;
    private Integer warningsCount;
    private LimitType limitType = LimitType.NOT_LIMIT;
    private String limitReason;
    private Date limitStart;
    private Boolean newLimit = false;

    public void clearCounts() {
        setCountPerMinute(null);
        setCountPerHour(null);
        setCountPerDay(null);
        setWarningsCount(null);
        setLimitReason(null);
    }

    public String getUserIdentify() {
        return userIdentify;
    }

    public void setUserIdentify(String userIdentify) {
        this.userIdentify = userIdentify;
    }

    public Integer getCountPerMinute() {
        return countPerMinute;
    }

    public void setCountPerMinute(Integer countPerMinute) {
        this.countPerMinute = countPerMinute;
    }

    public Integer getCountPerHour() {
        return countPerHour;
    }

    public void setCountPerHour(Integer countPerHour) {
        this.countPerHour = countPerHour;
    }

    public Integer getCountPerDay() {
        return countPerDay;
    }

    public void setCountPerDay(Integer countPerDay) {
        this.countPerDay = countPerDay;
    }

    public Integer getWarningsCount() {
        return warningsCount;
    }

    public void setWarningsCount(Integer warningsCount) {
        this.warningsCount = warningsCount;
    }

    public LimitType getLimitType() {
        return limitType;
    }

    public void setLimitType(LimitType limitType) {
        this.limitType = limitType;
    }

    public String getLimitReason() {
        return limitReason;
    }

    public void setLimitReason(String limitReason) {
        this.limitReason = limitReason;
    }

    public Date getLimitStart() {
        return limitStart;
    }

    public void setLimitStart(Date limitStart) {
        this.limitStart = limitStart;
    }

    public Boolean getNewLimit() {
        return newLimit;
    }

    public void setNewLimit(Boolean newLimit) {
        this.newLimit = newLimit;
    }

    @Override
    public int compareTo(AccessLimitStats o) {
        int i = o.countPerDay.compareTo(this.countPerDay);
        if (i == 0) {
            i = o.countPerHour.compareTo(this.countPerHour);
            if (i == 0) {
                i = o.countPerMinute.compareTo(this.countPerMinute);
            }
        }
        return i;
    }
}
