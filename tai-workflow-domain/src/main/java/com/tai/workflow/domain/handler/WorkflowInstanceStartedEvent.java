package com.tai.workflow.domain.handler;

import com.tai.workflow.model.WorkflowInstance;
import lombok.Getter;

/**
 * @author zhanghaolong1989@163.com
 */
@Getter
public class WorkflowInstanceStartedEvent extends WorkflowInstanceEvent {
    public WorkflowInstanceStartedEvent(WorkflowInstance workflowInstance) {
        super(workflowInstance);
        this.workflowInstance = workflowInstance;
    }
}
