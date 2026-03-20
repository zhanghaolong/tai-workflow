package com.tai.workflow.repository.mapper;

import com.tai.workflow.repository.entity.ActivityInstanceEntity;
import com.tai.workflow.repository.entity.ActivityInstanceSimpleEntity;
import com.tai.workflow.repository.param.ListActivityInstancesParam;
import com.tai.workflow.repository.param.UpdateActivityInstanceParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * ActivityInstance MyBatis Mapper
 *
 * @author zhanghaolong1989@163.com
 */
@Mapper
public interface ActivityInstanceMapper {

    ActivityInstanceEntity selectById(@Param("id") Long id);

    ActivityInstanceEntity selectByWorkflowInstanceIdAndName(@Param("workflowInstanceId") Long workflowInstanceId, 
                                                              @Param("name") String name);

    List<ActivityInstanceEntity> selectByWorkflowInstanceId(@Param("workflowInstanceId") Long workflowInstanceId);

    List<ActivityInstanceSimpleEntity> selectBriefByWorkflowInstanceId(@Param("workflowInstanceId") Long workflowInstanceId);

    List<ActivityInstanceEntity> selectByWorkflowInstanceIdAndSignalBizCodeIn(@Param("workflowInstanceId") Long workflowInstanceId, 
                                                                               @Param("signalBizCodes") List<String> signalBizCodes);

    int countByWorkflowInstanceIdAndStateNotIn(@Param("workflowInstanceId") Long workflowInstanceId, 
                                                @Param("states") List<String> states);

    int countByWorkflowInstanceIdAndNameInAndStateNotIn(@Param("workflowInstanceId") Long workflowInstanceId, 
                                                         @Param("names") Set<String> names, 
                                                         @Param("states") List<String> states);

    List<ActivityInstanceEntity> selectByNamespaceAndTimeoutTimeBetweenAndStateIn(
            @Param("namespace") String namespace,
            @Param("fromTime") Long fromTime,
            @Param("toTime") Long toTime,
            @Param("states") List<String> states);

    List<ActivityInstanceSimpleEntity> selectPageByCondition(ListActivityInstancesParam param);

    int insert(ActivityInstanceEntity entity);

    int updateActivityInstanceById(UpdateActivityInstanceParam param);

    int updateActivityInstanceByIdAndState(UpdateActivityInstanceParam param);

    int updateByWorkflowInstanceId(@Param("workflowInstanceId") Long workflowInstanceId,
                                    @Param("expectedActivityState") String expectedActivityState,
                                    @Param("activityStatesCondition") List<String> activityStatesCondition);

    int appendExecutionMsg(@Param("id") Long id, @Param("executionMsg") String executionMsg);
}
