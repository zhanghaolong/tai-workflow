package com.tai.workflow.repository.mapper;

import com.tai.workflow.repository.entity.WorkflowNodeLeaderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * WorkflowNodeLeader MyBatis Mapper
 *
 * @author zhanghaolong1989@163.com
 */
@Mapper
public interface WorkflowNodeLeaderMapper {

    WorkflowNodeLeaderEntity selectById(@Param("id") Long id);

    WorkflowNodeLeaderEntity selectByNamespace(@Param("namespace") String namespace);

    int insert(WorkflowNodeLeaderEntity entity);

    int updateLeaderRefreshTime(@Param("namespace") String namespace, 
                                 @Param("leaderNode") String leaderNode, 
                                 @Param("refreshTime") Date refreshTime);

    int deleteByNamespaceAndRefreshTimeLessThan(@Param("namespace") String namespace, 
                                                 @Param("timeThreshold") Date timeThreshold);
}
