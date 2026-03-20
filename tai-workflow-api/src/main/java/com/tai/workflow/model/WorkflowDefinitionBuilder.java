package com.tai.workflow.model;

import com.tai.workflow.graph.WorkflowDagEdge;
import com.tai.workflow.utils.CheckUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author zhanghaolong1989@163.com
 */
public final class WorkflowDefinitionBuilder {
    private final WorkflowDefinition workflowDefinition = new WorkflowDefinition();

    public WorkflowDefinitionBuilder addNode(ActivityDefinition activityDefinition) {
        CheckUtils.checkNotNull(activityDefinition, WorkflowConstants.INVALID_PARAMETER, "activityDefinition should not be null!");
        CheckUtils.checkCondition(!this.workflowDefinition.getNodeMap().containsKey(activityDefinition.getName()),
                WorkflowConstants.INVALID_PARAMETER, "activityName should be unique, activityName=" + activityDefinition.getName());
        workflowDefinition.getNodeMap().put(activityDefinition.getName(), activityDefinition);
        return this;
    }

    public WorkflowDefinitionBuilder addNode(Collection<? extends ActivityDefinition> activityDefinitions) {
        CheckUtils.checkNotEmpty(activityDefinitions, WorkflowConstants.INVALID_PARAMETER, "activityDefinitionCollection should not be empty!");
        activityDefinitions.forEach(this::addNode);
        return this;
    }

    public WorkflowDefinitionBuilder addEdge(String sourceNodeName, String targetNodeName) {
        CheckUtils.checkNotEmpty(sourceNodeName, WorkflowConstants.INVALID_PARAMETER, "sourceNodeName should not be empty!");
        CheckUtils.checkNotEmpty(targetNodeName, WorkflowConstants.INVALID_PARAMETER, "targetNodeName should not be empty!");
        workflowDefinition.getEdges().add(WorkflowDagEdge.builder().sourceNodeName(sourceNodeName).targetNodeName(targetNodeName).build());
        return this;
    }

    public WorkflowDefinitionBuilder addEdge(String sourceNodeName, Collection<String> targetNodeNameCollection) {
        CheckUtils.checkNotEmpty(targetNodeNameCollection, WorkflowConstants.INVALID_PARAMETER, "targetNodeNameCollection should not be empty!");
        targetNodeNameCollection.forEach((targetNodeName) -> this.addEdge(sourceNodeName, targetNodeName));
        return this;
    }

    public WorkflowDefinitionBuilder addEdge(Collection<String> sourceNodeNameCollection, String targetNodeName) {
        CheckUtils.checkNotEmpty(sourceNodeNameCollection, WorkflowConstants.INVALID_PARAMETER, "sourceNodeNameCollection should not be empty!");
        sourceNodeNameCollection.forEach((sourceNodeName) -> this.addEdge(sourceNodeName, targetNodeName));
        return this;
    }

    public WorkflowDefinitionBuilder addEdge(Collection<String> sourceNodeNameCollection, Collection<String> targetNodeNameCollection) {
        CheckUtils.checkNotEmpty(sourceNodeNameCollection, WorkflowConstants.INVALID_PARAMETER, "sourceNodeNameCollection should not be empty!");
        CheckUtils.checkNotEmpty(targetNodeNameCollection, WorkflowConstants.INVALID_PARAMETER, "targetNodeNameCollection should not be empty!");
        sourceNodeNameCollection.forEach((sourceNodeName) -> this.addEdge(sourceNodeName, targetNodeNameCollection));
        return this;
    }

    public WorkflowDefinitionBuilder name(String name) {
        workflowDefinition.setName(name);
        return this;
    }

    public WorkflowDefinitionBuilder displayName(String displayName) {
        workflowDefinition.setDisplayName(displayName);
        return this;
    }

    public WorkflowDefinitionBuilder description(String description) {
        workflowDefinition.setDescription(description);
        return this;
    }

    public WorkflowDefinitionBuilder timeMinutes(int timeMinutes) {
        workflowDefinition.setTimeoutMinutes(timeMinutes);
        return this;
    }

    public WorkflowDefinitionBuilder definitionVariables(final Map<String, Object> definitionVariables) {
        this.workflowDefinition.setDefinitionVariables(definitionVariables);
        return this;
    }

    public WorkflowDefinitionBuilder mergeToParentContextKeys(final Set<String> mergeToParentContextKeys) {
        this.workflowDefinition.getMergeToParentContextKeys().addAll(mergeToParentContextKeys);
        return this;
    }

    public WorkflowDefinition build() {
        return workflowDefinition;
    }
}
