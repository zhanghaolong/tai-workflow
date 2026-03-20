package com.tai.workflow.repository.persistence;

import com.tai.workflow.repository.entity.ActivityInstanceEntity;
import com.tai.workflow.repository.entity.ActivityInstanceSimpleEntity;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;

import java.util.List;
import java.util.Set;

/**
 * @author zhanghaolong1989@163.com
 */
public interface ActivityInstancePersistence {
    ActivityInstanceEntity findByActivityInstanceId(Long activityInstanceId);

    ActivityInstanceEntity findByWorkflowInstanceIdAndName(Long workflowInstanceId, String activityName);

    void batchUpsert(List<ActivityInstanceEntity> activityInstanceEntities);

    int countByWorkflowInstanceIdAndStateNotIn(Long workflowInstanceId, List<String> states);

    int countByWorkflowInstanceIdAndNameInAndStateIn(Long workflowInstanceId, Set<String> activityNames, List<String> name);

    List<ActivityInstanceEntity> findByWorkflowInstanceIdAndSignalBizCodeIn(Long workflowInstanceId, List<String> signalBizCodes);

    List<ActivityInstanceEntity> findByWorkflowInstanceId(Long workflowInstanceId);

    int updateActivityInstanceById(UpdateActivityInstanceParam updateActivityInstanceParam);

    int updateActivityInstanceByIdAndStates(UpdateActivityInstanceParam updateActivityInstanceParam);

    void updateByWorkflowInstanceId(Long workflowInstanceId, String expectedActivityState, List<String> activityStatesCondition, Long updateTime);

    List<ActivityInstanceEntity> findByNamespaceAndTimeoutTimeBetweenAndStateIn(String namespace, Long fromTime, Long toTime,
            List<String> activityStates);

    ActivityInstanceEntity findByPrimaryId(Long id);

    void appendExecutionMsg(Long activityInstanceId, String executionMsg);

    List<ActivityInstanceSimpleEntity> findBriefActivityInstancesByWorkflowInstanceId(Long workflowInstanceId);
}
