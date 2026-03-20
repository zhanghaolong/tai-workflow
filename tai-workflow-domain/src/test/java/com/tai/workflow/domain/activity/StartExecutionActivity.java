package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StartExecutionActivity implements Activity {
    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws InterruptedException {
        String payLoad = context.getActivityPayload(String.class);
        return ActivityExecutionResult.ofSucceeded(Map.of(context.getActivityInstance().getName(), payLoad));
    }

    @Override
    public void rollback(WorkflowContext context) {
        System.out.println("rollback StartActivity");
    }
}
