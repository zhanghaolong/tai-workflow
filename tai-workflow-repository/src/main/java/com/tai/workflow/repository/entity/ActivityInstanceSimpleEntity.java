package com.tai.workflow.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.Date;

/**
 * 去掉里面大字段，防止里面数据过大导致mysql排序加分页报错Out of sort memory, consider increasing server sort buffer size
 *
 * @author zhanghaolong1989@163.com
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldNameConstants
public class ActivityInstanceSimpleEntity {
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
     * 信号 code
     */
    private String signalBizCode;

    public ActivityInstanceSimpleEntity(Long id, Long workflowInstanceId, String name, String state, String signalBizCode) {
        this.id = id;
        this.workflowInstanceId = workflowInstanceId;
        this.name = name;
        this.state = state;
        this.signalBizCode = signalBizCode;
    }
}
