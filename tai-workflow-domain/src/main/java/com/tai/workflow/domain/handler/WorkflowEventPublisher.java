package com.tai.workflow.domain.handler;

import com.tai.workflow.model.WorkflowInstance;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Component
public class WorkflowEventPublisher implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    public WorkflowEventPublisher() {
    }

    public static void publishEvent(AbstractWorkflowEvent workflowEvent) {
        if (Objects.nonNull(workflowEvent)) {
            applicationContext.publishEvent(workflowEvent);
        }
    }

    public static void publishEvent(WorkflowInstance workflowInstance) {
        if (Objects.nonNull(workflowInstance)) {
            applicationContext.publishEvent(workflowInstance);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        WorkflowEventPublisher.applicationContext = applicationContext;
    }
}
