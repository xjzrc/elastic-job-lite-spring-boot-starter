 elastic-job-lite-spring-boot-starter
===================================
 elastic-job-lite-spring-boot-starter
让你可以使用spring-boot的方式开发依赖elastic-job的程序

*****

### 使用步骤(示例:[「spring-boot-starter-demo」](https://github.com/xjzrc/spring-boot-starter-demo))

* 在`spring boot`项目的`pom.xml`中添加以下依赖：

根据实际情况依赖最新版本(当前最新版2.0.0)
```xml
<dependency>
    <groupId>com.github.xjzrc.spring.boot</groupId>
    <artifactId>elastic-job-lite-spring-boot-starter</artifactId>
    <version>${lasted.release.version}</version>
</dependency>
```

* 编写你的作业服务

Simple作业配置
```java
public class SpringSimpleJob implements com.dangdang.ddframe.job.api.simple.SimpleJob {

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
public class SpringDataflowJob implements com.dangdang.ddframe.job.api.dataflow.DataflowJob<Foo> {

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

* 在application.yml添加elasticjob的相关配置信息,样例配置如下:

```yml
spring:
  elasticjob:
    #注册中心配置
    zookeeper:
      server-lists: 127.0.0.1:6181
      namespace: elastic-job-spring-boot-stater-demo
    #简单作业配置
    simples:
      #spring简单作业示例配置
      spring-simple-job:
        #配置简单作业，必须实现com.dangdang.ddframe.job.api.simple.SimpleJob
        job-class: com.zen.spring.boot.demo.elasticjob.job.SpringSimpleJob
        cron: 0/2 * * * * ?
        sharding-total-count: 3
        sharding-item-parameters: 0=Beijing,1=Shanghai,2=Guangzhou
        #配置监听器
        listener:
          #配置每台作业节点均执行的监听，必须实现com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener
          listener-class: com.zen.spring.boot.demo.elasticjob.listener.MyElasticJobListener
    #流式作业配置
    dataflows:
      #spring简单作业示例配置
      spring-dataflow-job:
        #配置简单作业，必须实现com.dangdang.ddframe.job.api.dataflow.DataflowJob<T>
        job-class: com.zen.spring.boot.demo.elasticjob.job.SpringDataflowJob
        cron: 0/2 * * * * ?
        sharding-total-count: 3
        sharding-item-parameters: 0=Beijing,1=Shanghai,2=Guangzhou
        streaming-process: true
        #配置监听器
        listener:
          #配置分布式场景中仅单一节点执行的监听，必须实现com.dangdang.ddframe.job.lite.api.listener.AbstractDistributeOnceElasticJobListener
          distributed-listener-class: com.zen.spring.boot.demo.elasticjob.listener.MyDistributeElasticJobListener
          started-timeout-milliseconds: 5000
          completed-timeout-milliseconds: 10000
    #脚本作业配置
    scripts:
      #脚本作业示例配置
      script-job:
        cron: 0/2 * * * * ?
        sharding-total-count: 3
        sharding-item-parameters: 0=Beijing,1=Shanghai,2=Guangzhou
        script-command-line: youPath/spring-boot-starter-demo/elastic-job-spring-boot-starter-demo/src/main/resources/script/demo.bat
```
