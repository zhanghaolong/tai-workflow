package com.tai.workflow.domain.service;

import com.tai.workflow.utils.CheckUtils;
import com.tai.workflow.utils.JsonUtils;
import com.tai.workflow.configuration.WorkflowConfiguration;
import com.tai.workflow.domain.executor.DagExecutionStrategyExecutor;
import com.tai.workflow.domain.handler.EventFactory;
import com.tai.workflow.domain.handler.WorkflowEventPublisher;
import com.tai.workflow.domain.handler.WorkflowInstanceStartedEvent;
import com.tai.workflow.domain.util.LoggerUtils;
import com.tai.workflow.domain.util.WorkflowUtils;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.SignalAction;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
public class WorkflowExecutionService {
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowInstanceService workflowInstanceService;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowExecutionCoreService workflowExecutionCoreService;
    private final WorkflowExecutionPool workflowExecutionPool;
    private final WorkflowRollbackService workflowRollbackService;
    private final WorkflowSignalService workflowSignalService;
    private final DagExecutionStrategyExecutor dagExecutionStrategyExecutor;
    private final WorkflowConfiguration workflowConfiguration;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorkflowInstance preStart(String workflowDefinitionName, Map<String, Object> contextParams, String token) {
        WorkflowDefinitionInternal workflowDefinitionInternal = workflowDefinitionService.findWorkflowDefinition(workflowConfiguration.getNamespace(),
                workflowDefinitionName, WorkflowUtils.getUniqueIdentity(workflowConfiguration.getNamespace()));
        return workflowInstanceService.initWorkflowInstance(workflowDefinitionInternal, contextParams, token);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorkflowInstance preStart(Long workflowDefinitionId, String token) {
        WorkflowDefinitionInternal workflowDefinitionInternal = workflowDefinitionService.findWorkflowDefinition(workflowDefinitionId);
        return workflowInstanceService.initWorkflowInstance(workflowDefinitionInternal, workflowDefinitionInternal.getDefinitionVariables(), token);
    }

    public void startWorkflowInstance(WorkflowInstance workflowInstance) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        runDag(workflowInstance);
        stopWatch.stop();
        log.info("WorkflowExecutionService start workflow {} successfully elapsed: {} ms", workflowInstance.getId(), stopWatch.getTotalTimeMillis());
    }

    private void runDag(WorkflowInstance workflowInstance) {
        workflowInstance.setState(WorkflowState.RUNNING);
        workflowInstanceService.updateWorkflowInstanceStatusUsingOptimisticLock(workflowInstance.getId(), WorkflowState.RUNNING,
                workflowInstance.getContextParams(), workflowInstance.getVersion());
        WorkflowEventPublisher.publishEvent(EventFactory.createWorkflowInstanceEvent(workflowInstance, WorkflowInstanceStartedEvent.class));
        doRunDag(workflowInstance, null, null);
    }

    public void processActivityExecutionResult(Long activityInstanceId, ActivityState activityState, ActivityExecutionResult activityExecutionResult,
            Map<String, Object> activityOutputContext) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ActivityInstance activityInstance = activityInstanceService.findActivityInstance(activityInstanceId);
        WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(activityInstance.getWorkflowInstanceId());

        try {
            boolean canRunDag = doSaveActivityExecutionResult(workflowInstance, activityInstance, activityState, activityExecutionResult,
                    activityOutputContext);
            if (canRunDag) {
                doRunDag(workflowInstance, activityInstance.getName(), activityState);
            }
        } catch (Throwable throwable) {
            log.error("processActivityExecutionResult runDag unknown Exception for:[{}]",
                    LoggerUtils.getTraceHeader(workflowInstance, activityInstance), throwable);
        }

        stopWatch.stop();
        log.info("processActivityExecutionResult(completeActivity): [{}] cost {} ms", LoggerUtils.getTraceHeader(workflowInstance, activityInstance),
                stopWatch.getTotalTimeMillis());
    }

    public void terminateWorkflowInstance(Long workflowInstanceId) {
        this.workflowExecutionCoreService.terminateWorkflowInstance(workflowInstanceId);
        List<WorkflowInstance> childrenWorkflowInstanceList = this.workflowInstanceService.findChildrenWorkflowInstance(workflowInstanceId, null);
        if (CollectionUtils.isNotEmpty(childrenWorkflowInstanceList)) {
            childrenWorkflowInstanceList.stream()
                    .filter((x) -> !List.of(WorkflowState.COMPLETED, WorkflowState.TERMINATED).contains(x.getState()))
                    .map(WorkflowInstance::getId)
                    .forEach(this::terminateWorkflowInstance);
        }
    }

    public void rollbackWorkflowInstance(Long workflowInstanceId, Long startRollbackActivityInstanceId, Map<String, Object> executionContext,
            Set<String> mustRollbackActivityNameSet) {
        WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(workflowInstanceId);
        CheckUtils.checkCondition(WorkflowState.PENDING != workflowInstance.getState(), WorkflowConstants.INVALID_PARAMETER,
                "should not rollback pending workflow");
        Runnable runnable = () -> workflowRollbackService.rollbackWorkflowInstance(workflowInstanceId, startRollbackActivityInstanceId,
                executionContext, mustRollbackActivityNameSet);
        workflowExecutionPool.getScheduleExecutor().execute(runnable);
    }

    public void signalWorkflowInstance(Long workflowInstanceId, Collection<String> signalBizCodeCollection, SignalAction signalAction,
            Long delayMills) {
        signalWorkflowInstance(workflowInstanceId, signalBizCodeCollection, signalAction, delayMills, Map.of(), Boolean.FALSE);
    }

    public void signalWorkflowInstance(Long workflowInstanceId, Collection<String> signalBizCodes, SignalAction signalAction, Long delayMills,
            Map<String, Object> contextParams, boolean uniqueCheck) {
        CheckUtils.checkNotEmpty(signalBizCodes, WorkflowConstants.INVALID_PARAMETER, "signalBizCodes should not be empty!");
        WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(workflowInstanceId);
        if (workflowInstance.getState() == WorkflowState.COMPLETED) {
            log.info("signalWorkflowInstance has already in end state could not be signaled for {}/{}/{}", workflowInstance.getId(),
                    workflowInstance.getName(), workflowInstance.getState());
        } else {
            List<ActivityInstance> activityInstances = activityInstanceService.findByWorkflowInstanceIdAndSignalBizCodeIn(workflowInstanceId,
                    signalBizCodes.stream().filter(StringUtils::isNotBlank).distinct().toList());
            activityInstances.forEach(activityInstance -> {
                String signalBizCode = activityInstance.getSignalBizCode();

                boolean processSignal = true;
                if (Objects.isNull(signalAction) || signalAction == SignalAction.SUCCESS) {
                    processSignal = workflowSignalService.saveWorkflowSignalRecord(activityInstance, signalBizCode, delayMills, uniqueCheck);
                }

                if (processSignal) {
                    doProcessSignal(workflowInstance, activityInstance, signalAction, signalBizCode, delayMills, contextParams);
                }
            });
        }
    }

    private void doProcessSignal(WorkflowInstance workflowInstance, ActivityInstance activityInstance, SignalAction signalAction,
            String signalBizCode, Long delayMills, Map<String, Object> contextParams) {
        Runnable runnable = () -> {
            if (SignalAction.TERMINATED == signalAction) {
                this.terminateWorkflowInstance(workflowInstance.getId());
            } else {
                if (signalBizCode.equals(activityInstance.getSignalBizCode())) {
                    Triple<ActivityState, ActivityExecutionResult, Map<String, Object>> processSignalResult =
                            workflowSignalService.processSignalResult(
                                    signalAction, contextParams);
                    Map<String, Object> signalContextParams = new HashMap<>(processSignalResult.getRight());
                    WorkflowInstance latestWorkflowInstance = workflowInstanceService.findWorkflowInstance(workflowInstance.getId());
                    Map<String, Object> currentContext = latestWorkflowInstance.getContextParams();
                    String countKey = "COUNT_" + signalBizCode;
                    int currentCount = 0;
                    if (MapUtils.isNotEmpty(currentContext) && currentContext.containsKey(countKey)) {
                        currentCount = ((Number) currentContext.get(countKey)).intValue();
                    }
                    signalContextParams.put(countKey, currentCount + 1);
                    processActivityExecutionResult(activityInstance.getId(), processSignalResult.getLeft(), processSignalResult.getMiddle(),
                            signalContextParams);
                }
            }
        };
        if (delayMills <= 0L) {
            workflowExecutionPool.getWorkerThreadPool().submit(runnable);
        } else {
            workflowExecutionPool.getScheduleExecutor().schedule(runnable, delayMills, TimeUnit.MILLISECONDS);
        }
    }

    public void doRunDag(WorkflowInstance workflowInstance, String lastActivityName, ActivityState lastActivityState) {
        dagExecutionStrategyExecutor.execute(workflowInstance, lastActivityName, lastActivityState);
    }

    public boolean doSaveActivityExecutionResult(WorkflowInstance workflowInstance, ActivityInstance activityInstance,
            ActivityState expectedActivityState, ActivityExecutionResult activityExecutionResult, Map<String, Object> activityOutputContext) {
        log.info("WorkflowExecutionCoreService.completeTask for {},activityExecutionResult is {}, expectedActivityState is {}",
                LoggerUtils.getTraceHeader(workflowInstance, activityInstance), JsonUtils.toJson(activityExecutionResult), expectedActivityState);
        Map<String, Object> outputContext = workflowInstance.getContextParams();
        if (MapUtils.isNotEmpty(activityOutputContext)) {
            outputContext.putAll(activityOutputContext);
        }

        UpdateActivityInstanceParam updateActivityInstanceParam = UpdateActivityInstanceParam.builder()
                .id(activityInstance.getId())
                .outputContext(JsonUtils.toJson(outputContext))
                .build();
        if (Objects.nonNull(activityExecutionResult)) {
            updateActivityInstanceParam.setExecutionMsg(activityExecutionResult.getMsg());
        }

        if (Objects.nonNull(expectedActivityState)) {
            ActivityDefinition activityDefinition = workflowDefinitionService.findActivityDefinition(workflowInstance.getWorkflowDefinitionId(),
                    activityInstance.getName());
            // 重试没有超过最大重试次数时 不需要设置为 FAILED
            if (expectedActivityState == ActivityState.FAILED
                && activityInstance.getRetryCount() < activityDefinition.getMaxRetry()) {
                expectedActivityState = ActivityState.PENDING;
            }

            updateActivityInstanceParam.setExpectedActivityState(expectedActivityState.name());
            updateActivityInstanceParam.setActivityStatesCondition(
                    List.of(ActivityState.SIGNAL_WAITING.name(),
                            ActivityState.RUNNING.name(),
                            ActivityState.PENDING.name(),
                            ActivityState.FAILED.name(),
                            expectedActivityState.name()));
            if (expectedActivityState.finalStage()) {
                updateActivityInstanceParam.setEndTime(new Date());
            }
        }

        if (MapUtils.isNotEmpty(activityOutputContext)) {
            workflowInstanceService.updateWorkflowInstanceStatusUsingOptimisticLock(workflowInstance.getId(), null,
                    outputContext, workflowInstance.getVersion());
        }

        return activityInstanceService.updateActivityInstance(updateActivityInstanceParam) > 0 && !activityExecutionResult.isSingleActivity();
    }

    public void runActivity(WorkflowInstance workflowInstance, String activityName, boolean resetRetryCount, boolean singleActivity) {
        workflowExecutionCoreService.runActivity(workflowInstance, activityName, resetRetryCount, singleActivity);
    }
}
