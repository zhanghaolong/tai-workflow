package com.tai.workflow.model;

import com.tai.workflow.enums.ActivityState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityInstance {
    private static final Long MILLIS_PER_SECOND = 1_000L;
    private Long id;
    private String namespace;
    private String name;
    private String displayName;
    private Long workflowInstanceId;
    private ActivityState state;
    private Date firstStartTime;
    private Date startTime;
    private Date endTime;
    private Date timeoutTime;
    private Integer retryCount;
    private String signalBizCode;
    private String executionMsg;
    private Map<String, Object> inputContext;
    private Map<String, Object> outputContext;
    private Date createTime;
    private Date updateTime;
}
