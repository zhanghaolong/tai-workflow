package com.tai.workflow.domain.service;

import com.tai.workflow.domain.handler.EventFactory;
import com.tai.workflow.domain.handler.WorkflowEventPublisher;
import com.tai.workflow.domain.handler.WorkflowInstanceCompletedEvent;
import com.tai.workflow.domain.handler.WorkflowInstanceStartedEvent;
import com.tai.workflow.domain.util.WorkflowUtils;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.SignalAction;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.graph.WorkflowDag;
import com.tai.workflow.graph.WorkflowDagNode;
import com.tai.workflow.model.BriefActivityInstance;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
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
public class WorkflowResumeService {
    private final WorkflowInstanceService workflowInstanceService;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowExecutionCoreService workflowExecutionCoreService;
    private final WorkflowSignalService workflowSignalService;
    private final WorkflowExecutionService workflowExecutionService;

    @Value("${web3.workflow.retry.skip.running.enable:true}")
    private boolean skipRunning;

    @EventListener
    public void resumeWorkflowInstance(WorkflowInstance workflowInstance) {
        if (workflowInstance.getState() == WorkflowState.COMPLETED) {
            return;
        }

        if (workflowInstance.getState() != WorkflowState.RUNNING) {
            workflowInstance.setState(WorkflowState.RUNNING);
            workflowInstanceService.updateWorkflowInstanceState(workflowInstance.getId(), WorkflowState.RUNNING);
            WorkflowEventPublisher.publishEvent(EventFactory.createWorkflowInstanceEvent(workflowInstance, WorkflowInstanceStartedEvent.class));
        }

        WorkflowDefinitionInternal workflowDefinitionInternal = workflowDefinitionService.findWorkflowDefinition(
                workflowInstance.getWorkflowDefinitionId());
        List<BriefActivityInstance> activityInstanceList = activityInstanceService.findBriefActivityInstancesByWorkflowInstanceId(
                workflowInstance.getId());
        Map<String, BriefActivityInstance> activityInstanceMap = WorkflowUtils.regulateActivityInstance(activityInstanceList);
        List<String> leafNodeForResume = findLeafNodeForResume(workflowDefinitionInternal, activityInstanceMap);
        if (CollectionUtils.isEmpty(leafNodeForResume)) {
            log.info("WorkflowResumeService resumeDag has done for there's no leafNodeForResume for workflow:{}/{}", workflowInstance.getId(),
                    workflowInstance.getName());
            workflowInstance.setState(WorkflowState.COMPLETED);
            WorkflowEventPublisher.publishEvent(EventFactory.createWorkflowInstanceEvent(workflowInstance, WorkflowInstanceCompletedEvent.class));
        } else {
            log.info("WorkflowResumeService resumeDag has found leafNodes to resume:{} for workflow:{}/{}", leafNodeForResume,
                    workflowInstance.getId(), workflowInstance.getName());
            leafNodeForResume.forEach((leafNodeName) -> doResumeWorkflowInstance(workflowInstance, activityInstanceMap.get(leafNodeName)));
        }
    }

    private void doResumeWorkflowInstance(WorkflowInstance workflowInstance, BriefActivityInstance activityInstance) {
        if (skipRunning) {
            if (activityInstance.getState() == ActivityState.RUNNING) {
                log.info("ActivityInstance {} of workflowId:{} has been skipped", activityInstance.getId(), workflowInstance.getId());
                return;
            }
        }

        if (ActivityState.SIGNAL_WAITING == activityInstance.getState()) {
            Long signalTime = workflowSignalService.getReceivedSignalTime(activityInstance.getWorkflowInstanceId(),
                    activityInstance.getSignalBizCode());
            if (Objects.nonNull(signalTime)) {
                long delayMills = signalTime - System.currentTimeMillis();
                if (delayMills < 0L) {
                    delayMills = 0L;
                }

                workflowExecutionService.signalWorkflowInstance(workflowInstance.getId(), List.of(activityInstance.getSignalBizCode()),
                        SignalAction.SUCCESS, delayMills);
            }

            return;
        }

        final List<String> activityStatesCondition = List.of(ActivityState.PENDING.name(), ActivityState.FAILED.name());
        boolean skipCurrentActivity = activityInstance.getState().successOrSkipped();
        List<WorkflowInstance> childrenWorkflowInstanceList = workflowInstanceService.findChildrenWorkflowInstance(workflowInstance.getId(),
                activityInstance.getId());
        if (!skipCurrentActivity && CollectionUtils.isNotEmpty(childrenWorkflowInstanceList)) {
            int affectedRows = activityInstanceService.updateActivityInstance(UpdateActivityInstanceParam.builder()
                    .id(activityInstance.getId())
                    .expectedActivityState(ActivityState.RUNNING.name())
                    .activityStatesCondition(activityStatesCondition)
                    .build());
            if (affectedRows > 0) {
                childrenWorkflowInstanceList.forEach(this::resumeWorkflowInstance);
            }
        } else {
            if (skipCurrentActivity) {
                workflowExecutionCoreService.tryRunNextActivities(workflowInstance, activityInstance.getName());
            } else {
                workflowExecutionCoreService.runActivity(workflowInstance, activityInstance.getName(), Boolean.FALSE, Boolean.FALSE);
            }
        }
    }

    private List<String> findLeafNodeForResume(WorkflowDefinitionInternal workflowDefinitionInternal,
            Map<String, BriefActivityInstance> activityInstanceMap) {
        List<String> pendingNodeNameSet = activityInstanceMap.values()
                .stream()
                .filter((activityInstance) -> ActivityState.PENDING.equals(activityInstance.getState()))
                .map(BriefActivityInstance::getName)
                .collect(Collectors.toList());
        WorkflowDag workflowDag = workflowDefinitionInternal.getWorkflowDag();
        return workflowDag.getNodeMap()
                .entrySet()
                .stream()
                .filter((nodeEntry) -> isLeafNodeForResume(workflowDag, nodeEntry.getValue(), activityInstanceMap, pendingNodeNameSet))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean isLeafNodeForResume(WorkflowDag workflowDag, WorkflowDagNode workflowDagNode,
            Map<String, BriefActivityInstance> activityInstanceMap,
            List<String> pendingNodeNameSet) {
        String activityName = workflowDagNode.getActivityDefinition().getName();
        if (Objects.isNull(activityInstanceMap.get(activityName))) {
            return Boolean.FALSE;
        } else {
            ActivityState activityState = activityInstanceMap.get(activityName).getState();
            Set<String> outgoingNodeNameSet =
                    Objects.isNull(workflowDagNode.getOutgoingNodeNameSet()) ? Collections.emptySet() : workflowDagNode.getOutgoingNodeNameSet();
            boolean allParentsTaskDone = allParentTasksDone(workflowDag, activityInstanceMap, workflowDagNode.getIncomingNodeNameSet());
            if (!allParentsTaskDone) {
                return Boolean.FALSE;
            } else {
                Collection<String> intersection = CollectionUtils.intersection(outgoingNodeNameSet, pendingNodeNameSet);
                return !List.of(ActivityState.PENDING, ActivityState.COMPLETED, ActivityState.SKIPPED).contains(activityState)
                       && CollectionUtils.isNotEmpty(intersection) ? Boolean.TRUE
                                                                   : List.of(ActivityState.PENDING, ActivityState.CANCELLED,
                                                                                   ActivityState.FAILED, ActivityState.SIGNAL_WAITING)
                               .contains(activityState);
            }
        }
    }

    private boolean allParentTasksDone(WorkflowDag workflowDag, Map<String, BriefActivityInstance> activityInstanceMap,
            Set<String> incomingNodeNameSet) {
        return CollectionUtils.isEmpty(incomingNodeNameSet) ? Boolean.TRUE : incomingNodeNameSet.stream()
                .allMatch((nodeName) -> Objects.nonNull(activityInstanceMap.get(nodeName)) && activityInstanceMap.get(nodeName)
                        .getState()
                        .successOrSkipped() && allParentTasksDone(workflowDag, activityInstanceMap,
                        workflowDag.getNode(nodeName).getIncomingNodeNameSet()));
    }
}
