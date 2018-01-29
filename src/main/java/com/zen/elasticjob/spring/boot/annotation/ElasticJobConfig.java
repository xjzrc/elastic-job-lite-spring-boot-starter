package com.zen.elasticjob.spring.boot.annotation;

import com.dangdang.ddframe.job.api.JobType;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ElasticJobConfig {

    @AliasFor("cron")
    String value() default "";

    /**
     * cron表达式，用于控制作业触发时间
     */
    @AliasFor("value")
    String cron() default "";

    /**
     * 作业分片总
     */
    int shardingTotalCount() default 1;

    /**
     * 分片序列号和个性化参数对照表.
     * 分片序列号和参数用等号分隔, 多个键值对用逗号分隔. 类似map.
     * 分片序列号从0开始, 不可大于或等于作业分片总数.
     * 如:
     * 0=a,1=b,2=c
     */
    String shardingItemParameters() default "";

    /**
     * 作业自定义参数.
     * 作业自定义参数，可通过传递该参数为作业调度的业务方法传参，用于实现带参数的作业
     * 例：每次获取的数据量、作业实例从数据库读取的主键等
     */
    String jobParameter() default "";

    /**
     * 是否开启任务执行失效转移，开启表示如果作业在一次任务执行中途宕机，
     * 允许将该次未完成的任务在另一作业节点上补偿执行
     */
    boolean failover() default false;

    /**
     * 是否开启错过任务重新执行
     */
    boolean misfire() default true;

    /**
     * 作业描述信息.
     */
    String description() default "";

    /**
     * 配置jobProperties定义的枚举控制Elastic-Job的实现细节
     * JOB_EXCEPTION_HANDLER用于扩展异常处理类
     */
    String jobExceptionHandler() default "";

    /**
     * 配置jobProperties定义的枚举控制Elastic-Job的实现细节
     * EXECUTOR_SERVICE_HANDLER用于扩展作业处理线程池类
     */
    String executorServiceHandler() default "";

    /**
     * 是否流式处理数据
     * 如果流式处理数据, 则fetchData不返回空结果将持续执行作业
     * 如果非流式处理数据, 则处理数据完成后作业结
     */
    boolean streamingProcess() default false;

    /**
     * 脚本型作业执行命令行
     */
    String scriptCommandLine() default "";


    /**
     * 本地配置是否可覆盖注册中心配置.
     * 如果可覆盖, 每次启动作业都以本地配置为准.
     */
    boolean overwrite() default true;
}