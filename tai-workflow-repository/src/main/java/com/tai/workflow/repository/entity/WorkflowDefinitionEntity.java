package com.tai.workflow.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.Date;

/**
 * @author zhanghaolong1989@163.com
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldNameConstants
public class WorkflowDefinitionEntity {
    /**
     * 自增
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 工作流定义名称
     */
    private String name;

    /**
     * 展示名称
     */
    private String displayName;

    /**
     * 工作流定义描述
     */
    private String description;

    /**
     * 工作流被定义的节点身份 id
     */
    private String workerNode;

    /**
     * 定义的初始化变量
     */
    private String variables;

    /**
     * 存储流程的DAG图
     */
    private String dag;
}
