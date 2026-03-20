package com.tai.workflow.model;

import com.tai.workflow.enums.WorkflowState;
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
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstance {
    private Long id;
    private String token;
    private Long parentId;
    private Long parentActivityId;
    private String namespace;
    private String name;
    private String displayName;
    private Long workflowDefinitionId;
    private WorkflowState state;
    private Date startTime;
    private Date endTime;
    private String workerNode;
    private Map<String, Object> contextParams;
    private Map<String, Object> definitionVariables;
    private Integer version;
    private String bizId;
    private Date createTime;
    private Date updateTime;
}
