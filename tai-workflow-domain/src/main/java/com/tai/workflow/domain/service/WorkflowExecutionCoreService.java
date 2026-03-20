package com.tai.workflow.domain.service;

import com.tai.workflow.utils.JsonUtils;
import com.tai.workflow.domain.handler.EventFactory;
import com.tai.workflow.domain.handler.WorkflowEventPublisher;
import com.tai.workflow.domain.handler.WorkflowInstanceCompletedEvent;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.graph.WorkflowDagNode;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowContext;
import com.tai.workflow.model.WorkflowInstance;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionCoreService {
    private final WorkflowDefinitionService workflowDefinitionService;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowContextService workflowContextService;
    private final WorkflowSignalService workflowSignalService;
    private final WorkflowTerminateService workflowTerminateService;
    private final WorkflowExecutionPool workflowExecutionPool;


    public void runActivity(WorkflowInstance workflowInstance, String activityName, boolean resetRetryCount, boolean singleActivity) {
        ActivityInstance activityInstance = activityInstanceService.findActivityInstance(workflowInstance.getId(), activityName);
        workflowInstance = workflowInstanceService.findWorkflowInstance(workflowInstance.getId());
        runActivity(workflowInstance, activityInstance, resetRetryCount, singleActivity);
    }

    public void runActivity(WorkflowInstance workflowInstance, ActivityInstance activityInstance, boolean resetRetryCount, boolean singleActivity) {
        doRunActivity(workflowInstance, activityInstance, resetRetryCount, singleActivity);
    }

    private void doRunActivity(WorkflowInstance workflowInstance, ActivityInstance activityInstance, boolean resetRetryCount, boolean singleActivity) {
        WorkflowContext workflowContext = workflowContextService.generateExecutionContext(workflowInstance, activityInstance, resetRetryCount);
        workflowExecutionPool.submitTask(workflowContext, this::preInvoke, singleActivity);
    }

    private Boolean preInvoke(WorkflowContext workflowContext) {
        WorkflowInstance workflowInstance = workflowContext.getWorkflowInstance();
        ActivityInstance activityInstance = workflowContext.getActivityInstance();
        boolean resetRetryCount = workflowContext.getResetRetryCount();
        Map<String, Object> inputContext = workflowInstance.getContextParams();
        if (MapUtils.isNotEmpty(activityInstance.getInputContext())) {
            if (MapUtils.isNotEmpty(inputContext)) {
                inputContext.putAll(activityInstance.getInputContext());
            } else {
                inputContext = activityInstance.getInputContext();
            }
        }

        UpdateActivityInstanceParam updateActivityInstanceParam = UpdateActivityInstanceParam.builder()
                .id(activityInstance.getId())
                .expectedActivityState(ActivityState.RUNNING.name())
                .activityStatesCondition(List.of(ActivityState.PENDING.name(), ActivityState.FAILED.name()))
                .inputContext(JsonUtils.toJson(inputContext))
                .startTime(new Date())
                .executionMsg(StringUtils.EMPTY)
                .build();
        if (Objects.isNull(activityInstance.getFirstStartTime())) {
            updateActivityInstanceParam.setFirstStartTime(new Date());
        }

        ActivityDefinition activityDefinition = workflowDefinitionService.findActivityDefinition(workflowInstance.getWorkflowDefinitionId(),
                activityInstance.getName());
        if (activityDefinition.getTimeoutMillis() > 0L) {
            updateActivityInstanceParam.setTimeoutTime(new Date(System.currentTimeMillis() + activityDefinition.getTimeoutMillis()));
        }

        updateActivityInstanceParam.setEndTime(null);
        if (resetRetryCount) {
            updateActivityInstanceParam.setRetryCount(0);
        }

        return activityInstanceService.updateActivityInstance(updateActivityInstanceParam) > 0;
    }

    public void retryActivity(final ActivityInstance activityInstance, final Map<String, Object> inputContextParams, long delayTimeMills,
            boolean systemRetry) {
        WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(activityInstance.getWorkflowInstanceId());
        if (WorkflowState.COMPLETED == workflowInstance.getState()) {
            return;
        }

        if (WorkflowState.RUNNING != workflowInstance.getState()) {
            workflowInstance.setState(WorkflowState.RUNNING);
            workflowInstanceService.updateWorkflowInstanceState(workflowInstance.getId(), WorkflowState.RUNNING);
        }

        UpdateActivityInstanceParam updateActivityInstanceParam = UpdateActivityInstanceParam.builder()
                .id(activityInstance.getId())
                .expectedActivityState(ActivityState.PENDING.name())
                .activityStatesCondition(List.of(ActivityState.FAILED.name(), ActivityState.PENDING.name()))
                .build();
        if (Objects.isNull(inputContextParams)) {
            updateActivityInstanceParam.setInputContext(JsonUtils.toJson(workflowInstance.getContextParams()));
        } else {
            updateActivityInstanceParam.setInputContext(JsonUtils.toJson(inputContextParams));
        }

        if (StringUtils.isNotBlank(activityInstance.getSignalBizCode())) {
            workflowSignalService.deleteSignalRecord(workflowInstance.getId(), activityInstance.getSignalBizCode());
        }

        if (systemRetry) {
            updateActivityInstanceParam.setRetryCount(activityInstance.getRetryCount() + 1);
        }

        if (delayTimeMills <= 0L) {
            if (activityInstanceService.updateActivityInstance(updateActivityInstanceParam) > 0) {
                runActivity(workflowInstance, activityInstanceService.findActivityInstance(activityInstance.getId()), !systemRetry, Boolean.FALSE);
            }
        } else {
            workflowExecutionPool.getScheduleExecutor()
                    .schedule(
                            () -> {
                                if (activityInstanceService.updateActivityInstance(updateActivityInstanceParam) > 0) {
                                    runActivity(workflowInstance, activityInstanceService.findActivityInstance(activityInstance.getId()),
                                            !systemRetry, Boolean.FALSE);
                                }
                            },
                            delayTimeMills, TimeUnit.MILLISECONDS);
        }
    }

    public void retryActivity(Long activityInstanceId, Map<String, Object> inputContextParams, long delayTimeMills, boolean systemRetry) {
        ActivityInstance activityInstance = activityInstanceService.findActivityInstance(activityInstanceId);
        retryActivity(activityInstance, inputContextParams, delayTimeMills, systemRetry);
    }

    public void tryRunActivity(WorkflowInstance workflowInstance, String activityName, String lastCompletedActivityName) {
        WorkflowDagNode workflowDagNode = workflowDefinitionService.findActivityDagNode(workflowInstance.getWorkflowDefinitionId(), activityName);
        Set<String> incomingNodeNameSet = workflowDagNode.getIncomingNodeNameSet();
        if (CollectionUtils.isEmpty(incomingNodeNameSet)) {
            runActivity(workflowInstance, activityName, Boolean.FALSE, Boolean.FALSE);
        } else if (incomingNodeNameSet.size() == 1 && incomingNodeNameSet.contains(lastCompletedActivityName)) {
            runActivity(workflowInstance, activityName, Boolean.FALSE, Boolean.FALSE);
        } else {
            boolean readyToRun = activityInstanceService.activityInstancesCompleted(workflowInstance.getId(), incomingNodeNameSet);
            if (readyToRun) {
                runActivity(workflowInstance, activityName, Boolean.FALSE, Boolean.FALSE);
            }
        }
    }

    public void tryRunNextActivities(WorkflowInstance workflowInstance, String completedActivityName) {
        WorkflowDagNode completedNode = workflowDefinitionService.findActivityDagNode(workflowInstance.getWorkflowDefinitionId(),
                completedActivityName);
        Set<String> outgoingNodeNameSet = completedNode.getOutgoingNodeNameSet();
        if (CollectionUtils.isNotEmpty(outgoingNodeNameSet)) {
            outgoingNodeNameSet.forEach(nextActivityName -> tryRunActivity(workflowInstance, nextActivityName, completedActivityName));
        } else {
            tryCompleteWorkflowInstance(workflowInstance.getId());
        }
    }

    public void terminateWorkflowInstance(Long workflowInstanceId) {
        workflowTerminateService.terminateWorkflowInstance(workflowInstanceId);
    }

    private void tryCompleteWorkflowInstance(Long workflowInstanceId) {
        boolean workflowCompleted = workflowInstanceService.workflowInstanceCompleted(workflowInstanceId);
        log.info("tryCompletedWorkflowInstance, workflowInstanceId=[{}] workflowCompleted=[{}]", workflowInstanceId, workflowCompleted);
        if (workflowCompleted) {
            WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(workflowInstanceId);
            workflowInstance.setState(WorkflowState.COMPLETED);
            WorkflowEventPublisher.publishEvent(EventFactory.createWorkflowInstanceEvent(workflowInstance, WorkflowInstanceCompletedEvent.class));
        }
    }
}
