package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class FailOnceActivityForResume implements Activity {
    public static Integer count = 0;

    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws InterruptedException {
        if (count < 1) {
            count++;
            throw new IllegalArgumentException("test exception");
        }

        Thread.sleep(1_000L);
        return ActivityExecutionResult.ofSucceeded(
                Map.of(FailOnceActivityForResume.class.getSimpleName(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
    }

    @Override
    public void rollback(WorkflowContext context) {
        System.out.println("rollback FailOnceActivity");
    }
}
