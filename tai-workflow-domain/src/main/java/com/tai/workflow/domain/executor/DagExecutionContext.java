package com.tai.workflow.domain.executor;

import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowInstance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Data
public class DagExecutionContext {
    private WorkflowInstance workflowInstance;
    private ActivityDefinition activityDefinition;
    private ActivityInstance activityInstance;
    private String lastActivityName;
    private ActivityState lastActivityState;
}
