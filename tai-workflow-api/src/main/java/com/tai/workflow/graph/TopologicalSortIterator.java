package com.tai.workflow.graph;

import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhanghaolong1989@163.com
 */
public class TopologicalSortIterator implements Iterator<WorkflowDagNode> {
    private final WorkflowDag workflowDag;
    private final Map<String, Integer> inDegreeInfo = new HashMap<>();
    private final LinkedList<String> queue;
    private final Boolean forwardTraverse;

    public TopologicalSortIterator(final WorkflowDag workflowDag, Boolean forwardTraverse) {
        this.forwardTraverse = forwardTraverse;
        this.workflowDag = workflowDag;
        this.getDagInDegree(workflowDag);
        this.queue = this.inDegreeInfo.entrySet()
                .stream()
                .filter((entry) -> entry.getValue() == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public boolean hasNext() {
        return !this.queue.isEmpty();
    }

    public WorkflowDagNode next() {
        if (this.queue.isEmpty()) {
            throw new NoSuchElementException("should not get called when queue become empty!");
        } else {
            String nodeName = this.queue.poll();
            Set<String> nextNodeNameSet = this.forwardTraverse ? this.workflowDag.getNode(nodeName).getOutgoingNodeNameSet()
                                                               : this.workflowDag.getNode(nodeName).getIncomingNodeNameSet();
            if (CollectionUtils.isNotEmpty(nextNodeNameSet)) {
                nextNodeNameSet.forEach(nextNodeName -> {
                    Integer afterMinusVal = this.inDegreeInfo.compute(nextNodeName, (__, value) -> value - 1);
                    if (afterMinusVal == 0) {
                        this.queue.offer(nextNodeName);
                    }
                });
            }

            return this.workflowDag.getNode(nodeName);
        }
    }

    private void getDagInDegree(final WorkflowDag workflowDag) {
        workflowDag.getNodeMap().forEach((nodeName, dagNode) -> {
            Set<String> nodeNameSet = this.forwardTraverse ? dagNode.getIncomingNodeNameSet() : dagNode.getOutgoingNodeNameSet();
            this.inDegreeInfo.put(nodeName, Optional.ofNullable(nodeNameSet).orElse(Set.of()).size());
        });
    }
}
