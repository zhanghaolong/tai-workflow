package com.tai.workflow.graph.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import org.springframework.stereotype.Component;

@Component
public class NullableActivity implements Activity {
    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) {
        return null;
    }
}
