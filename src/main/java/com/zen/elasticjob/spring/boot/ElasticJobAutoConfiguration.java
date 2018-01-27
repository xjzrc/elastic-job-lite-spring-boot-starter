package com.zen.elasticjob.spring.boot;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.api.JobConfiguration;
import com.dangdang.ddframe.job.spring.schedule.SpringJobScheduler;
import com.dangdang.ddframe.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.reg.zookeeper.ZookeeperRegistryCenter;
import com.zen.elasticjob.spring.boot.annotation.SimpleJobConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;


@Configuration
@ConditionalOnClass(ElasticJobAutoConfiguration.class)
@Slf4j
public class ElasticJobAutoConfiguration implements BeanPostProcessor, EnvironmentAware,ApplicationContextAware {

    private ZookeeperRegistryCenter regCenter;

    private ConfigurableEnvironment environment;

    private ApplicationContext applicationContext;

    private final static String PLACEHOLDER_PREFIX = "${";

    private final static String PLACEHOLDER_SUFFIX = "}";

    private final static String ZOOKEEPER_SUFFIX = "spring.elastic.job.zookeeper";

    private final static String NAMESPACE = "spring.elastic.job.namespace";


    @Bean("zkConfig")
    public ZookeeperConfiguration zkConfig() {
        String zookeeper = environment.getProperty(ZOOKEEPER_SUFFIX);
        String namespace = environment.getProperty(NAMESPACE);
        if (StringUtils.isEmpty(zookeeper) || StringUtils.isEmpty(namespace)) {
            log.error("spring.elastic.job.zookeeper or spring.elastic.job.namespace is null, please set the value in application.properties. (spring.elastic.job.namespace)(spring.elastic.job.zookeeper)");
            throw new RuntimeException("spring.elastic.job.zookeeper or spring.elastic.job.namespace is null, please set the value in application.properties. (spring.elastic.job.namespace)(spring.elastic.job.zookeeper)");
        }
        return new ZookeeperConfiguration(zookeeper, namespace);
    }

    @Bean(initMethod = "init")
    public ZookeeperRegistryCenter regCenter(@Qualifier("zkConfig") ZookeeperConfiguration config) {
        ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter(config);
        return zookeeperRegistryCenter;
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ElasticJob) {
            Annotation[] beanAnnotation = bean.getClass().getAnnotations();
            for (Annotation annotation : beanAnnotation) {
                if (annotation instanceof SimpleJobConfig) {
                    SimpleJobConfig simpleJobConfig = (SimpleJobConfig) annotation;

                    //required argument
                    String cron = parseStringValue(simpleJobConfig.cron());
                    int shardingTotalCount = Integer.parseInt(parseStringValue(String.valueOf(simpleJobConfig.shardingTotalCount())));

                    //not required argument
                    String jobParameter = parseStringValue(simpleJobConfig.jobParameter());
                    String description = parseStringValue(simpleJobConfig.description());
                    String shardingItemParameters = parseStringValue(simpleJobConfig.shardingItemParameters());
                    boolean overwrite = Boolean.valueOf(parseStringValue(String.valueOf(simpleJobConfig.overwrite())));

                    ElasticJob job = (ElasticJob) bean;
                    JobConfiguration jobConfiguration = new JobConfiguration(job.getClass().getName(), job.getClass(), shardingTotalCount, cron);

                    jobConfiguration.setJobParameter(jobParameter);
                    jobConfiguration.setDescription(description);
                    jobConfiguration.setOverwrite(overwrite);
                    jobConfiguration.setShardingItemParameters(shardingItemParameters);
                    if (regCenter == null) {
                        regCenter = regCenter(zkConfig());
                    }
                    SpringJobScheduler jobScheduler = new SpringJobScheduler(regCenter, jobConfiguration);
                    jobScheduler.setApplicationContext(applicationContext);
                    jobScheduler.init();
                }
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 如果采用通配符形式，则替换通配符
     *
     * @param key
     * @return
     */
    public String parseStringValue(String key) {
        if (key != null && !"".equals(key)) {
            int startIndex = key.indexOf(PLACEHOLDER_PREFIX);
            if (startIndex != -1) {
                int endIndex = key.indexOf(PLACEHOLDER_SUFFIX, startIndex + PLACEHOLDER_PREFIX.length());
                if (endIndex != -1) {
                    String parseKey = key.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
                    return environment.getProperty(parseKey);
                }
            }
        }
        return key;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }


}