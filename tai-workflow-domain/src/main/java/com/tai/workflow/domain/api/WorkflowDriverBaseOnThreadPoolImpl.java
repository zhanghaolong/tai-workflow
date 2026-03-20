package com.tai.workflow.domain.api;

import com.tai.workflow.enums.Order;
import com.tai.workflow.utils.CheckUtils;
import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.convert.WorkflowConvert;
import com.tai.workflow.domain.service.ActivityInstanceService;
import com.tai.workflow.domain.service.InvokeLocator;
import com.tai.workflow.domain.service.WorkflowDefinitionService;
import com.tai.workflow.domain.service.WorkflowExecutionService;
import com.tai.workflow.domain.service.WorkflowInstanceService;
import com.tai.workflow.domain.service.WorkflowResumeService;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.SignalAction;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowContext;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import com.tai.workflow.repository.param.ListWorkflowInstancesParam;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Component
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
public class WorkflowDriverBaseOnThreadPoolImpl implements WorkflowDriver {
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowInstanceService workflowInstanceService;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowResumeService workflowResumeService;
    private final InvokeLocator invokeLocator;
    private final WorkflowConvert workflowConvert;

    @Override
    public Long registerWorkflowDefinition(WorkflowDefinition workflowDefinition) {
        Collection<ActivityDefinition> activityDefinitions = workflowDefinition.getNodeMap().values();
        for (ActivityDefinition activityDefinition : activityDefinitions) {
            CheckUtils.checkCondition(invokeLocator.existInvoke(activityDefinition.getActivityClass()), WorkflowConstants.INVALID_PARAMETER,
                    String.format("ActivityDefinition name:[%s] could not locate activityClass:[%s]", activityDefinition.getName(),
                            activityDefinition.getActivityClass()));
        }
        WorkflowDefinitionInternal workflowDefinitionInternal = WorkflowDefinitionInternal.toWorkflowDefinition(workflowDefinition);
        CheckUtils.checkNotNull(workflowDefinitionInternal, WorkflowConstants.INVALID_PARAMETER, "workflowDefinition should not be null!");
        CheckUtils.checkNotEmpty(workflowDefinitionInternal.getName(), WorkflowConstants.INVALID_PARAMETER,
                "workflowDefinitionName should not be empty!");
        CheckUtils.checkNotNull(workflowDefinitionInternal.getWorkflowDag(), WorkflowConstants.INVALID_PARAMETER, "workflowDag should not be null!");
        return workflowDefinitionService.registerWorkflowDefinition(workflowDefinitionInternal);
    }

    @Override
    public Long startWorkflowInstance(String workflowDefinitionName, Map<String, Object> inputContextParams) {
        CheckUtils.checkNotEmpty(workflowDefinitionName, WorkflowConstants.INVALID_PARAMETER, "workflowDefinitionName should not be empty!");
        return startWorkflowInstance(workflowDefinitionName, inputContextParams, null);
    }

    @Override
    public Long startWorkflowInstance(String workflowDefinitionName, Map<String, Object> inputContextParams, String token) {
        CheckUtils.checkNotEmpty(workflowDefinitionName, WorkflowConstants.INVALID_PARAMETER, "workflowDefinitionName should not be empty!");
        WorkflowInstance workflowInstance;
        try {
            workflowInstance = workflowExecutionService.preStart(workflowDefinitionName, inputContextParams, token);
            workflowExecutionService.startWorkflowInstance(workflowInstance);
        } catch (DataIntegrityViolationException e) {
            if (StringUtils.contains(e.getMessage(), WorkflowConstants.DUPLICATE_KEY)
                || StringUtils.contains(e.getMessage(), WorkflowConstants.UNIQUE_KEY)) {
                // 说明同样的 token 已经存在了一个工作流实例 此时走 resume 流程
                workflowInstance = workflowInstanceService.findWorkflowInstance(token);
                workflowResumeService.resumeWorkflowInstance(workflowInstance);
            } else {
                throw e;
            }
        }

        return workflowInstance.getId();
    }

    @Override
    public Long startWorkflowInstance(WorkflowDefinition workflowDefinition) {
        return startWorkflowInstance(workflowDefinition, null);
    }

    @Override
    public Long startWorkflowInstance(WorkflowDefinition workflowDefinition, String token) {
        Long workflowDefinitionId = registerWorkflowDefinition(workflowDefinition);
        CheckUtils.checkNotNull(workflowDefinitionId, WorkflowConstants.INVALID_PARAMETER, "workflowDefinitionId should not be null!");
        WorkflowInstance workflowInstance;
        try {
            workflowInstance = workflowExecutionService.preStart(workflowDefinitionId, token);
            workflowExecutionService.startWorkflowInstance(workflowInstance);
        } catch (DataIntegrityViolationException e) {
            if (StringUtils.contains(e.getMessage(), WorkflowConstants.DUPLICATE_KEY)
                || StringUtils.contains(e.getMessage(), WorkflowConstants.UNIQUE_KEY)) {
                // 说明同样的 token 已经存在了一个工作流实例 此时走 resume 流程
                workflowInstance = workflowInstanceService.findWorkflowInstance(token);
                workflowResumeService.resumeWorkflowInstance(workflowInstance);
            } else {
                throw e;
            }
        }

        return workflowInstance.getId();
    }

    @Override
    public void retryWorkflowInstance(Long workflowInstanceId) {
        log.info("retryWorkflowInstance workflowInstanceId:{}", workflowInstanceId);
        WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(workflowInstanceId);
        workflowResumeService.resumeWorkflowInstance(workflowInstance);
    }

    @Override
    public void retrySingleActivityInstance(Long activityInstanceId) {
        ActivityInstance activityInstance = activityInstanceService.findActivityInstance(activityInstanceId);
        if (activityInstance != null) {
            activityInstanceService.updateActivityInstance(UpdateActivityInstanceParam.builder()
                    .id(activityInstance.getId())
                    .expectedActivityState(ActivityState.PENDING.name())
                    .build());
        }
        WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(activityInstance.getWorkflowInstanceId());
        workflowExecutionService.runActivity(workflowInstance, activityInstance.getName(), Boolean.FALSE, Boolean.TRUE);
    }

    @Override
    public void signalWorkflowInstance(Long workflowInstanceId, String signalBizCode, SignalAction signalAction) {
        signalWorkflowInstance(workflowInstanceId, signalBizCode, signalAction, Map.of());
    }

    @Override
    public void signalWorkflowInstance(Long workflowInstanceId, String signalBizCode, SignalAction signalAction, boolean uniqueCheck) {
        workflowExecutionService.signalWorkflowInstance(workflowInstanceId, Set.of(signalBizCode), signalAction, 0L, Map.of(), uniqueCheck);
    }

    @Override
    public void signalWorkflowInstance(Long workflowInstanceId, String signalBizCode, SignalAction signalAction,
            Map<String, Object> contextParams) {
        workflowExecutionService.signalWorkflowInstance(workflowInstanceId, Set.of(signalBizCode), signalAction, 0L, contextParams, Boolean.FALSE);
    }

    @Override
    public void signalWorkflowInstance(Long workflowInstanceId, String signalBizCode, SignalAction signalAction,
            Map<String, Object> contextParams, boolean uniqueCheck) {
        workflowExecutionService.signalWorkflowInstance(workflowInstanceId, Set.of(signalBizCode), signalAction, 0L, contextParams, uniqueCheck);
    }

    @Override
    public void rollbackWorkflowInstance(Long workflowInstanceId) {
        workflowExecutionService.rollbackWorkflowInstance(workflowInstanceId, null, Map.of(), Set.of());
    }

    @Override
    public void terminateWorkflowInstance(Long workflowInstanceId) {
        workflowExecutionService.terminateWorkflowInstance(workflowInstanceId);
    }

    @Override
    public void skipWorkflowInstance(Long workflowInstanceId, List<Long> activityInstanceIds) {
        log.info("skipWorkflowInstance workflowInstanceId:{} activityInstanceIds:{}", workflowInstanceId, activityInstanceIds);
        if (CollectionUtils.isEmpty(activityInstanceIds)) {
            return;
        }


        boolean signal = false;
        for (Long activityInstanceId: activityInstanceIds) {
            ActivityInstance activityInstance = activityInstanceService.findActivityInstance(activityInstanceId);
            if (Objects.isNull(activityInstance)
                || !Objects.equals(activityInstance.getWorkflowInstanceId(), workflowInstanceId)
                || activityInstance.getState().successOrSkipped()) {
                continue;
            }

            activityInstanceService.updateActivityInstance(UpdateActivityInstanceParam.builder()
                    .id(activityInstanceId)
                    .expectedActivityState(ActivityState.SKIPPED.name())
                    .endTime(new Date())
                    .build());

            if (StringUtils.isNotBlank(activityInstance.getSignalBizCode())) {
                signalWorkflowInstance(workflowInstanceId, activityInstance.getSignalBizCode(), SignalAction.SUCCESS);
                signal = true;
            }
        }

        if (!signal) {
            workflowResumeService.resumeWorkflowInstance(workflowInstanceService.findWorkflowInstance(workflowInstanceId));
        }
    }

    @Override
    public WorkflowDefinitionInternal getWorkflowDefinition(Long workflowDefinitionId) {
        return workflowDefinitionService.findWorkflowDefinition(workflowDefinitionId);
    }

    @Override
    public WorkflowInstance getWorkflowInstance(Long workflowInstanceId) {
        return workflowInstanceService.findWorkflowInstance(workflowInstanceId);
    }

    @Override
    public WorkflowInstance getWorkflowInstanceByToken(String token) {
        return workflowInstanceService.findWorkflowInstance(token);
    }

    @Override
    public WorkflowInstance getWorkflowInstanceById(Long id) {
        return workflowInstanceService.findById(id);
    }

    @Override
    public List<WorkflowInstance> findWorkflowInstanceByParentId(Long parentId) {
        return workflowInstanceService.findByParentId(parentId);
    }

    @Override
    public ActivityInstance getActivityInstance(Long workflowInstanceId, String activityName) {
        return activityInstanceService.findActivityInstance(workflowInstanceId, activityName);
    }

    @Override
    public ActivityInstance getActivityInstance(Long activityInstanceId) {
        return activityInstanceService.findActivityInstance(activityInstanceId);
    }

    @Override
    public List<ActivityInstance> listActivityInstances(Long workflowInstanceId) {
        return activityInstanceService.findByWorkflowInstanceId(workflowInstanceId);
    }

    @Override
    public ActivityInstance getActivityInstanceById(Long id) {
        return activityInstanceService.findByPrimaryId(id);
    }

    @Override
    public void materialize(WorkflowContext workflowContext) {
        Long workflowInstanceId = workflowContext.getWorkflowInstance().getId();
        Integer version = workflowContext.getWorkflowInstance().getVersion();
        workflowInstanceService.updateWorkflowInstanceStatusUsingOptimisticLock(workflowInstanceId, null, workflowContext.getContextParams(),
                version);
    }

    @Override
    public void refreshWorkflowContext(Long workflowInstanceId, final Map<String, Object> contextParams, final Set<String> removeKeys) {
        WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(workflowInstanceId);
        Integer version = workflowInstance.getVersion();
        Map<String, Object> mergedContextParams = workflowInstance.getContextParams();
        if (MapUtils.isEmpty(mergedContextParams)) {
            mergedContextParams = new HashMap<>();
        }

        if (MapUtils.isNotEmpty(contextParams)) {
            mergedContextParams.putAll(contextParams);
        }

        if (CollectionUtils.isNotEmpty(removeKeys)) {
            removeKeys.forEach(mergedContextParams::remove);
        }

        workflowInstanceService.updateWorkflowInstanceStatusUsingOptimisticLock(workflowInstanceId, null, mergedContextParams, version);
    }

    @Override
    public List<WorkflowInstance> listWorkflowInstances(Integer offset, Integer pageSize) {
        return workflowInstanceService.listWorkflowInstances(ListWorkflowInstancesParam.builder().nextToken(offset).maxResults(pageSize).idOrder(Order.DESC).build());
    }

    @Override
    public Long countWorkflowInstances() {
        return workflowInstanceService.countWorkflowInstances();
    }
}
