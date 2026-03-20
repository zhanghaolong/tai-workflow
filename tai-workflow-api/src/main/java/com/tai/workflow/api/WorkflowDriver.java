package com.tai.workflow.api;

import com.tai.workflow.enums.SignalAction;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowContext;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author shenwangyan
 */
public interface WorkflowDriver {
    Long registerWorkflowDefinition(WorkflowDefinition workflowDefinition);

    Long startWorkflowInstance(String workflowDefinitionName, Map<String, Object> inputContextParams);

    Long startWorkflowInstance(String workflowDefinitionName, Map<String, Object> inputContextParams, String token);

    Long startWorkflowInstance(WorkflowDefinition workflowDefinition);

    Long startWorkflowInstance(WorkflowDefinition workflowDefinition, String token);

    void retryWorkflowInstance(Long workflowInstanceId);

    void retrySingleActivityInstance(Long activityInstanceId);

    void signalWorkflowInstance(Long workflowInstanceId, String signalBizCode, SignalAction signalAction);

    void signalWorkflowInstance(Long workflowInstanceId, String signalBizCode, SignalAction signalAction, boolean uniqueCheck);

    void signalWorkflowInstance(Long workflowInstanceId, String signalBizCode, SignalAction signalAction, Map<String, Object> contextParams);

    void signalWorkflowInstance(Long workflowInstanceId, String signalBizCode, SignalAction signalAction, Map<String, Object> contextParams, boolean uniqueCheck);

    void rollbackWorkflowInstance(Long workflowInstanceId);

    void terminateWorkflowInstance(Long workflowInstanceId);

    void skipWorkflowInstance(Long workflowInstanceId, List<Long> activityInstanceIds);

    WorkflowDefinitionInternal getWorkflowDefinition(Long workflowDefinitionId);

    WorkflowInstance getWorkflowInstance(Long workflowInstanceId);

    WorkflowInstance getWorkflowInstanceByToken(String token);

    WorkflowInstance getWorkflowInstanceById(Long id);

    ActivityInstance getActivityInstance(Long workflowInstanceId, String activityName);

    ActivityInstance getActivityInstance(Long activityInstanceId);

    List<ActivityInstance> listActivityInstances(Long workflowInstanceId);

    void materialize(WorkflowContext workflowContext);

    void refreshWorkflowContext(Long workflowInstanceId, final Map<String, Object> contextParams, final Set<String> removeKeys);

    List<WorkflowInstance> findWorkflowInstanceByParentId(Long parentId);

    ActivityInstance getActivityInstanceById(Long id);

    List<WorkflowInstance> listWorkflowInstances(Integer offset, Integer pageSize);

    /**
     * 查询当前 namespace 下所有工作流实例的总个数
     *
     * @return 工作流实例总数
     */
    Long countWorkflowInstances();
}
