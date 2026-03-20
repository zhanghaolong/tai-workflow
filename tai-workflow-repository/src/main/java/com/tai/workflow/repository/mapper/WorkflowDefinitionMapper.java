package com.tai.workflow.repository.mapper;

import com.tai.workflow.repository.entity.WorkflowDefinitionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * WorkflowDefinition MyBatis Mapper
 *
 * @author zhanghaolong1989@163.com
 */
@Mapper
public interface WorkflowDefinitionMapper {

    WorkflowDefinitionEntity selectById(@Param("id") Long id);

    WorkflowDefinitionEntity selectTopByNamespaceAndNameAndWorkerNode(@Param("namespace") String namespace, 
                                                                       @Param("name") String name, 
                                                                       @Param("workerNode") String workerNode);
    int insert(WorkflowDefinitionEntity entity);

    int update(WorkflowDefinitionEntity entity);
}
