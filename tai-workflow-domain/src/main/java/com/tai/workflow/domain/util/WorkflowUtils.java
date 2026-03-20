package com.tai.workflow.domain.util;

import com.tai.workflow.domain.executor.ActivityRunner;
import com.tai.workflow.graph.WorkflowDag;
import com.tai.workflow.graph.WorkflowDagNode;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.BriefActivityInstance;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
public class WorkflowUtils {
    private static volatile String identity = null;

    public static WorkflowContext getCurrentWorkflowContext() {
        return ActivityRunner.WORKFLOW_CONTEXT_THREAD_LOCAL.get();
    }

    public static Optional<Pair<Long, Map<String, Object>>> extractParentWorkflowInstanceInfo() {
        WorkflowContext workflowContext = getCurrentWorkflowContext();
        if (Objects.isNull(workflowContext)) {
            return Optional.empty();
        } else {
            ActivityDefinition activityDefinition = workflowContext.getActivityDefinition();
            if (BooleanUtils.isTrue(activityDefinition.isIsolate())) {
                return Optional.empty();
            }

            Long parentWorkflowInstanceId = workflowContext.getWorkflowInstance().getId();
            String parentWorkflowInstanceToken = workflowContext.getWorkflowInstance().getToken();
            Map<String, Object> executionContextParam = new HashMap<>();
            executionContextParam.put(WorkflowConstants.PARENT_WORKFLOW_INSTANCE_ID, parentWorkflowInstanceId);
            executionContextParam.put(WorkflowConstants.PARENT_WORKFLOW_INSTANCE_TOKEN, parentWorkflowInstanceToken);
            executionContextParam.put(WorkflowConstants.PARENT_ACTIVITY_INSTANCE_ID, workflowContext.getActivityInstance().getId());
            executionContextParam.put(WorkflowConstants.PARENT_ACTIVITY_INSTANCE_NAME, workflowContext.getActivityInstance().getName());
            return Optional.of(Pair.of(parentWorkflowInstanceId, executionContextParam));
        }
    }

    public static Set<String> getChildrenActivityNames(WorkflowDag workflowDag, String startActivityName, boolean forwardTraverse) {
        Set<String> children = new HashSet<>();
        children.add(startActivityName);
        WorkflowDagNode node = workflowDag.getNode(startActivityName);
        getChildren(workflowDag, node, children, forwardTraverse);
        return children;
    }

    public static Map<String, BriefActivityInstance> regulateActivityInstance(final List<? extends BriefActivityInstance> activityInstances) {
        return activityInstances.stream().collect(Collectors.toMap(BriefActivityInstance::getName, Function.identity()));
    }

    public static Set<String> getChildrenActivityNames(WorkflowDag workflowDag, String startActivityName) {
        Set<String> children = new LinkedHashSet<>();
        children.add(startActivityName);
        WorkflowDagNode node = workflowDag.getNode(startActivityName);
        getChildren(workflowDag, node, children, Boolean.TRUE);
        return children;
    }

    private static void getChildren(WorkflowDag workflowDag, WorkflowDagNode node, Set<String> children, boolean forwardTraverse) {
        Set<String> nextNameSet = forwardTraverse ? node.getOutgoingNodeNameSet() : node.getIncomingNodeNameSet();
        if (!CollectionUtils.isEmpty(nextNameSet)) {
            children.addAll(nextNameSet);
            nextNameSet.forEach(nextNodeName -> {
                WorkflowDagNode nextNode = workflowDag.getNode(nextNodeName);
                getChildren(workflowDag, nextNode, children, forwardTraverse);
            });
        }
    }

    public static String getUniqueIdentity(String namespace) {
        if (Objects.nonNull(identity)) {
            return identity;
        }

        identity = IpUtils.getLocalIp4Address().orElse(StringUtils.EMPTY) + WorkflowConstants.DASH + namespace + WorkflowConstants.DASH
                   + UUID.randomUUID();
        return identity;
    }
}
