package com.zen.elasticjob.spring.boot;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.api.JobType;
import com.dangdang.ddframe.job.api.dataflow.DataflowJob;
import com.dangdang.ddframe.job.api.script.ScriptJob;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties.JobPropertiesEnum;
import com.dangdang.ddframe.job.lite.api.listener.AbstractDistributeOnceElasticJobListener;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.zen.elasticjob.spring.boot.annotation.ElasticJobConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringValueResolver;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 作业任务配置
 *
 * @author xinjingziranchan@gmail.com
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(ElasticJob.class)
@ConditionalOnBean(annotation = ElasticJobConfig.class)
@AutoConfigureAfter(RegistryCenterAutoConfiguration.class)
public class ElasticJobAutoConfiguration implements EmbeddedValueResolverAware {

    @Resource
    private ZookeeperRegistryCenter regCenter;

    @Autowired
    private ApplicationContext applicationContext;

    private StringValueResolver embeddedValueResolver;

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    @PostConstruct
    public void init() {
        //获取作业任务
        Map<String, ElasticJob> elasticJobMap = applicationContext.getBeansOfType(ElasticJob.class);
        //循环解析任务
        for (ElasticJob elasticJob : elasticJobMap.values()) {
            Class<? extends ElasticJob> jobClass = elasticJob.getClass();
            //获取作业任务注解配置
            ElasticJobConfig elasticJobConfig = jobClass.getAnnotation(ElasticJobConfig.class);
            //获取作业类型
            JobType jobType = getJobType(elasticJob);
            //对脚本类型做特殊处理，具体原因请查看：com.dangdang.ddframe.job.executor.JobExecutorFactory.getJobExecutor
            //当获取脚本作业执行器时，ElasticJob实例对象必须为空
            if (Objects.equals(JobType.SCRIPT, jobType)) {
                elasticJob = null;
            }
            //获取Lite作业配置
            LiteJobConfiguration liteJobConfiguration = getLiteJobConfiguration(jobType, jobClass, elasticJobConfig);
            //获取作业事件追踪的数据源配置
            JobEventRdbConfiguration jobEventRdbConfiguration = getJobEventRdbConfiguration(elasticJobConfig.eventTraceRdbDataSource());
            //获取作业监听器
            ElasticJobListener[] elasticJobListeners = creatElasticJobListeners(elasticJobConfig);
            elasticJobListeners = null == elasticJobListeners ? new ElasticJobListener[0] : elasticJobListeners;
            //注册作业
            if (null == jobEventRdbConfiguration) {
                new SpringJobScheduler(elasticJob, regCenter, liteJobConfiguration, elasticJobListeners).init();
            } else {
                new SpringJobScheduler(elasticJob, regCenter, liteJobConfiguration, jobEventRdbConfiguration, elasticJobListeners).init();
            }
        }
    }

    /**
     * 获取作业事件追踪的数据源配置
     *
     * @param eventTraceRdbDataSource 作业事件追踪的数据源Bean引用
     * @return JobEventRdbConfiguration
     */
    private JobEventRdbConfiguration getJobEventRdbConfiguration(String eventTraceRdbDataSource) {
        if (StringUtils.isBlank(eventTraceRdbDataSource)) {
            return null;
        }
        if (!applicationContext.containsBean(eventTraceRdbDataSource)) {
            throw new RuntimeException("not exist datasource [" + eventTraceRdbDataSource + "] !");
        }
        DataSource dataSource = (DataSource) applicationContext.getBean(eventTraceRdbDataSource);
        return new JobEventRdbConfiguration(dataSource);
    }

    /**
     * 获取作业任务类型
     *
     * @param elasticJob 作业任务
     * @return JobType
     */
    private JobType getJobType(ElasticJob elasticJob) {
        if (elasticJob instanceof SimpleJob) {
            return JobType.SIMPLE;
        } else if (elasticJob instanceof DataflowJob) {
            return JobType.DATAFLOW;
        } else if (elasticJob instanceof ScriptJob) {
            return JobType.SCRIPT;
        } else {
            throw new RuntimeException("unknown JobType [" + elasticJob.getClass() + "]!");
        }
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
        JobTypeConfiguration jobTypeConfiguration = getJobTypeConfiguration(jobCoreConfiguration, jobType, jobClass.getCanonicalName(),
                resolver2Boolean(elasticJobConfig.streamingProcess()), resolver(elasticJobConfig.scriptCommandLine()));

        //构建Lite作业
        return LiteJobConfiguration.newBuilder(Objects.requireNonNull(jobTypeConfiguration))
                .monitorExecution(resolver2Boolean(elasticJobConfig.monitorExecution()))
                .monitorPort(resolver2Int(elasticJobConfig.monitorPort()))
                .maxTimeDiffSeconds(resolver2Int(elasticJobConfig.maxTimeDiffSeconds()))
                .jobShardingStrategyClass(resolver(elasticJobConfig.jobShardingStrategyClass()))
                .reconcileIntervalMinutes(resolver2Int(elasticJobConfig.reconcileIntervalMinutes()))
                .disabled(resolver2Boolean(elasticJobConfig.disabled()))
                .overwrite(resolver2Boolean(elasticJobConfig.overwrite())).build();

    }

    /**
     * 构建任务核心配置
     *
     * @param jobName          任务执行名称
     * @param elasticJobConfig 任务配置
     * @return JobCoreConfiguration
     */
    private JobCoreConfiguration getJobCoreConfiguration(String jobName, ElasticJobConfig elasticJobConfig) {
        JobCoreConfiguration.Builder builder = JobCoreConfiguration.newBuilder(jobName, resolver(elasticJobConfig.cron()), resolver2Int(elasticJobConfig.shardingTotalCount()))
                .shardingItemParameters(resolver(elasticJobConfig.shardingItemParameters()))
                .jobParameter(resolver(elasticJobConfig.jobParameter()))
                .failover(resolver2Boolean(elasticJobConfig.failover()))
                .misfire(resolver2Boolean(elasticJobConfig.misfire()))
                .description(resolver(elasticJobConfig.description()));
        if (StringUtils.isNotBlank(elasticJobConfig.jobExceptionHandler())) {
            builder.jobProperties(JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), resolver(elasticJobConfig.jobExceptionHandler()));
        }
        if (StringUtils.isNotBlank(elasticJobConfig.executorServiceHandler())) {
            builder.jobProperties(JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), resolver(elasticJobConfig.executorServiceHandler()));
        }
        return builder.build();
    }

    /**
     * 获取任务类型配置
     *
     * @param jobCoreConfiguration 作业核心配置
     * @param jobType              作业类型
     * @param jobClass             作业类
     * @param streamingProcess     是否流式处理数据
     * @param scriptCommandLine    脚本型作业执行命令行
     * @return JobTypeConfiguration
     */
    private JobTypeConfiguration getJobTypeConfiguration(JobCoreConfiguration jobCoreConfiguration, JobType jobType,
                                                         String jobClass, boolean streamingProcess, String scriptCommandLine) {
        switch (jobType) {
            case DATAFLOW:
                return new DataflowJobConfiguration(jobCoreConfiguration, jobClass, streamingProcess);
            case SCRIPT:
                return new ScriptJobConfiguration(jobCoreConfiguration, scriptCommandLine);
            case SIMPLE:
            default:
                return new SimpleJobConfiguration(jobCoreConfiguration, jobClass);
        }
    }

    /**
     * 获取监听器
     *
     * @param elasticJobConfig 任务配置
     * @return ElasticJobListener[]
     */
    private ElasticJobListener[] creatElasticJobListeners(ElasticJobConfig elasticJobConfig) {
        List<ElasticJobListener> elasticJobListeners = new ArrayList<>(2);

        //注册每台作业节点均执行的监听
        ElasticJobListener elasticJobListener = registerElasticJobListener(resolver(elasticJobConfig.listenerClass()));
        if (null != elasticJobListener) {
            elasticJobListeners.add(elasticJobListener);
        }

        //注册分布式监听者
        AbstractDistributeOnceElasticJobListener distributedListener = registerAbstractDistributeOnceElasticJobListener(resolver(elasticJobConfig.distributedListenerClass()),
                resolver2Long(elasticJobConfig.startedTimeoutMilliseconds()), resolver2Long(elasticJobConfig.completedTimeoutMilliseconds()));
        if (null != distributedListener) {
            elasticJobListeners.add(distributedListener);
        }

        if (CollectionUtils.isEmpty(elasticJobListeners)) {
            return null;
        }

        //集合转数组
        ElasticJobListener[] elasticJobListenerArray = new ElasticJobListener[elasticJobListeners.size()];
        for (int i = 0; i < elasticJobListeners.size(); i++) {
            elasticJobListenerArray[i] = elasticJobListeners.get(i);
        }
        return elasticJobListenerArray;
    }

    /**
     * 创建每台作业节点均执行的监听
     *
     * @param listenerClass 监听者
     * @return ElasticJobListener
     */
    private ElasticJobListener registerElasticJobListener(String listenerClass) {

        //判断是否配置了监听者
        if (StringUtils.isBlank(listenerClass)) {
            return null;
        }

        //判断监听者是否已经在spring容器中存在
        if (applicationContext.containsBean(listenerClass)) {
            return applicationContext.getBean(listenerClass, ElasticJobListener.class);
        }

        //不存在则创建并注册到Spring容器中
        try {
            Class<? extends ElasticJobListener> listener = (Class<? extends ElasticJobListener>) Class.forName(listenerClass);
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(listener);
            beanDefinitionBuilder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            getDefaultListableBeanFactory().registerBeanDefinition(listenerClass, beanDefinitionBuilder.getBeanDefinition());
            return applicationContext.getBean(listenerClass, listener);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(MessageFormat.format("ClassNotFound: {0}!", listenerClass));
        } catch (Exception ex) {
            throw new RuntimeException(MessageFormat.format("{0} must extends ElasticJobListener!", listenerClass));
        }
    }

    /**
     * 创建分布式监听者到spring容器
     *
     * @param distributedListenerClass     监听者
     * @param startedTimeoutMilliseconds   最后一个作业执行前的执行方法的超时时间 单位：毫秒
     * @param completedTimeoutMilliseconds 最后一个作业执行后的执行方法的超时时间 单位：毫秒
     * @return AbstractDistributeOnceElasticJobListener
     */
    private AbstractDistributeOnceElasticJobListener registerAbstractDistributeOnceElasticJobListener(String distributedListenerClass,
                                                                                                      long startedTimeoutMilliseconds,
                                                                                                      long completedTimeoutMilliseconds) {
        ///判断是否配置了监听者
        if (StringUtils.isBlank(distributedListenerClass)) {
            return null;
        }

        //判断监听者是否已经在spring容器中存在
        if (applicationContext.containsBean(distributedListenerClass)) {
            return applicationContext.getBean(distributedListenerClass, AbstractDistributeOnceElasticJobListener.class);
        }

        //不存在则创建并注册到Spring容器中
        try {
            Class<? extends AbstractDistributeOnceElasticJobListener> distributedListener = (Class<? extends AbstractDistributeOnceElasticJobListener>) Class.forName(distributedListenerClass);
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(distributedListener);
            beanDefinitionBuilder.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            beanDefinitionBuilder.addConstructorArgValue(startedTimeoutMilliseconds);
            beanDefinitionBuilder.addConstructorArgValue(completedTimeoutMilliseconds);
            getDefaultListableBeanFactory().registerBeanDefinition(distributedListener.getSimpleName(), beanDefinitionBuilder.getBeanDefinition());
            return applicationContext.getBean(distributedListener.getSimpleName(), distributedListener);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(MessageFormat.format("ClassNotFound: {0}!", distributedListenerClass));
        } catch (Exception ex) {
            throw new RuntimeException(MessageFormat.format("{0} must extends ElasticJobListener!", distributedListenerClass));
        }
    }

    /**
     * 获取beanFactory
     *
     * @return DefaultListableBeanFactory
     */
    private DefaultListableBeanFactory getDefaultListableBeanFactory() {
        return (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    }

    /**
     * 解析配置值
     *
     * @param strVal 配置key
     * @return String
     */
    private String resolver(String strVal) {
        return embeddedValueResolver.resolveStringValue(strVal);
    }

    /**
     * 解析配置值
     *
     * @param strVal 配置key
     * @return int
     */
    private int resolver2Int(String strVal) {
        String resolverVal = resolver(strVal);
        try {
            return Integer.parseInt(resolverVal);
        } catch (NumberFormatException e) {
            throw new RuntimeException(MessageFormat.format("[{0}] resolver2Int [{1}] error: {2}!", strVal, resolverVal, e.getMessage()));
        }
    }

    /**
     * 解析配置值
     *
     * @param strVal 配置key
     * @return boolean
     */
    private boolean resolver2Boolean(String strVal) {
        return Boolean.parseBoolean(resolver(strVal));
    }

    /**
     * 解析配置值
     *
     * @param strVal 配置key
     * @return long
     */
    private long resolver2Long(String strVal) {
        String resolverVal = resolver(strVal);
        if (StringUtils.isBlank(resolverVal)) {
            return Long.MAX_VALUE;
        }
        try {
            return Long.parseLong(resolverVal);
        } catch (NumberFormatException e) {
            throw new RuntimeException(MessageFormat.format("[{0}] resolver2Long [{1}] error: {2}!", strVal, resolverVal, e.getMessage()));
        }
    }

}