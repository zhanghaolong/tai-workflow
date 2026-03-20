package com.tai.workflow.domain.executor.strategy;

import com.tai.workflow.domain.executor.DagExecutionContext;
import com.tai.workflow.domain.executor.DagExecutionStrategy;
import com.tai.workflow.domain.service.WorkflowExecutionCoreService;
import com.tai.workflow.enums.ActivityState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author zhanghaolong1989@163.com
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
@Component
@RequiredArgsConstructor
public class LastActivityNonFailureExecutionStrategyImpl implements DagExecutionStrategy {
    private final WorkflowExecutionCoreService workflowExecutionCoreService;

    public void execute(DagExecutionContext dagExecutionContext) {
        this.workflowExecutionCoreService.tryRunNextActivities(dagExecutionContext.getWorkflowInstance(), dagExecutionContext.getLastActivityName());
    }

    public boolean support(DagExecutionContext dagExecutionContext) {
        return ActivityState.FAILED != dagExecutionContext.getLastActivityState();
    }
}
