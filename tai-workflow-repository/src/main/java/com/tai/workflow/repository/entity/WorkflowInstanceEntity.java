package com.tai.workflow.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.Date;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldNameConstants
public class WorkflowInstanceEntity {
    /**
     * 自增
     */
    private Long id;

    /**
     * 标识工作流实例Token
     */
    private String token;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 工作流定义 id
     */
    private Long workflowDefinitionId;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 工作流实例名称
     */
    private String name;

    /**
     * 展示名称
     */
    private String displayName;

    /**
     * 工作流实例状态
     */
    private String state;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 父工作流实例 id
     */
    private Long parentId;

    /**
     * 父工作流活动实例 id
     */
    private Long parentActivityId;

    /**
     * 流程实例context
     */
    private String context;

    /**
     * 定义的变量
     */
    private String definitionVariables;

    /**
     * 工作流被定义的节点身份 id
     */
    private String workerNode;

    /**
     * 乐观锁版本
     */
    private Integer version;

    /**
     * 业务id 一般为工作流所属的集群 id
     */
    private String bizId;
}
