package com.tai.workflow.repository.persistence;

import com.tai.workflow.repository.entity.WorkflowDefinitionEntity;

/**
 * @author zhanghaolong1989@163.com
 */
public interface WorkflowDefinitionPersistence {

    WorkflowDefinitionEntity findByWorkflowDefinitionId(Long workflowDefinitionId);

    Long save(WorkflowDefinitionEntity workflowDefinitionEntity);

    WorkflowDefinitionEntity findByNameSpaceAndNameAndWorkerNode(String namespace, String workflowDefinitionName, String workerNode);
}
