package com.tai.workflow.domain.failover;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.configuration.WorkflowConfiguration;
import com.tai.workflow.domain.service.ActivityInstanceService;
import com.tai.workflow.domain.service.WorkflowDefinitionService;
import com.tai.workflow.domain.service.WorkflowExecutionService;
import com.tai.workflow.domain.service.WorkflowInstanceService;
import com.tai.workflow.domain.util.WorkflowUtils;
import com.tai.workflow.enums.SignalAction;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowInstance;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRelationshipListenerImpl implements RelationshipListener {
    private static final Integer SHUTDOWN_MAX_WAIT_SEC = 10;
    private final WorkflowExecutionService workflowExecutionService;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowInstanceService workflowInstanceService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowConfiguration workflowConfiguration;
    private final FailoverManagerBaseOnDatabase failoverManagerBaseOnDatabase;
    private final WorkflowDriver workflowDriver;
    private ScheduledThreadPoolExecutor scheduleService;

    @PostConstruct
    public void init() {
        // one for timeout activity check
        // one for workflow instance failover
        // one for leader election
        // one for node heartbeat
        final int totalCoreThreads = 4;
        final String selfNodeId = WorkflowUtils.getUniqueIdentity(workflowConfiguration.getNamespace());

        final LeaderCompeteThread leaderCompeteThread = new LeaderCompeteThread(selfNodeId, this, failoverManagerBaseOnDatabase);
        final WorkNodeRegisterThread workNodeRegisterThread = new WorkNodeRegisterThread(selfNodeId, failoverManagerBaseOnDatabase);
        scheduleService = new ScheduledThreadPoolExecutor(totalCoreThreads);
        scheduleService.scheduleAtFixedRate(leaderCompeteThread, 0L,
                workflowConfiguration.getFailOverScheduleTaskSeconds(), TimeUnit.SECONDS);
        scheduleService.scheduleAtFixedRate(workNodeRegisterThread, 0L,
                workflowConfiguration.getFailOverScheduleTaskSeconds(), TimeUnit.SECONDS);
    }

    public void becomeLeader() {
        // 只有 leader 执行下述例程
        // 1. timeoutActivity 检测
        // 2. workflow failover
        scheduleService.scheduleAtFixedRate(this::doTimeoutActivityCheck, 0L, workflowConfiguration.getHeartbeatTimeoutMinutes(), TimeUnit.MINUTES);
        scheduleService.scheduleAtFixedRate(failoverManagerBaseOnDatabase::checkDeadWorker, 0L,
                workflowConfiguration.getFailOverScheduleTaskSeconds(), TimeUnit.SECONDS);
    }

    public void doTimeoutActivityCheck() {
        if (log.isDebugEnabled()) {
            log.debug("DefaultRelationshipListenerImpl doTimeoutActivityCheck start to check timeout task!");
        }
        List<ActivityInstance> timeoutActivityInstances = activityInstanceService.listTimeoutActivityInstances(workflowConfiguration.getNamespace(),
                workflowConfiguration.getFailOverCheckTimeSecRange());
        timeoutActivityInstances.stream()
                .filter(timeoutActivityInstance -> Objects.nonNull(timeoutActivityInstance.getSignalBizCode()))
                .forEach((timeoutActivityInstance) -> {
                    try {
                        WorkflowInstance workflowInstance = workflowInstanceService.findWorkflowInstance(
                                timeoutActivityInstance.getWorkflowInstanceId());
                        if (!StringUtils.equalsIgnoreCase(workflowConfiguration.getNamespace(), workflowInstance.getNamespace())) {
                            return;
                        }

                        ActivityDefinition activityDefinition = workflowDefinitionService.findActivityDefinition(
                                workflowInstance.getWorkflowDefinitionId(),
                                timeoutActivityInstance.getName());
                        if (Objects.isNull(activityDefinition)) {
                            log.error("doTimeoutActivityCheck could not find activityDefinition for [{}/{}/{}]",
                                    workflowInstance.getWorkflowDefinitionId(),
                                    workflowInstance.getId(), timeoutActivityInstance.getName());
                            return;
                        }

                        log.info("doTimeoutActivityCheck has found [{}/{}/{}] timeout so signal it failed",
                                timeoutActivityInstance.getWorkflowInstanceId(),
                                timeoutActivityInstance.getName(), timeoutActivityInstance.getSignalBizCode());
                        workflowExecutionService.signalWorkflowInstance(timeoutActivityInstance.getWorkflowInstanceId(),
                                List.of(timeoutActivityInstance.getSignalBizCode()), SignalAction.FAILED_AT_ONCE, 0L);
                    } catch (Throwable throwable) {
                        log.error("doTimeoutActivityCheckJob has met throwable", throwable);
                    }
                });
    }

    public void lostLeader() {
        destroy();
    }

    @PreDestroy
    public void destroy() {
        scheduleService.shutdownNow();
    }
}
