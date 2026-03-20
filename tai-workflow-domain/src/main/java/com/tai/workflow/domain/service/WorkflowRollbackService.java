package com.tai.workflow.domain.service;

import com.tai.workflow.api.Activity;
import com.tai.workflow.utils.JsonUtils;
import com.tai.workflow.domain.handler.EventFactory;
import com.tai.workflow.domain.handler.WorkflowEventPublisher;
import com.tai.workflow.domain.handler.WorkflowInstanceFailedEvent;
import com.tai.workflow.domain.util.WorkflowUtils;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.graph.WorkflowDagNode;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowContext;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowRollbackService {
    private final WorkflowDefinitionService workflowDefinitionService;
    private final InvokeLocator invokeLocator;
    private final WorkflowContextService workflowContextService;
    private final WorkflowInstanceService workflowInstanceService;
    private final ActivityInstanceService activityInstanceService;

    public void rollbackWorkflowInstance(Long workflowInstanceId, Long startRollbackActivityInstanceId, Map<String, Object> executionContext,
            Set<String> mustRollbackActivityNameSet) {
        WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(workflowInstanceId);
        doRollbackWorkflowInstance(workflowInstance, startRollbackActivityInstanceId, executionContext, mustRollbackActivityNameSet);
    }

    private void doRollbackWorkflowInstance(WorkflowInstance workflowInstance, Long startRollbackActivityInstanceId,
            Map<String, Object> executionContext, Set<String> mustRollbackActivityNameSet) {
        WorkflowDefinitionInternal workflowDefinitionInternal = workflowDefinitionService.findWorkflowDefinition(workflowInstance);
        Set<String> rollbackActivityNameList;
        if (Objects.nonNull(startRollbackActivityInstanceId)) {
            ActivityInstance activityInstance = activityInstanceService.findActivityInstance(startRollbackActivityInstanceId);
            rollbackActivityNameList = WorkflowUtils.getChildrenActivityNames(workflowDefinitionInternal.getWorkflowDag(), activityInstance.getName(),
                    Boolean.FALSE);
        } else {
            rollbackActivityNameList = workflowDefinitionInternal.getWorkflowDag()
                    .stream(false)
                    .map(WorkflowDagNode::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (!CollectionUtils.isEmpty(rollbackActivityNameList)) {
            rollbackActivityNameList.forEach((rollbackActivityName) -> {
                ActivityInstance activityInstance = activityInstanceService.findActivityInstance(workflowInstance.getId(), rollbackActivityName);
                if (mustRollbackActivityNameSet.contains(rollbackActivityName) || activityInstance.getState().requireRollback()) {
                    doRollbackActivity(workflowInstance, activityInstance, executionContext);
                }
            });
        }

        workflowInstanceService.updateWorkflowInstanceState(workflowInstance.getId(), WorkflowState.FAILED);
        WorkflowEventPublisher.publishEvent(
                EventFactory.createWorkflowInstanceEvent(workflowInstanceService.findWorkflowInstance(workflowInstance.getId()),
                        WorkflowInstanceFailedEvent.class));
    }

    private void doRollbackActivity(WorkflowInstance workflowInstance, ActivityInstance activityInstance, Map<String, Object> executionContext) {
        WorkflowContext workflowContext = workflowContextService.generateExecutionContext(workflowInstance, activityInstance);
        doRollbackSingleActivity(workflowContext, executionContext);
    }

    private void doRollbackSingleActivity(WorkflowContext workflowContext, Map<String, Object> executionContext) {
        if (MapUtils.isNotEmpty(executionContext)) {
            workflowContext.mergeContext(executionContext);
        }

        String executionMsg = null;
        try {
            Class<? extends Activity> activityClass = workflowContext.getActivityDefinition().getActivityClass();
            if (Objects.nonNull(activityClass) && invokeLocator.existInvoke(activityClass)) {
                Activity activity = invokeLocator.getInvokeTarget(activityClass, false);
                activity.rollback(workflowContext);
            }
        } catch (Throwable throwable) {
            executionMsg = WorkflowConstants.ROLLBACK_FAILED.formatted(ExceptionUtils.getStackTrace(throwable));
        }

        UpdateActivityInstanceParam updateActivityInstanceParam = UpdateActivityInstanceParam.builder()
                .id(workflowContext.getActivityInstance().getId())
                .expectedActivityState(ActivityState.FAILED.name())
                .activityStatesCondition(List.of(ActivityState.RUNNING.name(), ActivityState.SIGNAL_WAITING.name()))
                .outputContext(JsonUtils.toJson(workflowContext.getAllContextParams()))
                .endTime(new Date())
                .build();
        activityInstanceService.updateActivityInstance(updateActivityInstanceParam);
        if (StringUtils.isNotBlank(executionMsg)) {
            activityInstanceService.appendExecutionMsg(workflowContext.getActivityInstance().getId(), executionMsg);
        }
    }
}
