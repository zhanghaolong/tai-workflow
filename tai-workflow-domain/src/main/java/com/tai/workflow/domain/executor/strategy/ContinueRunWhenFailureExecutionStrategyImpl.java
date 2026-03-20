package com.tai.workflow.domain.executor.strategy;

import com.tai.workflow.domain.executor.DagExecutionContext;
import com.tai.workflow.domain.executor.DagExecutionStrategy;
import com.tai.workflow.domain.service.ActivityInstanceService;
import com.tai.workflow.domain.service.WorkflowExecutionCoreService;
import com.tai.workflow.domain.service.WorkflowInstanceService;
import com.tai.workflow.domain.service.WorkflowSignalService;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
@Component
@RequiredArgsConstructor
public class ContinueRunWhenFailureExecutionStrategyImpl implements DagExecutionStrategy {
    private final WorkflowExecutionCoreService workflowExecutionCoreService;
    private final WorkflowInstanceService workflowInstanceService;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowSignalService workflowSignalService;

    public void execute(DagExecutionContext dagExecutionContext) {
        ActivityInstance activityInstance = dagExecutionContext.getActivityInstance();
        UpdateActivityInstanceParam updateActivityInstanceParam;
        if (StringUtils.isBlank(activityInstance.getSignalBizCode())) {
            updateActivityInstanceParam = UpdateActivityInstanceParam.builder()
                    .id(activityInstance.getId())
                    .expectedActivityState(ActivityState.SKIPPED.name())
                    .activityStatesCondition(List.of(ActivityState.FAILED.name()))
                    .endTime(new Date())
                    .build();
            activityInstanceService.updateActivityInstance(updateActivityInstanceParam);
            workflowExecutionCoreService.tryRunNextActivities(dagExecutionContext.getWorkflowInstance(), dagExecutionContext.getLastActivityName());
        } else if (!workflowSignalService.hasReceivedSignal(activityInstance.getWorkflowInstanceId(), activityInstance.getSignalBizCode())) {
            workflowInstanceService.makeActivitySignalWaiting(activityInstance.getId(), Map.of());
        }
    }

    public boolean support(DagExecutionContext dagExecutionContext) {
        ActivityInstance failedActivityInstance = dagExecutionContext.getActivityInstance();
        ActivityDefinition activityDefinition = dagExecutionContext.getActivityDefinition();
        return ActivityState.FAILED == dagExecutionContext.getLastActivityState() && Objects.nonNull(failedActivityInstance)
               && failedActivityInstance.getRetryCount() >= activityDefinition.getMaxRetry()
               && ActivityFailStrategy.CONTINUE_RUN == activityDefinition.getActivityFailStrategy();
    }
}
