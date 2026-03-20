package com.tai.workflow.domain.handler;

import com.tai.workflow.model.WorkflowInstance;
import lombok.Getter;

/**
 * @author zhanghaolong1989@163.com
 */
@Getter
public class WorkflowInstanceTerminatedEvent extends AbstractWorkflowEvent {
    private final WorkflowInstance workflowInstance;

    public WorkflowInstanceTerminatedEvent(WorkflowInstance workflowInstance) {
        super(workflowInstance);
        this.workflowInstance = workflowInstance;
    }
}
