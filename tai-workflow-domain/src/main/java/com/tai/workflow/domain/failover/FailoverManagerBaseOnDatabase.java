package com.tai.workflow.domain.failover;

import com.tai.workflow.configuration.WorkflowConfiguration;
import com.tai.workflow.domain.service.ActivityInstanceService;
import com.tai.workflow.domain.service.WorkflowInstanceService;
import com.tai.workflow.domain.service.WorkflowResumeService;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowInstance;
import com.tai.workflow.repository.entity.WorkflowNodeHeartbeatEntity;
import com.tai.workflow.repository.entity.WorkflowNodeLeaderEntity;
import com.tai.workflow.repository.persistence.WorkflowNodeHeartbeatPersistence;
import com.tai.workflow.repository.persistence.WorkflowNodeLeaderPersistence;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailoverManagerBaseOnDatabase {
    private static final Set<Long> IGNORE_WORKFLOW_IDS = new HashSet<>();

    private final WorkflowNodeHeartbeatPersistence workflowNodeHeartbeatPersistence;
    private final WorkflowNodeLeaderPersistence workflowNodeLeaderPersistence;
    @Getter
    private final WorkflowConfiguration workflowConfiguration;
    private final WorkflowInstanceService workflowInstanceService;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowResumeService workflowResumeService;

    @Value("${web3.workflow.failover.enable:true}")
    private boolean failoverEnable;

    public WorkflowNodeLeaderEntity queryLeader() {
        return workflowNodeLeaderPersistence.findByNamespace(workflowConfiguration.getNamespace());
    }

    public List<String> getWorkerNodeList(boolean online) {
        Date timeThreshold = new Date(System.currentTimeMillis() - workflowConfiguration.getHeartbeatTimeoutMinutes() * WorkflowConstants.SECONDS_PER_MINUTE
                                                          * WorkflowConstants.MILLIS_PER_SECOND);
        String namespace = workflowConfiguration.getNamespace();
        List<WorkflowNodeHeartbeatEntity> workflowNodeHeartbeatEntities;
        if (online) {
            workflowNodeHeartbeatEntities = workflowNodeHeartbeatPersistence.getOnlineWorkerNodeList(namespace, timeThreshold);
        } else {
            workflowNodeHeartbeatEntities = workflowNodeHeartbeatPersistence.getOfflineWorkerNodeList(namespace, timeThreshold);
        }

        return CollectionUtils.isEmpty(workflowNodeHeartbeatEntities) ? List.of() : workflowNodeHeartbeatEntities.stream()
                .map(WorkflowNodeHeartbeatEntity::getWorkerNode)
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean tryInsertLeader(String nodeId) {
        WorkflowNodeLeaderEntity workflowNodeLeaderEntity = WorkflowNodeLeaderEntity.builder()
                .namespace(workflowConfiguration.getNamespace())
                .leaderNode(nodeId)
                .refreshTime(new Date())
                .build();

        try {
            workflowNodeLeaderPersistence.save(workflowNodeLeaderEntity);
            return true;
        } catch (DataIntegrityViolationException e) {
            if (!StringUtils.contains(e.getMessage(), WorkflowConstants.DUPLICATE_KEY)) {
                log.error("tryInsertLeader met with exception", e);
            }

            return false;
        }
    }

    public int refreshLeader(String currentLeaderId) {
        return workflowNodeLeaderPersistence.refreshLeader(workflowConfiguration.getNamespace(), currentLeaderId, new Date());
    }

    public void registerHeartbeat(String selfNodeId) {
        if (StringUtils.isBlank(selfNodeId)) {
            log.error("DBFailoverManager node id can not be null when register heartbeat");
        } else {
            WorkflowNodeHeartbeatEntity workflowNodeHeartbeatEntity = workflowNodeHeartbeatPersistence.findByNamespaceAndWorkerNode(
                    workflowConfiguration.getNamespace(), selfNodeId);
            if (Objects.isNull(workflowNodeHeartbeatEntity)) {
                workflowNodeHeartbeatEntity = WorkflowNodeHeartbeatEntity.builder()
                        .namespace(workflowConfiguration.getNamespace())
                        .workerNode(selfNodeId)
                        .heartbeatTime(new Date())
                        .build();
                workflowNodeHeartbeatPersistence.save(workflowNodeHeartbeatEntity);
            } else {
                workflowNodeHeartbeatPersistence.updateHeartbeatTime(workflowConfiguration.getNamespace(), selfNodeId, new Date());
            }
        }
    }

    @Transactional
    public boolean deleteExpiredLeader(String namespace) {
        long timeThreshold = System.currentTimeMillis() - workflowConfiguration.getHeartbeatTimeoutMinutes() * WorkflowConstants.SECONDS_PER_MINUTE
                                                          * WorkflowConstants.MILLIS_PER_SECOND;
        return BooleanUtils.toBoolean(workflowNodeLeaderPersistence.deleteExpiredLeader(namespace, new Date(timeThreshold)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = {Exception.class})
    public void doOfflineWorkerNodeProcess() {
        List<String> offlineWorkerNodes = getWorkerNodeList(false);
        if (CollectionUtils.isEmpty(offlineWorkerNodes)) {
            return;
        }

        log.info("DBFailoverManager doOfflineWorkerNodeProcess failover enabled:{}, has checked offlineWorkerNodes:{} ", failoverEnable, offlineWorkerNodes);
        workflowNodeHeartbeatPersistence.deleteByNamespaceAndWorkerNodeIn(workflowConfiguration.getNamespace(), offlineWorkerNodes);
    }

    public void checkDeadWorker() {
        if (log.isDebugEnabled()) {
            log.debug("AbstractFailOverManager check for dead workers");
        }

        if (!failoverEnable) {
            return;
        }

        List<String> onlineWorkerNodeList = getWorkerNodeList(true);
        if (CollectionUtils.isNotEmpty(onlineWorkerNodeList)) {
            List<WorkflowInstance> workflowInstanceStartedByDeadWorkers = workflowInstanceService.listOfflineWorkflowInstances(onlineWorkerNodeList);
            tryResumeWorkflowInstance(workflowInstanceStartedByDeadWorkers);
        }
    }

    private void tryResumeWorkflowInstance(List<WorkflowInstance> workflowInstanceStartedByDeadWorkers) {
        if (!CollectionUtils.isEmpty(workflowInstanceStartedByDeadWorkers)) {
            Set<Long> rootIdSet = workflowInstanceStartedByDeadWorkers.stream()
                    .filter((workflowInstance) -> Objects.isNull(workflowInstance.getParentId()))
                    .map(WorkflowInstance::getId)
                    .collect(Collectors.toSet());
            workflowInstanceStartedByDeadWorkers.forEach((workflowInstance) -> {
                try {
                    boolean canResume = canResume(workflowInstance, rootIdSet);
                    log.info("tryToResumeWorkflow for workflowInstanceId:[{}] find it canResume:[{}] rootIdSet is:[{}]", workflowInstance.getId(),
                            canResume, rootIdSet);
                    if (canResume) {
                        activityInstanceService.updateByWorkflowInstanceId(workflowInstance.getId(), ActivityState.PENDING.name(),
                                List.of(ActivityState.RUNNING.name()));
                        workflowResumeService.resumeWorkflowInstance(workflowInstance);
                    }
                } catch (Exception exception) {
                    if (exception instanceof ClassNotFoundException) {
                        IGNORE_WORKFLOW_IDS.add(workflowInstance.getId());
                        log.info("AbstractFailOverManager tryResumeWorkflowInstance add workflowInstanceId:[{}] to ignoreWorkflowInstanceIds due to "
                                 + "exception", workflowInstance.getId(), exception);
                    } else {
                        log.error("AbstractFailOverManager tryResumeWorkflowInstance met exception for workflowInstanceId:[{}]",
                                workflowInstance.getId(), exception);
                    }
                }
            });
        }
    }

    private boolean canResume(WorkflowInstance workflowInstance, Set<Long> rootIdSet) {
        if (IGNORE_WORKFLOW_IDS.contains(workflowInstance.getId())) {
            return false;
        } else if (rootIdSet.contains(workflowInstance.getId())) {
            return true;
        } else {
            Long rootId = findRootId(workflowInstance);
            return !rootIdSet.contains(rootId);
        }
    }

    private Long findRootId(WorkflowInstance workflowInstance) {
        if (Objects.isNull(workflowInstance.getParentId())) {
            return workflowInstance.getId();
        } else {
            WorkflowInstance parentWorkflowInstance = workflowInstanceService.findWorkflowInstance(workflowInstance.getParentId());
            return Objects.isNull(parentWorkflowInstance) ? workflowInstance.getId() : findRootId(parentWorkflowInstance);
        }
    }
}
