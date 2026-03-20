package com.tai.workflow.domain.util;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowInstance;

import java.util.Comparator;
import java.util.List;

public class WorkflowTestUtils {
    public static void printActivityDetail(WorkflowDriver workflowDriver, Long workflowInstanceId) {
        StringBuilder sb = new StringBuilder();
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        sb.append("workflowInstance:")
                .append(workflowInstance.getId())
                .append("|name:")
                .append(workflowInstance.getName())
                .append("|state:")
                .append(workflowInstance.getState());
        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        activityInstances = activityInstances.stream().sorted(Comparator.comparing(ActivityInstance::getId)).toList();
        for (int i = 0; i < activityInstances.size(); i++) {
            sb.append(System.lineSeparator())
                    .append("activityInstance" + (i + 1) + ":" + activityInstances.get(i).getName() + "|displayName:" + activityInstances.get(i)
                            .getDisplayName() + "|state:" + activityInstances.get(i).getState() + "|signal:" + activityInstances.get(i)
                                    .getSignalBizCode() + "|executionMsg:" + activityInstances.get(i).getExecutionMsg()
                            + "|retryCount:" + activityInstances.get(i).getRetryCount());
        }

        System.out.println(sb);
    }
}
