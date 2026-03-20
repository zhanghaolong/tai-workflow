package com.tai.workflow.domain.factory;

import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.graph.WorkflowDagNode;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowInstance;

import java.util.Date;
import java.util.HashMap;

/**
 * @author zhanghaolong1989@163.com
 */
public class ActivityInstanceFactory {
    public static ActivityInstance generateActivityInstance(String namespace, WorkflowDagNode workflowDagNode, WorkflowInstance workflowInstance) {
        ActivityDefinition activityDefinition = workflowDagNode.getActivityDefinition();
        ActivityInstance activityInstance = new ActivityInstance();
        activityInstance.setWorkflowInstanceId(workflowInstance.getId());
        activityInstance.setNamespace(namespace);
        activityInstance.setName(activityDefinition.getName());
        activityInstance.setDisplayName(activityDefinition.getDisplayName());
        activityInstance.setRetryCount(0);
        activityInstance.setState(ActivityState.PENDING);
        activityInstance.setWorkflowInstanceId(workflowInstance.getId());
        activityInstance.setInputContext(new HashMap<>());
        activityInstance.setOutputContext(new HashMap<>());
        activityInstance.setSignalBizCode(activityDefinition.getSignalBizCode());
        activityInstance.setCreateTime(new Date());
        return activityInstance;
    }
}
