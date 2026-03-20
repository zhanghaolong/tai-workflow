package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class SleepActivity implements Activity {
    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws Exception {
        Thread.sleep(3_000);
        return ActivityExecutionResult.ofSucceeded(
                Map.of(SleepActivity.class.getSimpleName(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
    }
}
