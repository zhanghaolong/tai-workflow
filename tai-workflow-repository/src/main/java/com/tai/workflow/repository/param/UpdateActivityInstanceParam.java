package com.tai.workflow.repository.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateActivityInstanceParam {
    private Long id;
    private String expectedActivityState;
    private List<String> activityStatesCondition;
    private Integer retryCount;
    private String signalBizCode;
    private String executionMsg;
    private String inputContext;
    private String outputContext;
    private Date firstStartTime;
    private Date startTime;
    private Date endTime;
    private Date timeoutTime;
}
