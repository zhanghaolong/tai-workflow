package com.tai.workflow.domain.executor.strategy;

import com.tai.workflow.domain.executor.DagExecutionContext;
import com.tai.workflow.domain.executor.DagExecutionStrategy;
import com.tai.workflow.domain.service.WorkflowDefinitionService;
import com.tai.workflow.domain.service.WorkflowExecutionCoreService;
import com.tai.workflow.domain.service.WorkflowExecutionPool;
import com.tai.workflow.graph.WorkflowDag;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author zhanghaolong1989@163.com
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@Component
@RequiredArgsConstructor
public class RootNodeExecutionStrategyImpl implements DagExecutionStrategy {
    private final WorkflowExecutionCoreService workflowExecutionCoreService;
    private final WorkflowDefinitionService workflowDefinitionService;
    private final WorkflowExecutionPool workflowExecutionPool;

    public void execute(DagExecutionContext dagExecutionContext) {
        WorkflowInstance workflowInstance = dagExecutionContext.getWorkflowInstance();
        WorkflowDefinitionInternal workflowDefinitionInternal = this.workflowDefinitionService.findWorkflowDefinition(workflowInstance);
        WorkflowDag workflowDag = workflowDefinitionInternal.getWorkflowDag();
        workflowDag.getRootNodeNames().forEach(rootNodeName -> workflowExecutionCoreService.runActivity(workflowInstance, rootNodeName, Boolean.FALSE, Boolean.FALSE));
    }

    public boolean support(DagExecutionContext dagExecutionContext) {
        return StringUtils.isBlank(dagExecutionContext.getLastActivityName());
    }
}
