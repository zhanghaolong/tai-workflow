package com.tai.workflow.domain.service;

import com.tai.workflow.utils.JsonUtils;
import com.tai.workflow.configuration.WorkflowConfiguration;
import com.tai.workflow.domain.convert.WorkflowConvert;
import com.tai.workflow.domain.factory.ActivityInstanceFactory;
import com.tai.workflow.domain.factory.WorkflowInstanceFactory;
import com.tai.workflow.domain.handler.EventFactory;
import com.tai.workflow.domain.handler.WorkflowEventPublisher;
import com.tai.workflow.domain.handler.WorkflowInstanceHumanProcessingEvent;
import com.tai.workflow.domain.util.WorkflowUtils;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.graph.WorkflowDag;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import com.tai.workflow.repository.entity.WorkflowInstanceEntity;
import com.tai.workflow.repository.param.ListWorkflowInstancesParam;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import com.tai.workflow.repository.param.UpdateWorkflowInstanceParam;
import com.tai.workflow.repository.persistence.WorkflowInstancePersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
public class WorkflowInstanceService {
    private static final Integer MAX_RETRY = 10;
    private static final Integer OPTIMISTIC_LOCK_MIN = 10_000;
    private static final Integer OPTIMISTIC_LOCK_MAX = 90_000;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowInstancePersistence workflowInstancePersistence;
    private final WorkflowConfiguration workflowConfiguration;
    private final WorkflowConvert workflowInstanceConvert;

    public boolean workflowInstanceCompleted(Long workflowInstanceId) {
        return activityInstanceService.workflowInstanceCompleted(workflowInstanceId);
    }

    public WorkflowInstance findWorkflowInstance(Long workflowInstanceId) {
        WorkflowInstanceEntity workflowInstanceEntity = workflowInstancePersistence.findById(workflowInstanceId);
        return workflowInstanceConvert.convert(workflowInstanceEntity);
    }

    public WorkflowInstance findWorkflowInstance(String token) {
        WorkflowInstanceEntity workflowInstanceEntity = workflowInstancePersistence.findByNamespaceAndToken(workflowConfiguration.getNamespace(),
                token);
        return workflowInstanceConvert.convert(workflowInstanceEntity);
    }

    public WorkflowInstance findById(Long id) {
        WorkflowInstanceEntity workflowInstanceEntity = workflowInstancePersistence.findByPrimaryId(id);
        if (Objects.isNull(workflowInstanceEntity)) {
            return null;
        }
        return workflowInstanceConvert.convert(workflowInstanceEntity);
    }

    public List<WorkflowInstance> findByParentId(Long parentId) {
        List<WorkflowInstanceEntity> workflowInstanceEntities = workflowInstancePersistence.findByParentId(parentId);
        return Optional.ofNullable(workflowInstanceEntities)
                .orElse(List.of())
                .stream()
                .map(workflowInstanceConvert::convert)
                .collect(Collectors.toList());
    }

    public List<WorkflowInstance> findChildrenWorkflowInstance(Long parentWorkflowInstanceId, Long parentActivityId) {
        List<WorkflowInstanceEntity> workflowInstanceEntities = workflowInstancePersistence.findByParentIdAndParentActivityId(
                parentWorkflowInstanceId, parentActivityId);
        return Optional.ofNullable(workflowInstanceEntities)
                .orElse(List.of())
                .stream()
                .map(workflowInstanceConvert::convert)
                .collect(Collectors.toList());
    }

    public WorkflowInstance initWorkflowInstance(WorkflowDefinitionInternal workflowDefinitionInternal, Map<String, Object> contextParams,
            String token) {
        WorkflowInstance workflowInstance = WorkflowInstanceFactory.generateWorkflowInstance(workflowDefinitionInternal, contextParams, token);
        try {
            WorkflowInstanceEntity workflowInstanceEntity = workflowInstanceConvert.convert(workflowInstance);
            workflowInstancePersistence.save(workflowInstanceEntity);
            workflowInstance.setId(workflowInstanceEntity.getId());
            tryRelateToParentWorkflowInstance(workflowInstance);
            initAllActivityInstance(workflowDefinitionInternal, workflowInstance);
        } catch (DataIntegrityViolationException e) {
            log.info("initWorkflowInstance has found same token workflowInstance-workflowInstanceId:{} workflowInstanceName:{} with token {}",
                    workflowInstance.getId(), workflowInstance.getName(), workflowInstance.getToken());
            throw e;
        }

        return workflowInstance;
    }

    private void initAllActivityInstance(WorkflowDefinitionInternal workflowDefinitionInternal, WorkflowInstance workflowInstance) {
        WorkflowDag workflowDag = workflowDefinitionInternal.getWorkflowDag();
        List<ActivityInstance> activityInstances = workflowDag.stream()
                .map((workflowDagNode) -> ActivityInstanceFactory.generateActivityInstance(workflowConfiguration.getNamespace(), workflowDagNode,
                        workflowInstance))
                .collect(Collectors.toList());
        activityInstanceService.batchInsertActivityInstances(activityInstances);
    }

    private void tryRelateToParentWorkflowInstance(WorkflowInstance workflowInstance) {
        Optional<Pair<Long, Map<String, Object>>> parentWorkflowInstanceInfo = WorkflowUtils.extractParentWorkflowInstanceInfo();
        if (parentWorkflowInstanceInfo.isPresent()) {
            Long parentActivityId = (Long) parentWorkflowInstanceInfo.get().getRight().get(WorkflowConstants.PARENT_ACTIVITY_INSTANCE_ID);
            ActivityInstance parentActivityInstance = activityInstanceService.findActivityInstance(parentActivityId);
            if (NumberUtils.isDigits(parentActivityInstance.getSignalBizCode())) {
                Long workflowInstanceId = Long.valueOf(parentActivityInstance.getSignalBizCode());
                if (!workflowInstanceId.equals(workflowInstance.getId())) {
                    log.error("tryRelateToParentWorkflowInstance has found repeat child for {}/{}/{}", parentActivityInstance.getWorkflowInstanceId(),
                            parentActivityInstance.getId(), workflowInstanceId);
                    throw new IllegalStateException("you might have started multiple workflows in one activity!");
                }
            } else {
                UpdateActivityInstanceParam updateActivityInstanceParam = UpdateActivityInstanceParam.builder()
                        .id(parentActivityId)
                        .signalBizCode(String.valueOf(workflowInstance.getId()))
                        .build();
                activityInstanceService.updateActivityInstance(updateActivityInstanceParam);
            }
        }
    }

    public void makeWorkflowInstanceHumanProcessing(WorkflowInstance workflowInstance) {
        updateWorkflowInstanceState(workflowInstance.getId(), WorkflowState.HUMAN_PROCESSING);
        workflowInstance.setState(WorkflowState.HUMAN_PROCESSING);

        // Publish event after transaction commits to ensure data consistency
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WorkflowEventPublisher.publishEvent(
                        EventFactory.createWorkflowInstanceEvent(workflowInstance, WorkflowInstanceHumanProcessingEvent.class));
            }
        });
    }

    public void updateWorkflowInstanceState(Long workflowInstanceId, WorkflowState workflowState) {
        UpdateWorkflowInstanceParam updateWorkflowInstanceParam = UpdateWorkflowInstanceParam.builder()
                .id(workflowInstanceId)
                .state(workflowState.name())
                .build();
        if (workflowState.finalState()) {
            updateWorkflowInstanceParam.setEndTime(new Date());
        }

        workflowInstancePersistence.updateWorkflowInstance(updateWorkflowInstanceParam);
    }

    public void updateWorkflowInstanceStatusUsingOptimisticLock(Long workflowInstanceId, WorkflowState workflowState,
            Map<String, Object> contextParams, Integer version) {
        Map<String, Object> mergeContext = contextParams;

        int retryCount = 0;
        while (true) {
            UpdateWorkflowInstanceParam updateWorkflowInstanceParam = UpdateWorkflowInstanceParam.builder()
                    .id(workflowInstanceId)
                    .context(JsonUtils.toJson(mergeContext))
                    .build();

            if (retryCount <= MAX_RETRY) {
                updateWorkflowInstanceParam.setVersion(version);
            } else {
                log.info("updateWorkflowInstanceByIdUsingOptimisticLock try over maxRetries for:{} fallback to normal update", workflowInstanceId);
            }

            if (Objects.nonNull(workflowState)) {
                updateWorkflowInstanceParam.setState(workflowState.name());
                if (workflowState.finalState()) {
                    updateWorkflowInstanceParam.setEndTime(new Date());
                }
            }

            int updateResult = workflowInstancePersistence.updateWorkflowInstance(updateWorkflowInstanceParam);
            if (updateResult > 0) {
                return;
            }

            try {
                TimeUnit.MICROSECONDS.sleep(ThreadLocalRandom.current().nextInt(OPTIMISTIC_LOCK_MIN, OPTIMISTIC_LOCK_MAX));
            } catch (InterruptedException interruptedException) {
                log.warn("updateWorkflowInstanceByIdUsingOptimisticLock met with interrupted exception", interruptedException);
            }

            WorkflowInstance latestWorkflowInstance = workflowInstanceConvert.convert(workflowInstancePersistence.findById(workflowInstanceId));
            if (Objects.isNull(latestWorkflowInstance)) {
                break;
            }

            if (MapUtils.isNotEmpty(latestWorkflowInstance.getContextParams())) {
                mergeContext = latestWorkflowInstance.getContextParams();
            } else {
                mergeContext = new HashMap<>();
            }

            mergeContext.putAll(contextParams);
            version = latestWorkflowInstance.getVersion();
            log.info("updateWorkflowInstanceByIdUsingOptimisticLock update workflowInstance failed due to concurrent update for "
                     + "workflowInstanceId:{} latestVersion:{}", latestWorkflowInstance.getId(), latestWorkflowInstance.getVersion());

            retryCount++;
        } // end of while
    }

    public void makeActivitySignalWaiting(Long activityInstanceId, Map<String, Object> activityOutputContext) {
        ActivityInstance activityInstance = activityInstanceService.findActivityInstance(activityInstanceId);
        WorkflowInstance workflowInstance = findWorkflowInstance(activityInstance.getWorkflowInstanceId());
        Map<String, Object> outputContext = workflowInstance.getContextParams();
        if (MapUtils.isNotEmpty(activityOutputContext)) {
            outputContext.putAll(activityOutputContext);
            updateWorkflowInstanceStatusUsingOptimisticLock(workflowInstance.getId(), null, outputContext, workflowInstance.getVersion());
        }

        ActivityInstance updateActivityInstance = new ActivityInstance();
        updateActivityInstance.setId(activityInstanceId);
        updateActivityInstance.setState(ActivityState.SIGNAL_WAITING);
        updateActivityInstance.setExecutionMsg(StringUtils.EMPTY);

        UpdateActivityInstanceParam updateActivityInstanceParam = UpdateActivityInstanceParam.builder()
                .id(activityInstanceId)
                .expectedActivityState(ActivityState.SIGNAL_WAITING.name())
                .executionMsg(StringUtils.EMPTY)
                .activityStatesCondition(List.of(ActivityState.RUNNING.name(), ActivityState.FAILED.name()))
                .build();
        activityInstanceService.updateActivityInstance(updateActivityInstanceParam);
    }

    public List<WorkflowInstance> listOfflineWorkflowInstances(List<String> onlineWorkerNodeList) {
        Long fromTime = System.currentTimeMillis() - workflowConfiguration.getFailOverCheckTimeSecRange() * WorkflowConstants.MILLIS_PER_SECOND;
        List<WorkflowInstanceEntity> workflowInstanceEntities =
                workflowInstancePersistence.findByNamespaceAndStartTimeGreaterThanAndWorkerNodeNotInAndStateIn(
                        workflowConfiguration.getNamespace(), fromTime, onlineWorkerNodeList, List.of(WorkflowState.RUNNING.name()));
        if (CollectionUtils.isEmpty(workflowInstanceEntities)) {
            return List.of();
        }

        return workflowInstanceEntities.stream().map(workflowInstanceConvert::convert).collect(Collectors.toList());
    }

    public List<WorkflowInstance> listWorkflowInstances(ListWorkflowInstancesParam param) {
        List<WorkflowInstanceEntity> workflowInstanceEntities = workflowInstancePersistence.listWorkflowInstances(param);
        return Optional.ofNullable(workflowInstanceEntities)
                .orElse(List.of())
                .stream()
                .map(workflowInstanceConvert::convert)
                .collect(Collectors.toList());
    }

    /**
     * 统计当前 namespace 下的工作流实例数量
     *
     * @return 工作流实例数量
     */
    public Long countWorkflowInstances() {
        return workflowInstancePersistence.countByNamespace(workflowConfiguration.getNamespace());
    }
}
