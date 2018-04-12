package com.zen.elasticjob.spring.boot.jobinit;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.zen.elasticjob.spring.boot.ElasticJobProperties.DataflowConfiguration;

import java.util.Map;

/**
 * 流式任务初始
 *
 * @author xinjingziranchan@gmail.com
 * @version 2.0.0
 * @since 2.0.0
 */
public class DataflowJobInitialization extends AbstractJobInitialization {

    private Map<String, DataflowConfiguration> dataflowConfigurationMap;

    public DataflowJobInitialization(final Map<String, DataflowConfiguration> dataflowConfigurationMap) {
        this.dataflowConfigurationMap = dataflowConfigurationMap;
    }

    public void init() {
        for (String jobName : dataflowConfigurationMap.keySet()) {
            DataflowConfiguration configuration = dataflowConfigurationMap.get(jobName);
            initJob(jobName, configuration.getJobType(), configuration);
        }
    }

    @Override
    public JobTypeConfiguration getJobTypeConfiguration(String jobName, JobCoreConfiguration jobCoreConfiguration) {
        DataflowConfiguration configuration = dataflowConfigurationMap.get(jobName);
        return new DataflowJobConfiguration(jobCoreConfiguration, configuration.getJobClass(), configuration.isStreamingProcess());
    }
}
