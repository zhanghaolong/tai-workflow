package com.tai.workflow.graph;

import com.tai.workflow.model.ActivityDefinition;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
public class WorkflowDagNode {
    private ActivityDefinition activityDefinition;
    private String name;
    private Set<String> incomingNodeNameSet;
    private Set<String> outgoingNodeNameSet;

    public boolean isRoot() {
        return CollectionUtils.isEmpty(this.incomingNodeNameSet);
    }

    public boolean isLeaf() {
        return CollectionUtils.isEmpty(this.outgoingNodeNameSet);
    }

    public void addIncomingNode(String nodeName) {
        if (Objects.isNull(this.incomingNodeNameSet)) {
            this.incomingNodeNameSet = new HashSet<>();
        }

        this.incomingNodeNameSet.add(nodeName);
    }

    public void addOutgoingNode(String nodeName) {
        if (Objects.isNull(this.outgoingNodeNameSet)) {
            this.outgoingNodeNameSet = new HashSet<>();
        }

        this.outgoingNodeNameSet.add(nodeName);
    }
}
