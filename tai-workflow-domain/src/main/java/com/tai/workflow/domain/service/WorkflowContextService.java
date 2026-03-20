package com.tai.workflow.domain.service;

import com.tai.workflow.graph.WorkflowDag;
import com.tai.workflow.graph.WorkflowDagNode;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowContext;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author zhanghaolong1989@163.com
 */
@Service
@RequiredArgsConstructor
public class WorkflowContextService {
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowInstanceService workflowInstanceService;

    public WorkflowContext generateExecutionContext(WorkflowInstance workflowInstance, ActivityInstance activityInstance) {
        return generateExecutionContext(workflowInstance, activityInstance, null);
    }

    public WorkflowContext generateExecutionContext(WorkflowInstance workflowInstance, ActivityInstance activityInstance, Boolean resetRetryCount) {
        WorkflowContext workflowContext = new WorkflowContext();
        workflowContext.setWorkflowInstance(workflowInstance);
        workflowContext.setActivityInstance(activityInstance);
        WorkflowDefinitionInternal workflowDefinitionInternal = workflowDefinitionService.findWorkflowDefinition(
                workflowInstance.getWorkflowDefinitionId());
        workflowContext.setWorkflowDefinitionInternal(workflowDefinitionInternal);
        WorkflowDag dag = workflowDefinitionInternal.getWorkflowDag();
        WorkflowDagNode node = dag.getNode(activityInstance.getName());
        workflowContext.setActivityDefinition(node.getActivityDefinition());
        workflowContext.setActivityInstance(activityInstance);
        workflowContext.mergeContext(workflowInstance.getContextParams());
        workflowContext.setResetRetryCount(resetRetryCount);
        return workflowContext;
    }
}
