package com.tai.workflow.domain.handler;

import com.tai.workflow.model.WorkflowInstance;
import lombok.Getter;

/**
 * @author zhanghaolong1989@163.com
 */
@Getter
public abstract class WorkflowInstanceEvent extends AbstractWorkflowEvent {
    protected WorkflowInstance workflowInstance;

    public WorkflowInstanceEvent(Object source) {
        super(source);
    }
}
