package com.tai.workflow.domain.handler;

import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowContext;
import lombok.Getter;

/**
 * @author zhanghaolong1989@163.com
 */
@Getter
public class ActivityInstanceFailedEvent extends AbstractWorkflowEvent implements ActivityInstanceProvider {
    private final WorkflowContext workflowContext;
    private Throwable throwable;
    private String errorMessage;

    public ActivityInstanceFailedEvent(WorkflowContext workflowContext, Throwable throwable) {
        super(workflowContext);
        this.workflowContext = workflowContext;
        this.throwable = throwable;
    }

    public ActivityInstanceFailedEvent(WorkflowContext workflowContext, String errorMessage) {
        super(workflowContext);
        this.workflowContext = workflowContext;
        this.errorMessage = errorMessage;
    }

    public ActivityInstance provide() {
        return this.workflowContext.getActivityInstance();
    }
}
