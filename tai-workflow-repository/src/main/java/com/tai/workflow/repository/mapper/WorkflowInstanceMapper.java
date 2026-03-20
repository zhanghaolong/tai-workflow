package com.tai.workflow.repository.mapper;

import com.tai.workflow.repository.entity.WorkflowInstanceEntity;
import com.tai.workflow.repository.param.ListWorkflowInstancesParam;
import com.tai.workflow.repository.param.UpdateWorkflowInstanceParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * WorkflowInstance MyBatis Mapper
 *
 * @author zhanghaolong1989@163.com
 */
@Mapper
public interface WorkflowInstanceMapper {

    WorkflowInstanceEntity selectById(@Param("id") Long id);

    WorkflowInstanceEntity selectByNamespaceAndToken(@Param("namespace") String namespace, @Param("token") String token);

    WorkflowInstanceEntity selectByToken(@Param("namespace") String namespace, @Param("token") String token);

    List<WorkflowInstanceEntity> selectByParentId(@Param("parentId") Long parentId);

    List<WorkflowInstanceEntity> selectByParentIdAndParentActivityId(@Param("parentId") Long parentId, @Param("parentActivityId") Long parentActivityId);

    List<WorkflowInstanceEntity> selectByNameAndStateAndEndTimeBetween(@Param("name") String name,
                                                                        @Param("state") String state, 
                                                                        @Param("fromTime") Long fromTime, 
                                                                        @Param("toTime") Long toTime);

    List<WorkflowInstanceEntity> selectByNamespaceAndStartTimeGreaterThanAndWorkerNodeNotInAndStateIn(
            @Param("namespace") String namespace,
            @Param("fromTime") Long fromTime,
            @Param("workerNodeList") List<String> workerNodeList,
            @Param("states") List<String> states);

    List<WorkflowInstanceEntity> selectByNamespaceAndStartTimeGreaterThanAndStateInAndDefinitionVariablesLike(
            @Param("namespace") String namespace,
            @Param("startTime") Long startTime,
            @Param("states") List<String> states,
            @Param("definitionVariables") String definitionVariables);

    int insert(WorkflowInstanceEntity entity);

    int updateWorkflowInstance(UpdateWorkflowInstanceParam param);

    int updateWorkflowInstanceWithOptimisticLock(UpdateWorkflowInstanceParam param);

    List<WorkflowInstanceEntity> selectWorkflowInstances(ListWorkflowInstancesParam param);

    /**
     * 根据 namespace 统计工作流实例数量
     *
     * @param namespace 命名空间
     * @return 工作流实例数量
     */
    Long countByNamespace(@Param("namespace") String namespace);
}
