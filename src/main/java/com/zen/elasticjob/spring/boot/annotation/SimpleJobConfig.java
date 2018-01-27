package com.zen.elasticjob.spring.boot.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface SimpleJobConfig {

    /**
     * 作业启动时间的cron表达式.
     */
	String cron();

	/**
	 * 分片数量
	 * @return
	 */
	String shardingTotalCount() default "1";

    /**
     * 分片序列号和个性化参数对照表.
     *
     * <p>
     * 分片序列号和参数用等号分隔, 多个键值对用逗号分隔. 类似map.
     * 分片序列号从0开始, 不可大于或等于作业分片总数.
     * 如:
     * 0=a,1=b,2=c
     * </p>
     */
	String shardingItemParameters() default "";
    /**
     * 作业自定义参数.
     *
     * <p>
     * 可以配置多个相同的作业, 但是用不同的参数作为不同的调度实例.
     * </p>
     */
	String jobParameter() default "";


    /**
     * 本地配置是否可覆盖注册中心配置.
     * 如果可覆盖, 每次启动作业都以本地配置为准.
     */
    String overwrite() default "true";

    /**
     * 作业描述信息.
     */
    String description() default  "";
}