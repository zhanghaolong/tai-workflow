package com.tai.workflow.graph;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
public class WorkflowDag {
    private Map<String, WorkflowDagNode> nodeMap = new HashMap<>();
    private Set<WorkflowDagEdge> edges = new HashSet<>();

    public Stream<WorkflowDagNode> stream(Boolean forwardTraverse) {
        return StreamSupport.stream(Spliterators.spliterator(new TopologicalSortIterator(this, forwardTraverse), this.nodeMap.size(), 0), false);
    }

    public Stream<WorkflowDagNode> stream() {
        return this.stream(Boolean.TRUE);
    }

    public boolean checkDagCycle() {
        // should not be replaced to stream().count() for count would use size directly at some cases
        return this.stream().collect(Collectors.counting()) != this.getNodeMap().size();
    }

    public void addNode(WorkflowDagNode node) {
        if (this.nodeMap.containsKey(node.getName())) {
            throw new IllegalArgumentException("NodeName:" + node.getName() + " already exist");
        } else {
            this.nodeMap.put(node.getName(), node);
        }
    }

    public void addEdge(String sourceNodeName, String targetNodeName) {
        if (!this.nodeMap.containsKey(sourceNodeName)) {
            throw new IllegalArgumentException("sourceNodeName[" + sourceNodeName + "] is not exist");
        } else if (!this.nodeMap.containsKey(targetNodeName)) {
            throw new IllegalArgumentException("targetNodeName[" + targetNodeName + "] is not exist");
        } else {
            this.edges.add(WorkflowDagEdge.builder().sourceNodeName(sourceNodeName).targetNodeName(targetNodeName).build());
            this.nodeMap.get(sourceNodeName).addOutgoingNode(targetNodeName);
            this.nodeMap.get(targetNodeName).addIncomingNode(sourceNodeName);
        }
    }

    public Set<String> getRootNodeNames() {
        return this.nodeMap.values().stream().filter(WorkflowDagNode::isRoot).map(WorkflowDagNode::getName).collect(Collectors.toSet());
    }

    public Set<String> getLeafNodeNames() {
        return this.nodeMap.values().stream().filter(WorkflowDagNode::isLeaf).map(WorkflowDagNode::getName).collect(Collectors.toSet());
    }

    public WorkflowDagNode getNode(String nodeName) {
        return this.nodeMap.get(nodeName);
    }
}
