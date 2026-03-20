package com.tai.workflow.graph;

import com.tai.workflow.model.ActivityDefinition;

/**
 * @author zhanghaolong1989@163.com
 */
public class WorkflowDagNodeFactory {
    public static WorkflowDagNode createWorkflowDagNode(ActivityDefinition activityDefinition) {
        WorkflowDagNode workflowDagNode = new WorkflowDagNode();
        workflowDagNode.setName(activityDefinition.getName());
        workflowDagNode.setActivityDefinition(activityDefinition);
        return workflowDagNode;
    }
}
