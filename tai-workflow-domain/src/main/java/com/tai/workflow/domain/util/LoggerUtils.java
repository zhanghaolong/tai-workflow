package com.tai.workflow.domain.util;

import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowContext;
import com.tai.workflow.model.WorkflowInstance;

/**
 * @author zhanghaolong1989@163.com
 */
public class LoggerUtils {
    private static final String COLON = ":";
    private static final String SEPARATOR = ",";

    public static String getTraceHeader(WorkflowContext context) {
        return getTraceHeader(context.getWorkflowInstance(), context.getActivityInstance());
    }

    public static String getTraceHeader(WorkflowInstance workflowInstance, ActivityInstance activityInstance) {
        StringBuilder sb = new StringBuilder();
        return sb.append(WorkflowConstants.WORKFLOW_INSTANCE_ID + COLON)
                .append(workflowInstance.getId())
                .append(SEPARATOR + WorkflowConstants.STATUS + COLON)
                .append(workflowInstance.getState())
                .append(SEPARATOR + WorkflowConstants.ACTIVITY_INSTANCE_ID + COLON)
                .append(activityInstance.getId())
                .append(SEPARATOR + WorkflowConstants.ACTIVITY_INSTANCE_NAME + COLON)
                .append(activityInstance.getName())
                .append(SEPARATOR + WorkflowConstants.STATUS + COLON)
                .append(activityInstance.getState())
                .toString();
    }
}
