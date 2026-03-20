package com.tai.workflow.domain.service;

import com.tai.workflow.domain.convert.WorkflowConvert;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.BriefActivityInstance;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.repository.entity.ActivityInstanceEntity;
import com.tai.workflow.repository.entity.ActivityInstanceSimpleEntity;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import com.tai.workflow.repository.persistence.ActivityInstancePersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
public class ActivityInstanceService {
    private final ActivityInstancePersistence activityInstancePersistence;
    private final WorkflowConvert activityInstanceConvert;

    public ActivityInstance findActivityInstance(Long workflowInstanceId, String activityName) {
        ActivityInstanceEntity activityInstanceEntity = activityInstancePersistence.findByWorkflowInstanceIdAndName(workflowInstanceId, activityName);
        return activityInstanceConvert.convert(activityInstanceEntity);
    }

    public ActivityInstance findActivityInstance(Long activityInstanceId) {
        ActivityInstanceEntity activityInstanceEntity = activityInstancePersistence.findByActivityInstanceId(activityInstanceId);
        return activityInstanceConvert.convert(activityInstanceEntity);
    }

    public int updateActivityInstance(UpdateActivityInstanceParam updateActivityInstanceParam) {
        if (CollectionUtils.isEmpty(updateActivityInstanceParam.getActivityStatesCondition())) {
            return activityInstancePersistence.updateActivityInstanceById(updateActivityInstanceParam);
        } else {
            return activityInstancePersistence.updateActivityInstanceByIdAndStates(updateActivityInstanceParam);
        }
    }

    /**
     * Batch insert activity instances using MyBatis batch insert
     */
    public void batchInsertActivityInstances(List<ActivityInstance> activityInstances) {
        if (CollectionUtils.isEmpty(activityInstances)) {
            return;
        }

        List<ActivityInstanceEntity> activityInstanceEntities = activityInstances.stream()
                .map(activityInstanceConvert::convert)
                .toList();
        activityInstancePersistence.batchUpsert(activityInstanceEntities);
    }

    public boolean workflowInstanceCompleted(Long workflowInstanceId) {
        int notCompleteActivityCount = activityInstancePersistence.countByWorkflowInstanceIdAndStateNotIn(workflowInstanceId,
                List.of(ActivityState.COMPLETED.name(), ActivityState.SKIPPED.name()));
        return notCompleteActivityCount == 0;
    }

    public boolean activityInstancesCompleted(Long workflowInstanceId, Set<String> activityNames) {
        int notCompleteActivityCount = activityInstancePersistence.countByWorkflowInstanceIdAndNameInAndStateIn(workflowInstanceId, activityNames,
                List.of(ActivityState.COMPLETED.name(), ActivityState.SKIPPED.name()));
        return notCompleteActivityCount == 0;
    }

    public List<ActivityInstance> findByWorkflowInstanceIdAndSignalBizCodeIn(Long workflowInstanceId, Collection<String> signalBizCodes) {
        List<ActivityInstanceEntity> activityInstanceEntities = activityInstancePersistence.findByWorkflowInstanceIdAndSignalBizCodeIn(
                workflowInstanceId, new ArrayList<>(new HashSet<>(signalBizCodes)));
        if (CollectionUtils.isEmpty(activityInstanceEntities)) {
            return List.of();
        }

        return activityInstanceEntities.stream().map(activityInstanceConvert::convert).collect(Collectors.toList());
    }

    public List<ActivityInstance> findByWorkflowInstanceId(Long workflowInstanceId) {
        List<ActivityInstanceEntity> activityInstanceEntities = activityInstancePersistence.findByWorkflowInstanceId(workflowInstanceId);
        if (CollectionUtils.isEmpty(activityInstanceEntities)) {
            return List.of();
        }

        return activityInstanceEntities.stream().map(activityInstanceConvert::convert).collect(Collectors.toList());
    }

    public ActivityInstance findByPrimaryId(Long id) {
        ActivityInstanceEntity entity = activityInstancePersistence.findByPrimaryId(id);
        if (Objects.isNull(entity)) {
            return null;
        }
        return activityInstanceConvert.convert(entity);
    }

    public void updateByWorkflowInstanceId(Long workflowInstanceId, String expectedActivityState, List<String> activityStatesCondition) {
        activityInstancePersistence.updateByWorkflowInstanceId(workflowInstanceId, expectedActivityState, activityStatesCondition,
                System.currentTimeMillis());
    }

    public List<ActivityInstance> listTimeoutActivityInstances(String namespace, long failOverCheckTimeSecRange) {
        long toTime = System.currentTimeMillis();
        long fromTime = toTime - failOverCheckTimeSecRange * WorkflowConstants.MILLIS_PER_SECOND;
        List<ActivityInstanceEntity> activityInstanceEntities = activityInstancePersistence.findByNamespaceAndTimeoutTimeBetweenAndStateIn(namespace,
                fromTime,
                toTime, List.of(ActivityState.PENDING.name(), ActivityState.RUNNING.name(), ActivityState.SIGNAL_WAITING.name()));
        if (CollectionUtils.isEmpty(activityInstanceEntities)) {
            return List.of();
        }

        return activityInstanceEntities.stream().map(activityInstanceConvert::convert).collect(Collectors.toList());
    }

    public void appendExecutionMsg(Long activityInstanceId, String executionMsg) {
        activityInstancePersistence.appendExecutionMsg(activityInstanceId, executionMsg);
    }

    public List<BriefActivityInstance> findBriefActivityInstancesByWorkflowInstanceId(Long workflowInstanceId) {
        List<ActivityInstanceSimpleEntity> activityInstanceSimpleEntities =
                activityInstancePersistence.findBriefActivityInstancesByWorkflowInstanceId(
                workflowInstanceId);
        if (CollectionUtils.isEmpty(activityInstanceSimpleEntities)) {
            return List.of();
        }

        return activityInstanceSimpleEntities.stream().map(activityInstanceConvert::toBriefActivityInstance).toList();
    }
}
