package com.tai.workflow.domain.handler;

import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowContext;
import lombok.Getter;

/**
 * @author zhanghaolong1989@163.com
 */
@Getter
public class ActivityInstanceCompletedEvent extends AbstractWorkflowEvent implements ActivityInstanceProvider {
    private final ActivityExecutionResult activityExecutionResult;

    private final WorkflowContext workflowContext;

    public ActivityInstanceCompletedEvent(WorkflowContext workflowContext, ActivityExecutionResult activityExecutionResult) {
        super(workflowContext);
        this.workflowContext = workflowContext;
        this.activityExecutionResult = activityExecutionResult;
    }

    public ActivityInstance provide() {
        return this.workflowContext.getActivityInstance();
    }
}
