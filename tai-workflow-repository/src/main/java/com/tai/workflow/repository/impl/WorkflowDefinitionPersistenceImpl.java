package com.tai.workflow.repository.impl;

import com.tai.workflow.repository.entity.WorkflowDefinitionEntity;
import com.tai.workflow.repository.mapper.WorkflowDefinitionMapper;
import com.tai.workflow.repository.persistence.WorkflowDefinitionPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionPersistenceImpl implements WorkflowDefinitionPersistence {
    private final WorkflowDefinitionMapper mapper;
    private static final WeakReferenceCache<Long, WorkflowDefinitionEntity> WORKFLOW_DEFINITION_CACHE = new WeakReferenceCache<>();

    @Override
    public WorkflowDefinitionEntity findByWorkflowDefinitionId(Long workflowDefinitionId) {
        WorkflowDefinitionEntity entity = WORKFLOW_DEFINITION_CACHE.get(workflowDefinitionId);
        if (Objects.isNull(entity)) {
            entity = mapper.selectById(workflowDefinitionId);
            if (Objects.nonNull(entity)) {
                WORKFLOW_DEFINITION_CACHE.put(workflowDefinitionId, entity);
            }
        }

        return WORKFLOW_DEFINITION_CACHE.get(workflowDefinitionId);
    }

    @Override
    public Long save(WorkflowDefinitionEntity workflowDefinitionEntity) {
        mapper.insert(workflowDefinitionEntity);
        return workflowDefinitionEntity.getId();
    }

    @Override
    public WorkflowDefinitionEntity findByNameSpaceAndNameAndWorkerNode(String namespace, String workflowDefinitionName, String workerNode) {
        return mapper.selectTopByNamespaceAndNameAndWorkerNode(namespace, workflowDefinitionName, workerNode);
    }
}
