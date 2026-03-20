package com.tai.workflow.model;

import com.tai.workflow.graph.WorkflowDagEdge;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author shenwangyan
 */
@Data
@NoArgsConstructor
public class WorkflowDefinition {
    private String name;
    private Long version;
    private String displayName;
    private String description;
    private Map<String, Object> definitionVariables;
    private Map<String, ActivityDefinition> nodeMap = new HashMap<>(0);
    private Set<WorkflowDagEdge> edges = new HashSet<>(0);
    private Set<String> mergeToParentContextKeys = new HashSet<>(0);
    private int timeoutMinutes;

    public static WorkflowDefinitionBuilder builder() {
        return new WorkflowDefinitionBuilder();
    }
}
