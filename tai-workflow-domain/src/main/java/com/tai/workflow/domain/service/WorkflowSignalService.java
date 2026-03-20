package com.tai.workflow.domain.service;

import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.SignalAction;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.repository.entity.WorkflowSignalRecordEntity;
import com.tai.workflow.repository.persistence.WorkflowSignalRecordPersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowSignalService {
    private final WorkflowSignalRecordPersistence workflowSignalRecordPersistence;

    public boolean hasReceivedSignal(Long workflowInstanceId, String signalBizCode) {
        if (StringUtils.isBlank(signalBizCode)) {
            return true;
        } else {
            return workflowSignalRecordPersistence.existsByWorkflowInstanceIdAndSignalBizCode(workflowInstanceId, signalBizCode);
        }
    }

    public Long getReceivedSignalTime(Long workflowInstanceId, String signalBizCode) {
        if (StringUtils.isBlank(signalBizCode)) {
            return null;
        } else {
            WorkflowSignalRecordEntity workflowSignalRecordEntity = workflowSignalRecordPersistence.findByWorkflowInstanceIdAndSignalBizCode(
                    workflowInstanceId, signalBizCode);
            return Objects.isNull(workflowSignalRecordEntity) ? null : workflowSignalRecordEntity.getSignalTime().getTime();
        }
    }

    public boolean saveWorkflowSignalRecord(ActivityInstance activityInstance, String signalBizCode, Long delayMillis,
            boolean uniqueCheck) {
        Date signalTime;
        if (!Objects.isNull(delayMillis) && delayMillis > 0L) {
            signalTime = new Date(System.currentTimeMillis() + delayMillis);
        } else {
            signalTime = new Date(System.currentTimeMillis());
        }

        WorkflowSignalRecordEntity workflowSignalRecordEntity = WorkflowSignalRecordEntity.builder()
                .workflowInstanceId(activityInstance.getWorkflowInstanceId())
                .signalBizCode(signalBizCode)
                .signalTime(signalTime)
                .build();

        if (uniqueCheck) {
            try {
                workflowSignalRecordPersistence.insert(workflowSignalRecordEntity);
                return true;
            } catch (Throwable ignore) {
                return false;
            }
        } else {
            workflowSignalRecordPersistence.upsert(workflowSignalRecordEntity);
            return true;
        }
    }

    public void deleteSignalRecord(Long workflowInstanceId, String signalBizCode) {
        workflowSignalRecordPersistence.deleteByWorkflowInstanceIdAndSignalBizCode(workflowInstanceId, signalBizCode);
    }

    public Triple<ActivityState, ActivityExecutionResult, Map<String, Object>> processSignalResult(SignalAction signalAction,
            Map<String, Object> contextParams) {
        Map<String, Object> mergedContextParams = new HashMap<>();
        ActivityState activityStatus = ActivityState.COMPLETED;
        ActivityExecutionResult activityExecutionResult = null;
        if (Objects.nonNull(signalAction) && SignalAction.SUCCESS != signalAction) {
            if (SignalAction.TERMINATED == signalAction) {
                activityStatus = ActivityState.CANCELLED;
                activityExecutionResult = ActivityExecutionResult.ofFailed("SYSTEM CANCELLED");
            } else if (SignalAction.FAILED_AT_ONCE == signalAction) {
                activityStatus = ActivityState.FAILED;
                activityExecutionResult = ActivityExecutionResult.ofFailed("SYSTEM FORCE_ROLLBACK");
            } else if (SignalAction.FAILED_NORMAL == signalAction) {
                activityStatus = ActivityState.FAILED;
                activityExecutionResult = ActivityExecutionResult.ofFailed("SYSTEM FAILED_NORMAL");
            }
        } else {
            activityExecutionResult = ActivityExecutionResult.ofSucceeded();
        }

        if (MapUtils.isNotEmpty(contextParams)) {
            mergedContextParams.putAll(contextParams);
        }

        return Triple.of(activityStatus, activityExecutionResult, mergedContextParams);
    }
}
