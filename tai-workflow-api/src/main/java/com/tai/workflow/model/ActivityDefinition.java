package com.tai.workflow.model;

import com.tai.workflow.api.Activity;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.utils.CheckUtils;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
@Builder
@NoArgsConstructor
public class ActivityDefinition {
    private static final int INVOKE_SPLIT_COUNT = 2;
    private static final int DEFAULT_MAX_RETRY = 0;
    private static final long DEFAULT_RETRY_INTERVAL_MILLS = 5000L;
    private static final long DEFAULT_TIMEOUT_MILLS = 0L;
    private String name;
    private String displayName;
    private Class<? extends Activity> activityClass;
    private ActivityFailStrategy activityFailStrategy;
    private int maxRetry;
    private long retryIntervalMillis;
    private long timeoutMillis;
    private String signalBizCode;
    private Object payload;
    private boolean isolate;

    ActivityDefinition(String name, String displayName, Class<? extends Activity> activityClass, ActivityFailStrategy activityFailStrategy,
                       int maxRetry, long retryIntervalMillis, long timeoutMillis, String signalBizCode, Object payload, boolean isolate) {
        CheckUtils.checkNotEmpty(name, WorkflowConstants.INVALID_PARAMETER, "name should not be empty!");
        CheckUtils.checkNotNull(activityClass, WorkflowConstants.INVALID_PARAMETER, "activityClass should not be empty!");
        this.name = name;
        this.displayName = displayName;
        this.activityClass = activityClass;
        this.activityFailStrategy = activityFailStrategy;
        this.maxRetry = maxRetry;
        this.retryIntervalMillis = retryIntervalMillis;
        this.timeoutMillis = timeoutMillis;
        this.signalBizCode = signalBizCode;
        this.payload = payload;
        this.isolate = isolate;
    }
}
