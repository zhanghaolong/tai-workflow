package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Activity that records the current thread name in the workflow context.
 * Used for testing thread behavior in workflow execution.
 */
@Component
public class SaveThreadActivity implements Activity {
    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws InterruptedException {
        return ActivityExecutionResult.ofSucceeded(
                Map.of("currentThread_" + context.getActivityInstance().getName(), Thread.currentThread().getName()));
    }

    @Override
    public void rollback(WorkflowContext context) {
        // no-op
    }
}
