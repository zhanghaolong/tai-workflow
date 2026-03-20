package com.tai.workflow.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author shenwangyan
 */
@Configuration
@ConfigurationProperties(prefix = "web3.workflow", ignoreInvalidFields = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowConfiguration {
    private String namespace = "default";
    private int activityRunningPoolCoreSize = 100;
    private int activityRunningPoolMaxSize = 300;
    private int keepAliveMinutes = 10;
    private int activityRunningPoolQueueSize = 1024;
    private int scheduleRunningPoolCoreSize = 50;
    private long failOverCheckTimeSecRange = 43200L;
    private int heartbeatTimeoutMinutes = 1;
    private long failOverScheduleTaskSeconds = 20;
    private int workflowSubmitRootNodePoolCoreSize = 20;
    private int workflowSubmitRootNodePoolMaxSize = 100;
    private int workflowSubmitRootNodeKeepAliveSeconds = 30;
    private int workflowSubmitRootNodePoolQueueSize = 50;
}
