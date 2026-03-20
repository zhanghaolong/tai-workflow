package com.tai.workflow.domain.executor.strategy;

import com.tai.workflow.domain.executor.DagExecutionContext;
import com.tai.workflow.domain.executor.DagExecutionStrategy;
import com.tai.workflow.domain.service.WorkflowRollbackService;
import com.tai.workflow.domain.util.LoggerUtils;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * @author zhanghaolong1989@163.com
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
@Component
@RequiredArgsConstructor
public class RollbackWhenFailureExecutionStrategyImpl implements DagExecutionStrategy {
    private final WorkflowRollbackService workflowRollbackService;

    public void execute(DagExecutionContext dagExecutionContext) {
        log.info("RollbackWhenFailureExecutionStrategyImpl workflow rollback started for:{}",
                LoggerUtils.getTraceHeader(dagExecutionContext.getWorkflowInstance(), dagExecutionContext.getActivityInstance()));
        Set<String> mustRollbackActivityNameSet = new HashSet<>();
        mustRollbackActivityNameSet.add(dagExecutionContext.getLastActivityName());
        workflowRollbackService.rollbackWorkflowInstance(dagExecutionContext.getWorkflowInstance().getId(), null,
                dagExecutionContext.getWorkflowInstance().getContextParams(), mustRollbackActivityNameSet);
        log.info("RollbackWhenFailureExecutionStrategyImpl workflow rollback completed for: [{}]",
                LoggerUtils.getTraceHeader(dagExecutionContext.getWorkflowInstance(), dagExecutionContext.getActivityInstance()));
    }

    public boolean support(DagExecutionContext dagExecutionContext) {
        ActivityInstance failedActivityInstance = dagExecutionContext.getActivityInstance();
        ActivityDefinition activityDefinition = dagExecutionContext.getActivityDefinition();

        if (StringUtils.containsIgnoreCase(failedActivityInstance.getExecutionMsg(), "FORCE_ROLLBACK")) {
            return Boolean.TRUE;
        }

        return ActivityState.FAILED == dagExecutionContext.getLastActivityState()
               && ActivityFailStrategy.ROLLBACK == activityDefinition.getActivityFailStrategy()
               && failedActivityInstance.getRetryCount() >= activityDefinition.getMaxRetry();
    }
}
