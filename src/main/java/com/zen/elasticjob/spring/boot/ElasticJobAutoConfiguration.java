package com.zen.elasticjob.spring.boot;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.api.JobType;
import com.dangdang.ddframe.job.api.dataflow.DataflowJob;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties.JobPropertiesEnum;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.zen.elasticjob.spring.boot.annotation.ElasticJobConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;


@Configuration
@ConditionalOnClass(ElasticJob.class)
@ConditionalOnBean(annotation = ElasticJobConfig.class)
@AutoConfigureAfter(RegistryCenterAutoConfiguration.class)
public class ElasticJobAutoConfiguration {

    @Autowired
    private ZookeeperRegistryCenter regCenter;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {

        //初始化简单任务作业
        initSimpleJob();

        //初始化流水线任务作业
        initDataflowJob();
    }

    /**
     * 初始化简单任务作业
     */
    private void initSimpleJob() {
        Map<String, SimpleJob> simpleJobMap = applicationContext.getBeansOfType(SimpleJob.class);
        for (Map.Entry<String, SimpleJob> entry : simpleJobMap.entrySet()) {
            SimpleJob simpleJob = entry.getValue();
            ElasticJobConfig elasticJobConfig = simpleJob.getClass().getAnnotation(ElasticJobConfig.class);
            new SpringJobScheduler(simpleJob, regCenter, getLiteJobConfiguration(JobType.SIMPLE, simpleJob.getClass(), elasticJobConfig)).init();
        }
    }

    /**
     * 初始化流水线任务作业
     */
    private void initDataflowJob() {
        Map<String, DataflowJob> dataflowJobMap = applicationContext.getBeansOfType(DataflowJob.class);
        for (Map.Entry<String, DataflowJob> entry : dataflowJobMap.entrySet()) {
            DataflowJob dataflowJob = entry.getValue();
            ElasticJobConfig elasticJobConfig = dataflowJob.getClass().getAnnotation(ElasticJobConfig.class);
            new SpringJobScheduler(dataflowJob, regCenter, getLiteJobConfiguration(JobType.DATAFLOW, dataflowJob.getClass(), elasticJobConfig)).init();
        }
    }

    /**
     * 构建任务核心配置
     *
     * @param jobName          任务执行名称
     * @param elasticJobConfig 任务配置
     * @return JobCoreConfiguration
     */
    private JobCoreConfiguration getJobCoreConfiguration(String jobName, ElasticJobConfig elasticJobConfig) {
        JobCoreConfiguration.Builder builder = JobCoreConfiguration.newBuilder(jobName, elasticJobConfig.cron(), elasticJobConfig.shardingTotalCount())
                .shardingItemParameters(elasticJobConfig.shardingItemParameters())
                .jobParameter(elasticJobConfig.jobParameter())
                .failover(elasticJobConfig.failover())
                .misfire(elasticJobConfig.misfire())
                .description(elasticJobConfig.description());
        if (StringUtils.isNotBlank(elasticJobConfig.jobExceptionHandler())) {
            builder.jobProperties(JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), elasticJobConfig.jobExceptionHandler());
        }
        if (StringUtils.isNotBlank(elasticJobConfig.executorServiceHandler())) {
            builder.jobProperties(JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), elasticJobConfig.executorServiceHandler());
        }
        return builder.build();
    }

    /**
     * 构建Lite作业
     *
     * @param jobType          任务类型
     * @param jobClass         任务执行类
     * @param elasticJobConfig 任务配置
     * @return LiteJobConfiguration
     */
    private LiteJobConfiguration getLiteJobConfiguration(final JobType jobType, final Class<? extends ElasticJob> jobClass, ElasticJobConfig elasticJobConfig) {

        //构建核心配置
        JobCoreConfiguration jobCoreConfiguration = getJobCoreConfiguration(jobClass.getName(), elasticJobConfig);

        //构建任务类型配置
        JobTypeConfiguration jobTypeConfiguration = null;
        switch (jobType) {
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