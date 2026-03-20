package com.tai.workflow.domain.executor.strategy;

import com.tai.workflow.domain.convert.WorkflowConvert;
import com.tai.workflow.domain.executor.DagExecutionContext;
import com.tai.workflow.domain.executor.DagExecutionStrategy;
import com.tai.workflow.domain.handler.WorkflowEventPublisher;
import com.tai.workflow.domain.service.ActivityInstanceService;
import com.tai.workflow.domain.service.WorkflowExecutionCoreService;
import com.tai.workflow.domain.service.WorkflowExecutionPool;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.repository.entity.WorkflowInstanceEntity;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import com.tai.workflow.repository.persistence.WorkflowInstancePersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Component
@RequiredArgsConstructor
public class RetryWhenFailureExecutionStrategyImpl implements DagExecutionStrategy {
    private final WorkflowExecutionCoreService workflowExecutionCoreService;
    private final WorkflowInstancePersistence workflowInstancePersistence;
    private final WorkflowConvert workflowConvert;
    private final ActivityInstanceService activityInstanceService;
    private final WorkflowExecutionPool workflowExecutionPool;

    public void execute(DagExecutionContext dagExecutionContext) {
        ActivityDefinition activityDefinition = dagExecutionContext.getActivityDefinition();
        ActivityInstance failedActivityInstance = dagExecutionContext.getActivityInstance();

        // 判断failedActivityInstance是否已经产生了子工作流 如果已经生成 则直接 retryWorkflowInstance
        List<WorkflowInstanceEntity> childWorkflowInstances = workflowInstancePersistence.findByParentIdAndParentActivityId(
                failedActivityInstance.getWorkflowInstanceId(),
                failedActivityInstance.getId());
        if (CollectionUtils.isEmpty(childWorkflowInstances)) {
            workflowExecutionCoreService.retryActivity(failedActivityInstance.getId(), failedActivityInstance.getInputContext(),
                    activityDefinition.getRetryIntervalMillis(), Boolean.TRUE);
        } else {
            UpdateActivityInstanceParam updateActivityInstanceParam = UpdateActivityInstanceParam.builder()
                    .id(failedActivityInstance.getId())
                    .retryCount(failedActivityInstance.getRetryCount() + 1)
                    .build();
            activityInstanceService.updateActivityInstance(updateActivityInstanceParam);
            childWorkflowInstances.stream()
                    .peek(childWorkflowInstance -> childWorkflowInstance.setState(WorkflowState.RUNNING.name()))
                    .forEach(childWorkflowInstance -> {
                        if (activityDefinition.getRetryIntervalMillis() <= 0) {
                            WorkflowEventPublisher.publishEvent(workflowConvert.convert(childWorkflowInstance));
                        } else {
                            workflowExecutionPool.getScheduleExecutor()
                                    .schedule(
                                            () -> WorkflowEventPublisher.publishEvent(workflowConvert.convert(childWorkflowInstance)),
                                            activityDefinition.getRetryIntervalMillis(), TimeUnit.MILLISECONDS);
                        }
                    });
        }
    }

    public boolean support(DagExecutionContext dagExecutionContext) {
        ActivityInstance failedActivityInstance = dagExecutionContext.getActivityInstance();
        ActivityDefinition activityDefinition = dagExecutionContext.getActivityDefinition();

        if (StringUtils.containsIgnoreCase(failedActivityInstance.getExecutionMsg(), "FORCE_ROLLBACK")) {
            return Boolean.FALSE;
        }

        return ActivityState.FAILED == dagExecutionContext.getLastActivityState()
               && failedActivityInstance.getRetryCount() < activityDefinition.getMaxRetry();
    }
}
