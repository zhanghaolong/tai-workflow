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
public class WorkflowSignalRecordEntity {
    /**
     * 自增
     */
    private Long id;

    /**
     * 工作流实例 id
     */
    private Long workflowInstanceId;

    /**
     * 信号业务 code
     */
    private String signalBizCode;

    /**
     * 信号通知时间
     */
    private Date signalTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
