package com.tai.workflow.repository.persistence;

import com.tai.workflow.repository.entity.WorkflowInstanceEntity;
import com.tai.workflow.repository.param.ListWorkflowInstancesParam;
import com.tai.workflow.repository.param.UpdateWorkflowInstanceParam;

import java.util.List;

/**
 * @author zhanghaolong1989@163.com
 */
public interface WorkflowInstancePersistence {
    WorkflowInstanceEntity findById(Long workflowInstanceId);

    WorkflowInstanceEntity findByNamespaceAndToken(String namespace, String token);

    List<WorkflowInstanceEntity> findByParentIdAndParentActivityId(Long parentWorkflowInstanceId, Long parentActivityId);

    void save(WorkflowInstanceEntity workflowInstanceEntity);

    int updateWorkflowInstance(UpdateWorkflowInstanceParam updateWorkflowInstanceParam);

    List<WorkflowInstanceEntity> findByNamespaceAndStartTimeGreaterThanAndWorkerNodeNotInAndStateIn(String namespace, Long fromTime,
            List<String> onlineWorkerNodeList, List<String> states);

    List<WorkflowInstanceEntity> findByNamespaceAndStartTimeGreaterThanAndStateInAndDefinitionVariablesLike(String namespace,
            Long fromTime, List<String> states, String definitionVariables);

    WorkflowInstanceEntity findByToken(String namespace, String token);

    List<WorkflowInstanceEntity> findByParentId(Long parentId);

    WorkflowInstanceEntity findByPrimaryId(Long id);

    List<WorkflowInstanceEntity> findByNameAndStateAndEndTimeBetween(String name, String state, Long fromTime,
            Long toTime);

    List<WorkflowInstanceEntity> listWorkflowInstances(ListWorkflowInstancesParam param);

    /**
     * 根据 namespace 统计工作流实例数量
     *
     * @param namespace 命名空间
     * @return 工作流实例数量
     */
    Long countByNamespace(String namespace);

}
