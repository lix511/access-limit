# Access Limit - 频次控制器
本工具包可用于对资源访问频次的记录与控制，具有以下特性：
- 资源控制粒度可自定义。
- 频次监控类型包括：每分钟，每小时，每天三个统计单位。
- 支持分布式环境部署。
- 支持警告，禁用两种拦截方式。

# 依赖
- redis

# 使用方式
```java
AccessLimiter al = new AccessLimiter(jedisPool, "resource");
...

try {
    al.inc('userIdentify');
} catch (AccessLimitException e) {
    AccessLimitStats stats = e.getStats();
    //todo 可以根据得到的相关控制信息，进行后续的业务逻辑处理
} catch (LockTimeoutException e) {
    e.printStackTrace();
}
```