package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Activity that simulates slow execution for timeout testing.
 */
@Component
public class SlowActivity implements Activity {

    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws InterruptedException {
        // Simulate slow operation (5 seconds)
        Thread.sleep(5000L);
        return ActivityExecutionResult.ofSucceeded(
                Map.of("SlowActivity", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
    }

    @Override
    public void rollback(WorkflowContext context) {
        System.out.println("rollback SlowActivity");
    }
}
