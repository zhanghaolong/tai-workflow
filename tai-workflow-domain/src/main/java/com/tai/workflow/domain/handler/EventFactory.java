package com.tai.workflow.domain.handler;

import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowContext;
import com.tai.workflow.model.WorkflowInstance;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;

@Slf4j
public class EventFactory {
    public static AbstractWorkflowEvent createWorkflowInstanceEvent(final WorkflowInstance workflowInstance,
            final Class<? extends AbstractWorkflowEvent> clazz) {
        try {
            Constructor<? extends AbstractWorkflowEvent> constructor = clazz.getConstructor(WorkflowInstance.class);
            return constructor.newInstance(workflowInstance);
        } catch (Exception e) {
            log.error("createWorkflowInstanceEvent met with error", e);
            return null;
        }
    }

    public static AbstractWorkflowEvent createActivityInstanceEvent(final ActivityInstance activityInstance,
            final Class<? extends AbstractWorkflowEvent> clazz) {
        try {
            Constructor<? extends AbstractWorkflowEvent> constructor = clazz.getConstructor(ActivityInstance.class);
            return constructor.newInstance(activityInstance);
        } catch (Exception e) {
            log.error("createActivityInstanceEvent met with error:", e);
            return null;
        }
    }

    public static ActivityInstanceCompletedEvent createActivityCompletedEvent(WorkflowContext context,
            ActivityExecutionResult activityExecutionResult) {
        return new ActivityInstanceCompletedEvent(context, activityExecutionResult);
    }

    public static ActivityInstanceFailedEvent createActivityFailedEvent(WorkflowContext context, Throwable throwable) {
        return new ActivityInstanceFailedEvent(context, throwable);
    }

    public static ActivityInstanceFailedEvent createActivityFailedEvent(WorkflowContext context, String errorMessage) {
        return new ActivityInstanceFailedEvent(context, errorMessage);
    }
}
