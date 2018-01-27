 elastic-job-spring-boot-starter
===================================
 elastic-job-spring-boot-starter
让你可以使用spring-boot的方式开发依赖elastic-job的程序

*****

### 使用步骤

* 在`spring boot`项目的`pom.xml`中添加以下依赖：

根据实际情况依赖最新版本
```xml
<dependency>
    <groupId>com.zen.boot</groupId>
    <artifactId>elastic-job-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

<br/>
1.在application.properties添加相关配置信息,样例配置如下:
<br/>
```properties
spring.elastic.job.zookeeper=127.0.0.1:2181
spring.elastic.job.namespace=dc
```
<br/>
2.job类继承com.dangdang.ddframe.job.plugin.job.type.simple.AbstractSimpleElasticJob
<br/>
<br/>
3.job类添加注解:
<br/>
```java
@ElasticJobConfig(cron = "作业启动时间的cron表达式.",
                    shardingTotalCount = 分片数量,
                    shardingItemParameters="分片序列号和个性化参数对照表,分片序列号从0开始, 不可大于或等于作业分片总数,0=a,1=b,2=c",
                    jobParameter="作业自定义参数(可以配置多个相同的作业, 但是用不同的参数作为不同的调度实例.)",
                    overwrite=本地配置是否可覆盖注册中心配置.如果可覆盖, 每次启动作业都以本地配置为准.
                    description = "作业描述信息.")
```





