package com.zen.elasticjob.spring.boot;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.zen.elasticjob.spring.boot.annotation.ElasticJobConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册中心
 *
 * @author xinjingziranchan@gmail.com
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(ElasticJob.class)
@ConditionalOnBean(annotation = ElasticJobConfig.class)
@EnableConfigurationProperties(ZookeeperRegistryProperties.class)
public class RegistryCenterAutoConfiguration {

    @Autowired
    private ZookeeperRegistryProperties regCenterProperties;

    @Bean(initMethod = "init")
    @ConditionalOnMissingBean
    public ZookeeperRegistryCenter regCenter() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration(regCenterProperties.getServerLists(), regCenterProperties.getNamespace());
        zookeeperConfiguration.setBaseSleepTimeMilliseconds(regCenterProperties.getBaseSleepTimeMilliseconds());
        zookeeperConfiguration.setConnectionTimeoutMilliseconds(regCenterProperties.getConnectionTimeoutMilliseconds());
        zookeeperConfiguration.setMaxSleepTimeMilliseconds(regCenterProperties.getMaxSleepTimeMilliseconds());
        zookeeperConfiguration.setSessionTimeoutMilliseconds(regCenterProperties.getSessionTimeoutMilliseconds());
        zookeeperConfiguration.setMaxRetries(regCenterProperties.getMaxRetries());
        zookeeperConfiguration.setDigest(regCenterProperties.getDigest());
        return new ZookeeperRegistryCenter(zookeeperConfiguration);
    }

}
