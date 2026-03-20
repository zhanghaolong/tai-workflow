package com.tai.workflow.domain.executor.strategy;

import com.tai.workflow.domain.executor.DagExecutionContext;
import com.tai.workflow.domain.executor.DagExecutionStrategy;
import com.tai.workflow.domain.service.WorkflowInstanceService;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Component
@RequiredArgsConstructor
public class WorkflowInstanceStatusNotAllowRunningExecutionStrategyImpl implements DagExecutionStrategy {
    private final WorkflowInstanceService workflowInstanceService;

    public void execute(DagExecutionContext dagExecutionContext) {
        WorkflowInstance workflowInstance = dagExecutionContext.getWorkflowInstance();
        log.info("WorkflowInstanceStatusNotAllowRunningExecutionStrategyImpl [{}/{}] has already been in TERMINATED state, could not execute [{}] "
                 + "any more", workflowInstance.getId(), workflowInstance.getName(), dagExecutionContext.getLastActivityName());
    }

    public boolean support(DagExecutionContext dagExecutionContext) {
        WorkflowInstance workflowInstance = dagExecutionContext.getWorkflowInstance();
        if (WorkflowState.TERMINATED != workflowInstance.getState()) {
            workflowInstance = this.workflowInstanceService.findWorkflowInstance(workflowInstance.getId());
        }

        return WorkflowState.TERMINATED == workflowInstance.getState();
    }
}

