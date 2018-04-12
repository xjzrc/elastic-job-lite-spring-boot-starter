package com.zen.elasticjob.spring.boot.jobinit;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.zen.elasticjob.spring.boot.ElasticJobProperties.ScriptConfiguration;

import java.util.Map;

/**
 * 脚本任务初始
 *
 * @author xinjingziranchan@gmail.com
 * @version 2.0.0
 * @since 2.0.0
 */
public class ScriptJobInitialization extends AbstractJobInitialization {

    private Map<String, ScriptConfiguration> scriptConfigurationMap;

    public ScriptJobInitialization(final Map<String, ScriptConfiguration> scriptConfigurationMap) {
        this.scriptConfigurationMap = scriptConfigurationMap;
    }

    public void init() {
        for (String jobName : scriptConfigurationMap.keySet()) {
            ScriptConfiguration configuration = scriptConfigurationMap.get(jobName);
            initJob(jobName, configuration.getJobType(), configuration);
        }
    }

    @Override
    public JobTypeConfiguration getJobTypeConfiguration(String jobName, JobCoreConfiguration jobCoreConfiguration) {
        ScriptConfiguration configuration = scriptConfigurationMap.get(jobName);
        return new ScriptJobConfiguration(jobCoreConfiguration, configuration.getScriptCommandLine());
    }
}
