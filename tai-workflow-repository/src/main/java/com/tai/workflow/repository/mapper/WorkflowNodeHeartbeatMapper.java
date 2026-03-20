package com.tai.workflow.repository.mapper;

import com.tai.workflow.repository.entity.WorkflowNodeHeartbeatEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * WorkflowNodeHeartbeat MyBatis Mapper
 *
 * @author zhanghaolong1989@163.com
 */
@Mapper
public interface WorkflowNodeHeartbeatMapper {

    WorkflowNodeHeartbeatEntity selectById(@Param("id") Long id);

    WorkflowNodeHeartbeatEntity selectByNamespaceAndWorkerNode(@Param("namespace") String namespace, 
                                                                @Param("workerNode") String workerNode);

    List<WorkflowNodeHeartbeatEntity> selectByNamespaceAndHeartbeatTimeLessThanEqual(@Param("namespace") String namespace, 
                                                                                      @Param("timeThreshold") Date timeThreshold);

    List<WorkflowNodeHeartbeatEntity> selectByNamespaceAndHeartbeatTimeGreaterThan(@Param("namespace") String namespace, 
                                                                                    @Param("timeThreshold") Date timeThreshold);

    int insert(WorkflowNodeHeartbeatEntity entity);

    int updateHeartbeatTime(@Param("namespace") String namespace, 
                            @Param("workerNode") String workerNode, 
                            @Param("heartbeatTime") Date heartbeatTime);

    int deleteByNamespaceAndWorkerNodeIn(@Param("namespace") String namespace, 
                                          @Param("workerNodes") List<String> workerNodes);
}
