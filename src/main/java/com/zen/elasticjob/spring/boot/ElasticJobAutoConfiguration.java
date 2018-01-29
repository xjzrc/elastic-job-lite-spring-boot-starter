package com.zen.elasticjob.spring.boot;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties.JobPropertiesEnum;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.zen.elasticjob.spring.boot.annotation.ElasticJobConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.Annotation;
import java.util.Objects;


@Configuration
@ConditionalOnClass(ElasticJob.class)
@ConditionalOnBean(annotation = ElasticJobConfig.class)
@AutoConfigureAfter(RegistryCenterAutoConfiguration.class)
public class ElasticJobAutoConfiguration implements BeanPostProcessor {

    @Autowired
    private ZookeeperRegistryCenter regCenter;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ElasticJob) {
            Annotation[] beanAnnotation = bean.getClass().getAnnotations();
            for (Annotation annotation : beanAnnotation) {
                if (annotation instanceof ElasticJobConfig) {
                    ElasticJobConfig elasticJobConfig = (ElasticJobConfig) annotation;
                    ElasticJob elasticJob = (ElasticJob) bean;
                    new SpringJobScheduler(elasticJob, regCenter, getLiteJobConfiguration(elasticJob.getClass(), elasticJobConfig)).init();
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * 构建任务核心配置
     *
     * @param jobClass         任务执行类
     * @param elasticJobConfig 任务配置
     * @return JobCoreConfiguration
     */
    private JobCoreConfiguration getJobCoreConfiguration(final Class<? extends ElasticJob> jobClass, ElasticJobConfig elasticJobConfig) {
        return JobCoreConfiguration.newBuilder(jobClass.getName(), elasticJobConfig.cron(), elasticJobConfig.shardingTotalCount())
                .shardingItemParameters(elasticJobConfig.shardingItemParameters())
                .jobParameter(elasticJobConfig.jobParameter())
                .failover(elasticJobConfig.failover())
                .misfire(elasticJobConfig.misfire())
                .description(elasticJobConfig.description())
                .jobProperties(JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), elasticJobConfig.jobExceptionHandler())
                .jobProperties(JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), elasticJobConfig.executorServiceHandler())
                .build();
    }

    /**
     * 构建Lite作业
     *
     * @param jobClass         任务执行类
     * @param elasticJobConfig 任务配置
     * @return LiteJobConfiguration
     */
    private LiteJobConfiguration getLiteJobConfiguration(final Class<? extends ElasticJob> jobClass, ElasticJobConfig elasticJobConfig) {

        //构建核心配置
        JobCoreConfiguration jobCoreConfiguration = getJobCoreConfiguration(jobClass, elasticJobConfig);

        //构建任务类型配置
        JobTypeConfiguration jobTypeConfiguration = null;
        switch (elasticJobConfig.jonType()) {
            case DATAFLOW:
                jobTypeConfiguration = new DataflowJobConfiguration(jobCoreConfiguration, jobClass.getCanonicalName(), elasticJobConfig.streamingProcess());
                break;
            case SCRIPT:
                break;
            case SIMPLE:
            default:
                jobTypeConfiguration = new SimpleJobConfiguration(jobCoreConfiguration, jobClass.getCanonicalName());
        }

        //构建Lite作业
        return LiteJobConfiguration.newBuilder(Objects.requireNonNull(jobTypeConfiguration))
                .overwrite(elasticJobConfig.overwrite()).build();

    }

}