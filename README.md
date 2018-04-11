 elastic-job-lite-spring-boot-starter
===================================
 elastic-job-lite-spring-boot-starter
让你可以使用spring-boot的方式开发依赖elastic-job的程序

*****

### 使用步骤(示例:[「spring-boot-starter-demo」](https://github.com/xjzrc/spring-boot-starter-demo))

* 在`spring boot`项目的`pom.xml`中添加以下依赖：

根据实际情况依赖最新版本(当前最新版1.1.0)
```xml
<dependency>
    <groupId>com.github.xjzrc.spring.boot</groupId>
    <artifactId>elastic-job-lite-spring-boot-starter</artifactId>
    <version>${lasted.release.version}</version>
</dependency>
```

* 在application.properties添加elasticjob的相关配置信息,样例配置如下:

```properties
#注册中心配置
spring.elasticjob.zookeeper.serverLists = 127.0.0.1:2181
spring.elasticjob.zookeeper.namespace = elastic-job-spring-boot-stater-demo
#simple作业配置
simpleJob.cron = 0/2 * * * * ?
simpleJob.shardingTotalCount = 3
simpleJob.shardingItemParameters = 0=Beijing,1=Shanghai,2=Guangzhou
#dataflow作业配置
dataflowJob.cron = 0/2 * * * * ?
dataflowJob.shardingTotalCount = 3
dataflowJob.shardingItemParameters = 0=Beijing,1=Shanghai,2=Guangzhou
#script作业配置
scriptJob.cron = 0/2 * * * * ?
scriptJob.shardingTotalCount = 3
scriptJob.shardingItemParameters = 0=Beijing,1=Shanghai,2=Guangzhou
scriptJob.scriptCommandLine = yourPath/spring-boot-starter-demo/elastic-job-spring-boot-starter-demo/src/main/resources/script/demo.sh
```

* 编写你的作业服务,只需在作业任务类上添加`@ElasticJobConfig`（import com.zen.elasticjob.spring.boot.annotation.ElasticJobConfig）注解 ,其中cron是作业执行时间.

Simple作业配置
```java
@ElasticJobConfig(cron = "${simpleJob.cron}", shardingTotalCount = "${simpleJob.shardingTotalCount}", shardingItemParameters = "${simpleJob.shardingItemParameters}")
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
@ElasticJobConfig(cron = "${dataflowJob.cron}", shardingTotalCount = "${dataflowJob.shardingTotalCount}", shardingItemParameters = "${dataflowJob.shardingItemParameters}")
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
script作业配置(# need absolute path)
```java
@ElasticJobConfig(cron = "${scriptJob.cron}", shardingTotalCount = "${scriptJob.shardingTotalCount}",
        shardingItemParameters = "${scriptJob.shardingItemParameters}",
        scriptCommandLine = "${scriptJob.cron}")
public class SpringScripJob implements ScriptJob {
}
```