package com.tai.workflow.api;

import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;

public interface Activity {
    ActivityExecutionResult invoke(WorkflowContext context) throws Exception;

    default void rollback(WorkflowContext context) {
    }
}
