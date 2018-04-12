package com.zen.elasticjob.spring.boot;

import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.zen.elasticjob.spring.boot.ElasticJobProperties.ZkConfiguration;
import com.zen.elasticjob.spring.boot.jobinit.DataflowJobInitialization;
import com.zen.elasticjob.spring.boot.jobinit.ScriptJobInitialization;
import com.zen.elasticjob.spring.boot.jobinit.SimpleJobInitialization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 作业任务配置
 *
 * @author xinjingziranchan@gmail.com
 * @version 2.0.0
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(ElasticJobProperties.class)
public class ElasticJobAutoConfiguration {

    /**
     * 默认注册中心
     */
    public static final String DEFAULT_REGISTRY_CENTER_NAME = "elasticJobRegistryCenter";

    @Autowired
    private ElasticJobProperties elasticJobProperties;

    @Bean(name = DEFAULT_REGISTRY_CENTER_NAME, initMethod = "init")
    @ConditionalOnMissingBean
    public ZookeeperRegistryCenter elasticJobRegistryCenter() {
        ZkConfiguration regCenterProperties = elasticJobProperties.getZookeeper();
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration(regCenterProperties.getServerLists(), regCenterProperties.getNamespace());
        zookeeperConfiguration.setBaseSleepTimeMilliseconds(regCenterProperties.getBaseSleepTimeMilliseconds());
        zookeeperConfiguration.setConnectionTimeoutMilliseconds(regCenterProperties.getConnectionTimeoutMilliseconds());
        zookeeperConfiguration.setMaxSleepTimeMilliseconds(regCenterProperties.getMaxSleepTimeMilliseconds());
        zookeeperConfiguration.setSessionTimeoutMilliseconds(regCenterProperties.getSessionTimeoutMilliseconds());
        zookeeperConfiguration.setMaxRetries(regCenterProperties.getMaxRetries());
        zookeeperConfiguration.setDigest(regCenterProperties.getDigest());
        return new ZookeeperRegistryCenter(zookeeperConfiguration);
    }

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    @ConditionalOnBean(ZookeeperRegistryCenter.class)
    public SimpleJobInitialization simpleJobInitialization() {
        return new SimpleJobInitialization(elasticJobProperties.getSimples());
    }

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    @ConditionalOnBean(ZookeeperRegistryCenter.class)
    public DataflowJobInitialization dataflowJobInitialization() {
        return new DataflowJobInitialization(elasticJobProperties.getDataflows());
    }

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    @ConditionalOnBean(ZookeeperRegistryCenter.class)
    public ScriptJobInitialization scriptJobInitialization() {
        return new ScriptJobInitialization(elasticJobProperties.getScripts());
    }
}