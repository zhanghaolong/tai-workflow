package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.enums.SignalAction;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Activity that signals itself during execution.
 * Used for testing signal-within-activity scenarios.
 */
@Component
public class SignalSelfActivity implements Activity {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws InterruptedException {
        Long workflowInstanceId = context.getWorkflowInstance().getId();
        String signalBizCode = context.getActivityDefinition().getSignalBizCode();

        // Signal self before activity completes
        workflowDriver.signalWorkflowInstance(workflowInstanceId, signalBizCode, SignalAction.SUCCESS);

        return ActivityExecutionResult.ofSucceeded();
    }

    @Override
    public void rollback(WorkflowContext context) {
        // no-op
    }
}
