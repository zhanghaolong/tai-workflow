package com.tai.workflow.repository.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateWorkflowInstanceParam {
    private Long id;
    private String bizId;
    private String state;
    private Date endTime;
    private String context;
    private Integer version;
}
