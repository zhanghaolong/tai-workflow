package com.tai.workflow.domain.service;

import com.tai.workflow.domain.handler.EventFactory;
import com.tai.workflow.domain.handler.WorkflowEventPublisher;
import com.tai.workflow.domain.handler.WorkflowInstanceTerminatedEvent;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.WorkflowState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhanghaolong1989@163.com
 */
@Service
@RequiredArgsConstructor
public class WorkflowTerminateService {
    private final WorkflowInstanceService workflowInstanceService;
    private final ActivityInstanceService activityInstanceService;

    public void terminateWorkflowInstance(Long workflowInstanceId) {
        workflowInstanceService.updateWorkflowInstanceState(workflowInstanceId, WorkflowState.TERMINATED);
        activityInstanceService.updateByWorkflowInstanceId(workflowInstanceId, ActivityState.CANCELLED.name(),
                List.of(ActivityState.RUNNING.name(), ActivityState.SIGNAL_WAITING.name(), ActivityState.PENDING.name()));
        WorkflowEventPublisher.publishEvent(
                EventFactory.createWorkflowInstanceEvent(this.workflowInstanceService.findWorkflowInstance(workflowInstanceId),
                        WorkflowInstanceTerminatedEvent.class));
    }
}
