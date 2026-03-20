package com.tai.workflow.domain.service;

import com.tai.workflow.utils.CheckUtils;
import com.tai.workflow.configuration.WorkflowConfiguration;
import com.tai.workflow.domain.convert.WorkflowConvert;
import com.tai.workflow.domain.util.WorkflowUtils;
import com.tai.workflow.graph.WorkflowDag;
import com.tai.workflow.graph.WorkflowDagNode;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import com.tai.workflow.repository.entity.WorkflowDefinitionEntity;
import com.tai.workflow.repository.persistence.WorkflowDefinitionPersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class WorkflowDefinitionService {
    private final WorkflowDefinitionPersistence workflowDefinitionPersistence;
    private final WorkflowConfiguration workflowConfiguration;
    private final WorkflowConvert workflowConvert;

    public Long registerWorkflowDefinition(WorkflowDefinitionInternal workflowDefinitionInternal) {
        WorkflowDefinitionEntity workflowDefinitionEntity = workflowConvert.convert(workflowDefinitionInternal);
        workflowDefinitionEntity.setNamespace(workflowConfiguration.getNamespace());
        workflowDefinitionEntity.setWorkerNode(WorkflowUtils.getUniqueIdentity(workflowConfiguration.getNamespace()));
        workflowDefinitionEntity.setCreateTime(new Date());
        workflowDefinitionEntity.setUpdateTime(new Date());
        return workflowDefinitionPersistence.save(workflowDefinitionEntity);
    }

    public WorkflowDefinitionInternal findWorkflowDefinition(WorkflowInstance workflowInstance) {
        return findWorkflowDefinition(workflowInstance.getWorkflowDefinitionId());
    }

    public WorkflowDefinitionInternal findWorkflowDefinition(String namespace, String workflowDefinitionName, String workerNode) {
        WorkflowDefinitionEntity workflowDefinitionEntity = workflowDefinitionPersistence.findByNameSpaceAndNameAndWorkerNode(namespace,
                workflowDefinitionName, workerNode);
        return workflowConvert.convert(workflowDefinitionEntity);
    }

    public WorkflowDefinitionInternal findWorkflowDefinition(Long workflowDefinitionId) {
        WorkflowDefinitionEntity workflowDefinitionEntity = workflowDefinitionPersistence.findByWorkflowDefinitionId(workflowDefinitionId);
        return workflowConvert.convert(workflowDefinitionEntity);
    }

    public WorkflowDagNode findActivityDagNode(Long workflowDefinitionId, String activityName) {
        WorkflowDefinitionEntity workflowDefinitionEntity = workflowDefinitionPersistence.findByWorkflowDefinitionId(workflowDefinitionId);
        WorkflowDag workflowDag = workflowConvert.convert(workflowDefinitionEntity).getWorkflowDag();
        return workflowDag.getNode(activityName);
    }

    public ActivityDefinition findActivityDefinition(Long workflowDefinitionId, String activityName) {
        WorkflowDagNode workflowDagNode = findActivityDagNode(workflowDefinitionId, activityName);
        CheckUtils.checkNotNull(workflowDagNode, WorkflowConstants.INVALID_PARAMETER, "workflowDagNode should not be null!");
        return workflowDagNode.getActivityDefinition();
    }
}
