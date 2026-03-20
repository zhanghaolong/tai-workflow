package com.tai.workflow.domain.handler;

import org.springframework.context.ApplicationEvent;

/**
 * @author zhanghaolong1989@163.com
 */
public abstract class AbstractWorkflowEvent extends ApplicationEvent {
    public AbstractWorkflowEvent(Object source) {
        super(source);
    }
}
