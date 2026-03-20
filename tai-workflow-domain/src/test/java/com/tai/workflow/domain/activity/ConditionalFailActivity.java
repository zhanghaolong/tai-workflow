package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity that fails based on configurable conditions.
 */
@Component
public class ConditionalFailActivity implements Activity {
    private static final AtomicInteger executionCount = new AtomicInteger(0);

    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws InterruptedException {
        Thread.sleep(100L);

        int count = executionCount.incrementAndGet();
        Integer failAfterCount = context.getContextParam("failAfterCount", Integer.class);

        if (failAfterCount != null && count >= failAfterCount) {
            throw new IllegalStateException("ConditionalFailActivity failed at execution count: " + count);
        }

        return ActivityExecutionResult.ofSucceeded(
                Map.of("ConditionalFailActivity", "execution_" + count));
    }

    @Override
    public void rollback(WorkflowContext context) {
        System.out.println("rollback ConditionalFailActivity");
    }

    public static void resetCount() {
        executionCount.set(0);
    }
}
