package com.zen.elasticjob.spring.boot.jobinit;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.zen.elasticjob.spring.boot.ElasticJobProperties.SimpleConfiguration;

import java.util.Map;

/**
 * 简单任务初始
 *
 * @author xinjingziranchan@gmail.com
 * @version 2.0.0
 * @since 2.0.0
 */
public class SimpleJobInitialization extends AbstractJobInitialization {

    private Map<String, SimpleConfiguration> simpleConfigurationMap;

    public SimpleJobInitialization(final Map<String, SimpleConfiguration> simpleConfigurationMap) {
        this.simpleConfigurationMap = simpleConfigurationMap;
    }

    public void init() {
        for (String jobName : simpleConfigurationMap.keySet()) {
            SimpleConfiguration configuration = simpleConfigurationMap.get(jobName);
            initJob(jobName, configuration.getJobType(), configuration);
        }
    }

    @Override
    public JobTypeConfiguration getJobTypeConfiguration(String jobName, JobCoreConfiguration jobCoreConfiguration) {
        SimpleConfiguration configuration = simpleConfigurationMap.get(jobName);
        return new SimpleJobConfiguration(jobCoreConfiguration, configuration.getJobClass());
    }
}
