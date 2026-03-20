package com.tai.workflow.repository.impl;

import com.tai.workflow.repository.entity.ActivityInstanceEntity;
import com.tai.workflow.repository.entity.ActivityInstanceSimpleEntity;
import com.tai.workflow.repository.mapper.ActivityInstanceMapper;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import com.tai.workflow.repository.persistence.ActivityInstancePersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author zhanghaolong1989@163.com
 */
@Service
@RequiredArgsConstructor
public class ActivityInstancePersistenceImpl implements ActivityInstancePersistence {
    private final ActivityInstanceMapper mapper;

    @Override
    public ActivityInstanceEntity findByActivityInstanceId(Long activityInstanceId) {
        return mapper.selectById(activityInstanceId);
    }

    @Override
    public ActivityInstanceEntity findByWorkflowInstanceIdAndName(Long workflowInstanceId, String activityName) {
        return mapper.selectByWorkflowInstanceIdAndName(workflowInstanceId, activityName);
    }

    @Override
    public void batchUpsert(List<ActivityInstanceEntity> activityInstanceEntities) {
        for (ActivityInstanceEntity entity : activityInstanceEntities) {
            if (entity.getId() == null) {
                entity.setCreateTime(new Date());
                mapper.insert(entity);
            } else {
                // Update existing entity via updateActivityInstanceById
                UpdateActivityInstanceParam param = UpdateActivityInstanceParam.builder()
                        .id(entity.getId())
                        .expectedActivityState(entity.getState())
                        .startTime(entity.getStartTime())
                        .endTime(entity.getEndTime())
                        .inputContext(entity.getInputContext())
                        .outputContext(entity.getOutputContext())
                        .build();
                mapper.updateActivityInstanceById(param);
            }
        }
    }

    @Override
    public int countByWorkflowInstanceIdAndStateNotIn(Long workflowInstanceId, List<String> states) {
        return mapper.countByWorkflowInstanceIdAndStateNotIn(workflowInstanceId, states);
    }

    @Override
    public int countByWorkflowInstanceIdAndNameInAndStateIn(Long workflowInstanceId, Set<String> activityNames, List<String> states) {
        return mapper.countByWorkflowInstanceIdAndNameInAndStateNotIn(workflowInstanceId, activityNames, states);
    }

    @Override
    public List<ActivityInstanceEntity> findByWorkflowInstanceIdAndSignalBizCodeIn(Long workflowInstanceId, List<String> signalBizCodes) {
        return mapper.selectByWorkflowInstanceIdAndSignalBizCodeIn(workflowInstanceId, signalBizCodes);
    }

    @Override
    public List<ActivityInstanceEntity> findByWorkflowInstanceId(Long workflowInstanceId) {
        return mapper.selectByWorkflowInstanceId(workflowInstanceId);
    }

    @Override
    public List<ActivityInstanceSimpleEntity> findBriefActivityInstancesByWorkflowInstanceId(Long workflowInstanceId) {
        return mapper.selectBriefByWorkflowInstanceId(workflowInstanceId);
    }

    @Override
    public ActivityInstanceEntity findByPrimaryId(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public void appendExecutionMsg(Long activityInstanceId, String executionMsg) {
        mapper.appendExecutionMsg(activityInstanceId, executionMsg);
    }

    @Override
    public int updateActivityInstanceById(UpdateActivityInstanceParam updateActivityInstanceParam) {
        return mapper.updateActivityInstanceById(updateActivityInstanceParam);
    }

    @Override
    public int updateActivityInstanceByIdAndStates(UpdateActivityInstanceParam updateActivityInstanceParam) {
        return mapper.updateActivityInstanceByIdAndState(updateActivityInstanceParam);
    }

    @Override
    public void updateByWorkflowInstanceId(Long workflowInstanceId, String expectedActivityState, List<String> activityStatesCondition,
            Long updateTime) {
        mapper.updateByWorkflowInstanceId(workflowInstanceId, expectedActivityState, activityStatesCondition);
    }

    @Override
    public List<ActivityInstanceEntity> findByNamespaceAndTimeoutTimeBetweenAndStateIn(String namespace, Long fromTime, Long toTime,
            List<String> activityStates) {
        return mapper.selectByNamespaceAndTimeoutTimeBetweenAndStateIn(namespace, fromTime, toTime, activityStates);
    }

}
