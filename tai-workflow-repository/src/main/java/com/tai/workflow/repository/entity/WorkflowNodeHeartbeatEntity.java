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
public class WorkflowNodeHeartbeatEntity {
    /**
     * 自增
     */
    private Long id;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * worker 的身份信息
     */
    private String workerNode;

    /**
     * 信号通知时间
     */
    private Date heartbeatTime;
}
