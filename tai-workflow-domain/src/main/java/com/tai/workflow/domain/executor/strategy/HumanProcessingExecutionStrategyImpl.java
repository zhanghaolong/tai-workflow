package com.tai.workflow.domain.executor.strategy;

import com.tai.workflow.domain.executor.DagExecutionContext;
import com.tai.workflow.domain.executor.DagExecutionStrategy;
import com.tai.workflow.domain.service.WorkflowInstanceService;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
@Component
@RequiredArgsConstructor
public class HumanProcessingExecutionStrategyImpl implements DagExecutionStrategy {
    private final WorkflowInstanceService workflowInstanceService;

    public void execute(DagExecutionContext dagExecutionContext) {
        workflowInstanceService.makeWorkflowInstanceHumanProcessing(dagExecutionContext.getWorkflowInstance());
    }

    public boolean support(DagExecutionContext dagExecutionContext) {
        ActivityInstance failedActivityInstance = dagExecutionContext.getActivityInstance();
        ActivityDefinition activityDefinition = dagExecutionContext.getActivityDefinition();
        return ActivityState.FAILED == dagExecutionContext.getLastActivityState() && Objects.nonNull(failedActivityInstance)
               && failedActivityInstance.getRetryCount() >= activityDefinition.getMaxRetry() && Objects.equals(ActivityFailStrategy.HUMAN_PROCESSING,
                activityDefinition.getActivityFailStrategy());
    }
}
