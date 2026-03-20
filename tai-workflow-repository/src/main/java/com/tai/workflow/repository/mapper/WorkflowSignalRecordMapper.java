package com.tai.workflow.repository.mapper;

import com.tai.workflow.repository.entity.WorkflowSignalRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * WorkflowSignalRecord MyBatis Mapper
 *
 * @author zhanghaolong1989@163.com
 */
@Mapper
public interface WorkflowSignalRecordMapper {

    WorkflowSignalRecordEntity selectById(@Param("id") Long id);

    WorkflowSignalRecordEntity selectByWorkflowInstanceIdAndSignalBizCode(@Param("workflowInstanceId") Long workflowInstanceId, 
                                                                           @Param("signalBizCode") String signalBizCode);

    int existsByWorkflowInstanceIdAndSignalBizCode(@Param("workflowInstanceId") Long workflowInstanceId, 
                                                    @Param("signalBizCode") String signalBizCode);

    int insert(WorkflowSignalRecordEntity entity);

    int updateSignalTime(@Param("id") Long id, @Param("signalTime") Long signalTime);

    int upsert(@Param("workflowInstanceId") Long workflowInstanceId, 
               @Param("signalBizCode") String signalBizCode, 
               @Param("signalTime") Date signalTime);

    int deleteByWorkflowInstanceId(@Param("workflowInstanceId") Long workflowInstanceId);

    int deleteByWorkflowInstanceIdAndSignalBizCode(@Param("workflowInstanceId") Long workflowInstanceId, 
                                                    @Param("signalBizCode") String signalBizCode);
}
