package com.tai.workflow.repository.impl;

import com.tai.workflow.repository.entity.WorkflowInstanceEntity;
import com.tai.workflow.repository.mapper.WorkflowInstanceMapper;
import com.tai.workflow.repository.param.ListWorkflowInstancesParam;
import com.tai.workflow.repository.param.UpdateWorkflowInstanceParam;
import com.tai.workflow.repository.persistence.WorkflowInstancePersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Service
@RequiredArgsConstructor
public class WorkflowInstancePersistenceImpl implements WorkflowInstancePersistence {
    private final WorkflowInstanceMapper mapper;

    @Override
    public WorkflowInstanceEntity findById(Long workflowInstanceId) {
        return mapper.selectById(workflowInstanceId);
    }

    @Override
    public WorkflowInstanceEntity findByNamespaceAndToken(String namespace, String token) {
        return mapper.selectByNamespaceAndToken(namespace, token);
    }

    @Override
    public List<WorkflowInstanceEntity> findByParentIdAndParentActivityId(Long parentWorkflowInstanceId, Long parentActivityId) {
        return mapper.selectByParentIdAndParentActivityId(parentWorkflowInstanceId, parentActivityId);
    }

    @Override
    public List<WorkflowInstanceEntity> findByParentId(Long parentId) {
        return mapper.selectByParentId(parentId);
    }

    @Override
    public WorkflowInstanceEntity findByPrimaryId(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public List<WorkflowInstanceEntity> findByNameAndStateAndEndTimeBetween(String name, String state, Long fromTime,
            Long toTime) {
        return mapper.selectByNameAndStateAndEndTimeBetween(name, state, fromTime, toTime);
    }

    @Override
    public void save(WorkflowInstanceEntity workflowInstanceEntity) {
        workflowInstanceEntity.setCreateTime(new Date());
        mapper.insert(workflowInstanceEntity);
    }

    @Override
    public int updateWorkflowInstance(UpdateWorkflowInstanceParam updateWorkflowInstanceParam) {
        if (Objects.isNull(updateWorkflowInstanceParam.getVersion())) {
            return mapper.updateWorkflowInstance(updateWorkflowInstanceParam);
        } else {
            return mapper.updateWorkflowInstanceWithOptimisticLock(updateWorkflowInstanceParam);
        }
    }

    @Override
    public List<WorkflowInstanceEntity> findByNamespaceAndStartTimeGreaterThanAndWorkerNodeNotInAndStateIn(String namespace, Long fromTime,
            List<String> onlineWorkerNodeList, List<String> states) {
        return mapper.selectByNamespaceAndStartTimeGreaterThanAndWorkerNodeNotInAndStateIn(namespace, fromTime, onlineWorkerNodeList, states);
    }

    @Override
    public List<WorkflowInstanceEntity> findByNamespaceAndStartTimeGreaterThanAndStateInAndDefinitionVariablesLike(String namespace,
            Long fromTime, List<String> states, String definitionVariables) {
        return mapper.selectByNamespaceAndStartTimeGreaterThanAndStateInAndDefinitionVariablesLike(namespace, fromTime, states,
                definitionVariables);
    }

    @Override
    public WorkflowInstanceEntity findByToken(String namespace, String token) {
        return mapper.selectByToken(namespace, token);
    }

    @Override
    public List<WorkflowInstanceEntity> listWorkflowInstances(ListWorkflowInstancesParam param) {
        return mapper.selectWorkflowInstances(param);
    }

    @Override
    public Long countByNamespace(String namespace) {
        return mapper.countByNamespace(namespace);
    }
}
