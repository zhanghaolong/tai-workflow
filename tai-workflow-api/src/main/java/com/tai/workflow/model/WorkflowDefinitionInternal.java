package com.tai.workflow.model;

import com.tai.workflow.graph.WorkflowDag;
import com.tai.workflow.graph.WorkflowDagBuilder;
import com.tai.workflow.graph.WorkflowDagNodeFactory;
import com.tai.workflow.utils.CheckUtils;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
public class WorkflowDefinitionInternal {
    private Long id;
    private String namespace;
    private String name;
    private String displayName;
    private String description;
    private Map<String, Object> definitionVariables;
    private Set<String> mergeToParentContextKeys;
    private WorkflowDag workflowDag;
    private Date createTime;
    private Date updateTime;

    public static WorkflowDefinitionInternal toWorkflowDefinition(final WorkflowDefinition workflowDefinition) {
        CheckUtils.checkNotNull(workflowDefinition, WorkflowConstants.INVALID_PARAMETER, "workflowDefinition should not be null!");
        CheckUtils.checkNotEmpty(workflowDefinition.getName(), WorkflowConstants.INVALID_PARAMETER, "workflow name should not be empty!");
        CheckUtils.checkCondition(MapUtils.isNotEmpty(workflowDefinition.getNodeMap()), WorkflowConstants.INVALID_PARAMETER,
                "workflow nodeMap should not be empty!");
        WorkflowDagBuilder workflowDagBuilder = WorkflowDagBuilder.create();
        workflowDefinition.getNodeMap().values().stream().map(WorkflowDagNodeFactory::createWorkflowDagNode).forEach(workflowDagBuilder::addNode);
        workflowDefinition.getEdges().forEach((dagEdge) -> workflowDagBuilder.addEdge(dagEdge.getSourceNodeName(), dagEdge.getTargetNodeName()));
        WorkflowDag workflowDag = workflowDagBuilder.build();
        CheckUtils.checkCondition(!workflowDag.checkDagCycle(), WorkflowConstants.INVALID_PARAMETER, "DAG should not has cycle!");
        CheckUtils.checkCondition(!hasRepeatSignalCode(workflowDefinition.getNodeMap().values()), WorkflowConstants.INVALID_PARAMETER,
                "DAG should not has repeat signalBizCode!");
        WorkflowDefinitionInternal workflowDefinitionInternal = new WorkflowDefinitionInternal();
        BeanUtils.copyProperties(workflowDefinition, workflowDefinitionInternal, "nodeMap", "edges");
        workflowDefinitionInternal.setWorkflowDag(workflowDag);
        if (Objects.isNull(workflowDefinitionInternal.getDisplayName())) {
            workflowDefinitionInternal.setDisplayName(workflowDefinitionInternal.getName());
        }

        Map<String, Object> definitionVariables = new HashMap<>();
        if (MapUtils.isNotEmpty(workflowDefinitionInternal.getDefinitionVariables())) {
            definitionVariables.putAll(workflowDefinitionInternal.getDefinitionVariables());
        }

        if (CollectionUtils.isNotEmpty(workflowDefinitionInternal.getMergeToParentContextKeys())) {
            definitionVariables.put(WorkflowConstants.MERGE_TO_PARENT_CONTEXT_KEYS, workflowDefinitionInternal.getMergeToParentContextKeys());
        }

        workflowDefinitionInternal.setDefinitionVariables(definitionVariables);
        return workflowDefinitionInternal;
    }

    private static boolean hasRepeatSignalCode(final Collection<ActivityDefinition> activityDefinitions) {
        Set<String> signalBizCodeSet = new HashSet<>();
        for (ActivityDefinition activityDefinition : activityDefinitions) {
            if (StringUtils.isNotBlank(activityDefinition.getSignalBizCode())) {
                if (signalBizCodeSet.contains(activityDefinition.getSignalBizCode())) {
                    return true;
                }

                signalBizCodeSet.add(activityDefinition.getSignalBizCode());
            }
        } // end of for

        return false;
    }
}
