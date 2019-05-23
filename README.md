![GitHub tag (latest SemVer)](https://img.shields.io/github/tag/xiangzhitech/access-limit.svg)
![GitHub All Releases](https://img.shields.io/github/downloads/xiangzhitech/access-limit/total.svg)

# Access Limit - 频次控制器
本工具包可用于对资源访问频次的记录与控制，具有以下特性：
- 资源控制粒度可自定义。
- 频次监控类型包括：每分钟，每小时，每天三个统计单位。
- 支持分布式环境部署。
- 支持警告，禁用两种拦截方式。

# 依赖
- redis

# 项目中通过maven引用方式
```
<dependency>
  <groupId>io.github.lix511</groupId>
  <artifactId>access-limit</artifactId>
  <version>1.0</version>
</dependency>
```

# 代码使用说明
```java
//构造函数第二个参数：是根据需要控制的粒度起的一个名称，例如你想控制视频播放频次，那么这里传入"video"
AccessLimiter al = new AccessLimiter(jedisPool, "video");
...

//以下代码在需要进行频次控制的地方调用
try {
    //参数"userIdentify"可以是userId，username等能唯一标识用户的值
    al.inc("userIdentify");
} catch (AccessLimitException e) {
    //如果超过设定的调用频次限制，则会抛出AccessLimitException异常
    AccessLimitStats stats = e.getStats();
    //todo 可以根据得到的相关控制信息，进行后续的业务逻辑处理
} 
```