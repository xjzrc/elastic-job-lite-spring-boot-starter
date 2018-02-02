 elastic-job-spring-boot-starter
===================================
 elastic-job-spring-boot-starter
让你可以使用spring-boot的方式开发依赖elastic-job的程序

*****

### 使用步骤(示例:[「spring-boot-starter-demo」](https://github.com/xjzrc/spring-boot-starter-demo))

* 在`spring boot`项目的`pom.xml`中添加以下依赖：

根据实际情况依赖最新版本
```xml
<dependency>
    <groupId>com.zen.spring.boot</groupId>
    <artifactId>elastic-job-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

* 在application.properties添加elasticjob的相关配置信息,样例配置如下:

```properties
spring.elasticjob.zookeeper.serverLists=127.0.0.1:2181
spring.elasticjob.zookeeper.namespace=elastic-job-spring-boot-stater-demo
```

* 编写你的作业服务,只需在作业任务类上添加`@ElasticJobConfig`（import com.zen.elasticjob.spring.boot.annotation.ElasticJobConfig）注解 ,其中cron是作业执行时间.

Simple作业配置
```java
@ElasticJobConfig(cron = "0/2 * * * * ?", shardingTotalCount = 3, shardingItemParameters = "0=Beijing,1=Shanghai,2=Guangzhou")
public class SpringSimpleJob implements SimpleJob {

    @Resource
    private FooRepository fooRepository;

    @Override
    public void execute(final ShardingContext shardingContext) {
        System.out.println(String.format("Item: %s | Time: %s | Thread: %s | %s",
                shardingContext.getShardingItem(), new SimpleDateFormat("HH:mm:ss").format(new Date()), Thread.currentThread().getId(), "SIMPLE"));
        List<Foo> data = fooRepository.findTodoData(shardingContext.getShardingParameter(), 10);
        for (Foo each : data) {
            fooRepository.setCompleted(each.getId());
        }
    }
}
```

dataflow作业配置
```java
@ElasticJobConfig(cron = "0/2 * * * * ?", shardingTotalCount = 3, shardingItemParameters = "0=Beijing,1=Shanghai,2=Guangzhou")
public class SpringDataflowJob implements DataflowJob<Foo> {

    @Resource
    private FooRepository fooRepository;

    @Override
    public List<Foo> fetchData(final ShardingContext shardingContext) {
        System.out.println(String.format("Item: %s | Time: %s | Thread: %s | %s",
                shardingContext.getShardingItem(), new SimpleDateFormat("HH:mm:ss").format(new Date()), Thread.currentThread().getId(), "DATAFLOW FETCH"));
        return fooRepository.findTodoData(shardingContext.getShardingParameter(), 10);
    }

    @Override
    public void processData(final ShardingContext shardingContext, final List<Foo> data) {
        System.out.println(String.format("Item: %s | Time: %s | Thread: %s | %s",
                shardingContext.getShardingItem(), new SimpleDateFormat("HH:mm:ss").format(new Date()), Thread.currentThread().getId(), "DATAFLOW PROCESS"));
        for (Foo each : data) {
            fooRepository.setCompleted(each.getId());
        }
    }
}
```