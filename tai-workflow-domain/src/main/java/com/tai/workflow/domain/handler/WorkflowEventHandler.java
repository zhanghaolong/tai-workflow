package com.tai.workflow.domain.handler;

import com.tai.workflow.domain.service.ActivityInstanceService;
import com.tai.workflow.domain.service.WorkflowExecutionService;
import com.tai.workflow.domain.service.WorkflowInstanceService;
import com.tai.workflow.domain.service.WorkflowSignalService;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.SignalAction;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEventHandler {
    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowInstanceService workflowInstanceService;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowSignalService workflowSignalService;

    @EventListener
    public void onWorkflowInstanceCompleted(WorkflowInstanceCompletedEvent event) {
        completeWorkflowInstance(event.getWorkflowInstance());
    }

    @EventListener
    public void onWorkflowInstanceFailed(WorkflowInstanceFailedEvent event) {
        failWorkflowInstance(event.getWorkflowInstance(), SignalAction.FAILED_NORMAL);
    }

    @EventListener
    public void onWorkflowInstanceHumanProcessing(WorkflowInstanceHumanProcessingEvent event) {
        failWorkflowInstance(event.getWorkflowInstance(), SignalAction.FAILED_NORMAL);
    }

    @EventListener
    public void onWorkflowInstanceTerminated(WorkflowInstanceTerminatedEvent event) {
        WorkflowInstance workflowInstance = event.getWorkflowInstance();
        if (Objects.nonNull(workflowInstance.getParentId())) {
            workflowExecutionService.signalWorkflowInstance(workflowInstance.getParentId(), List.of(String.valueOf(workflowInstance.getId())),
                    SignalAction.TERMINATED, 0L);
        }
    }

    @EventListener
    public void onActivityInstanceCompleted(ActivityInstanceCompletedEvent event) {
        ActivityInstance activityInstance = activityInstanceService.findActivityInstance(event.getWorkflowContext().getActivityInstance().getId());
        Map<String, Object> mergedContextParams = new HashMap<>();
        if (MapUtils.isNotEmpty(event.getWorkflowContext().getAllContextParams())) {
            mergedContextParams.putAll(event.getWorkflowContext().getAllContextParams());
        }

        if (MapUtils.isNotEmpty(event.getActivityExecutionResult().getExtValues())) {
            mergedContextParams.putAll(event.getActivityExecutionResult().getExtValues());
        }

        if (StringUtils.isBlank(activityInstance.getSignalBizCode())) {
            workflowExecutionService.processActivityExecutionResult(activityInstance.getId(), ActivityState.COMPLETED,
                    event.getActivityExecutionResult(), mergedContextParams);
        } else if (!workflowSignalService.hasReceivedSignal(activityInstance.getWorkflowInstanceId(), activityInstance.getSignalBizCode())) {
            workflowInstanceService.makeActivitySignalWaiting(activityInstance.getId(), mergedContextParams);
        } else {
            workflowExecutionService.processActivityExecutionResult(activityInstance.getId(), ActivityState.COMPLETED,
                    event.getActivityExecutionResult(), mergedContextParams);
        }
    }

    @EventListener
    public void onActivityInstanceFailed(ActivityInstanceFailedEvent event) {
        ActivityExecutionResult errorResult = ActivityExecutionResult.ofFailed(getExecutionMsg(event.getThrowable(), event.getErrorMessage()));
        Map<String, Object> contextParams = new HashMap<>();
        workflowExecutionService.processActivityExecutionResult(event.getWorkflowContext().getActivityInstance().getId(), ActivityState.FAILED,
                errorResult, contextParams);
    }

    private void completeWorkflowInstance(WorkflowInstance workflowInstance) {
        workflowInstanceService.updateWorkflowInstanceState(workflowInstance.getId(), WorkflowState.COMPLETED);
        if (Objects.nonNull(workflowInstance.getParentId())) {
            List<String> mergeToParentContextKeys = (List<String>) workflowInstance.getContextParams()
                    .get(WorkflowConstants.MERGE_TO_PARENT_CONTEXT_KEYS);
            if (CollectionUtils.isEmpty(mergeToParentContextKeys)) {
                workflowExecutionService.signalWorkflowInstance(workflowInstance.getParentId(), List.of(String.valueOf(workflowInstance.getId())),
                        SignalAction.SUCCESS, 0L);
            } else {
                Map<String, Object> mergeContext = new HashMap<>();
                mergeToParentContextKeys.forEach(mergeToParentContextKey ->
                        mergeContext.put(mergeToParentContextKey, workflowInstance.getContextParams().get(mergeToParentContextKey)));
                workflowExecutionService.signalWorkflowInstance(workflowInstance.getParentId(), List.of(String.valueOf(workflowInstance.getId())),
                        SignalAction.SUCCESS, 0L, mergeContext, Boolean.FALSE);
            }
        }
    }

    private void failWorkflowInstance(WorkflowInstance workflowInstance, SignalAction signalAction) {
        if (Objects.nonNull(workflowInstance.getParentId())) {
            Map<String, Object> signalContextParams = new HashMap<>();
            workflowExecutionService.signalWorkflowInstance(workflowInstance.getParentId(), List.of(String.valueOf(workflowInstance.getId())),
                    signalAction, 0L, signalContextParams, Boolean.FALSE);
        }
    }

    private String getExecutionMsg(Throwable throwable, String errorMessage) {
        return Objects.nonNull(throwable) ? ExceptionUtils.getStackTrace(throwable) : errorMessage;
    }
}
