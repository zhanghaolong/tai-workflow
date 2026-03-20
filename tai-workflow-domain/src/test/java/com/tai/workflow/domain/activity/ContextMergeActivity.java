package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Activity that produces output context for testing context merge scenarios.
 */
@Component
public class ContextMergeActivity implements Activity {

    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws InterruptedException {
        Thread.sleep(100L);

        String activityName = context.getActivityInstance().getName();
        return ActivityExecutionResult.ofSucceeded(
                Map.of(
                        activityName + "_key1", activityName + "_value1",
                        activityName + "_key2", activityName + "_value2",
                        "shared_key", activityName + "_shared_value"
                )
        );
    }

    @Override
    public void rollback(WorkflowContext context) {
        System.out.println("rollback ContextMergeActivity: " + context.getActivityInstance().getName());
    }
}
