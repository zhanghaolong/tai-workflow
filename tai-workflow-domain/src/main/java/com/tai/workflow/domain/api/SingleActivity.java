package com.tai.workflow.domain.api;

import com.tai.workflow.api.Activity;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import lombok.AllArgsConstructor;

//SingleActivity: 指的是只执行当前 Activity 但是不会驱动 DAG 图继续执行
@AllArgsConstructor
public final class SingleActivity implements Activity {
    private final Activity activity;

    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws Exception {
        return activity.invoke(context);
    }

    @Override
    public void rollback(WorkflowContext context) {
        activity.rollback(context);
    }
}
