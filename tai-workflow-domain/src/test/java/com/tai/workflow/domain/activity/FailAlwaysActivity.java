package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FailAlwaysActivity implements Activity {
    private static int count = 0;

    @Autowired
    private WorkflowDriver workflowDriver;

    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) {
        context.setContextParam("object", Object.class);

        // If "materializeBeforeFail" is set, persist the context before throwing
        Boolean materialize = context.getContextParam("materializeBeforeFail", Boolean.class);
        if (Boolean.TRUE.equals(materialize)) {
            workflowDriver.materialize(context);
        }

        throw new IllegalArgumentException("test exception %s".formatted(count++));
    }

    @Override
    public void rollback(WorkflowContext context) {
        System.out.println("rollback FailAlwaysActivity");
    }
}
