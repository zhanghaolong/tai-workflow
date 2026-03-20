package com.tai.workflow.graph;

/**
 * @author zhanghaolong1989@163.com
 */
public final class WorkflowDagBuilder {
    private final WorkflowDag workflowDag = new WorkflowDag();

    private WorkflowDagBuilder() {
    }

    public static WorkflowDagBuilder create() {
        return new WorkflowDagBuilder();
    }

    public void addNode(WorkflowDagNode node) {
        this.workflowDag.addNode(node);
    }

    public void addEdge(String sourceNodeName, String targetNodeName) {
        this.workflowDag.addEdge(sourceNodeName, targetNodeName);
    }

    public WorkflowDag build() {
        return this.workflowDag;
    }
}
