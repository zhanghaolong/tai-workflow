package com.tai.workflow.domain;

import com.tai.workflow.domain.handler.AbstractWorkflowEvent;
import com.tai.workflow.domain.handler.WorkflowInstanceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author zhanghaolong1989@163.com keep this for testing multicast
 */
@Slf4j
@Component
public class WorkflowEventListener implements ApplicationListener<AbstractWorkflowEvent> {
    @Override
    public void onApplicationEvent(AbstractWorkflowEvent event) {
        if (event instanceof WorkflowInstanceEvent workflowInstanceEvent) {
            log.info("WorkflowEventListener has received event:{}", workflowInstanceEvent.getClass().getSimpleName());
        }
    }
}
