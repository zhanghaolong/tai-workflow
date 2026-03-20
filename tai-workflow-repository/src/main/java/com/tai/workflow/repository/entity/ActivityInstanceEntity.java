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
public class ActivityInstanceEntity {
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
     * 工作流实例 id
     */
    private Long workflowInstanceId;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 工作流活动实例名称
     */
    private String name;

    /**
     * 工作流活动实例名称
     */
    private String displayName;

    /**
     * 工作流活动实例状态
     */
    private String state;

    /**
     * 工作流活动实例前置状态 方便排查问题
     */
    private String prevState;

    /**
     * 执行过程的信息 主要是异常栈
     */
    private String executionMsg;

    /**
     * 上次执行过程的信息 主要是异常栈
     */
    private String prevExecutionMsg;

    /**
     * 第一次开始时间
     */
    private Date firstStartTime;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 超时时间
     */
    private Date timeoutTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 输入的 Context
     */
    private String inputContext;

    /**
     * 输出的 Context
     */
    private String outputContext;

    /**
     * 信号 code
     */
    private String signalBizCode;
}
