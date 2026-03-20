package com.tai.workflow.model;

import com.tai.workflow.enums.ActivityState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BriefActivityInstance {
    private Long id;
    private String name;
    private Long workflowInstanceId;
    private ActivityState state;
    private String signalBizCode;
}
