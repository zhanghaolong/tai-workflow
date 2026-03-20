package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class UnifyActivity implements Activity {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws InterruptedException {
        // Default invoke implementation
        Thread.sleep(100L);
        return ActivityExecutionResult.ofSucceeded(Map.of(context.getActivityInstance().getName(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
    }

    @Override
    public void rollback(WorkflowContext context) {
        System.out.println("rollback UnifyActivity");
    }
}
