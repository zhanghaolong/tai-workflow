package com.tai.workflow.domain.factory;

import com.tai.workflow.domain.util.WorkflowUtils;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.WorkflowConstants;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zhanghaolong1989@163.com
 */
public class WorkflowInstanceFactory {
    public static WorkflowInstance generateWorkflowInstance(WorkflowDefinitionInternal workflowDefinitionInternal, Map<String, Object> contextParams,
            String token) {
        WorkflowInstance workflowInstance = new WorkflowInstance();
        workflowInstance.setToken(token);
        workflowInstance.setContextParams(Optional.ofNullable(contextParams).orElse(new HashMap<>()));
        workflowInstance.setState(WorkflowState.PENDING);
        workflowInstance.setNamespace(workflowDefinitionInternal.getNamespace());
        workflowInstance.setName(workflowDefinitionInternal.getName());
        workflowInstance.setDisplayName(workflowDefinitionInternal.getDisplayName());
        workflowInstance.setStartTime(new Date());
        workflowInstance.setWorkflowDefinitionId(workflowDefinitionInternal.getId());
        workflowInstance.setDefinitionVariables(workflowDefinitionInternal.getDefinitionVariables());

        workflowInstance.setWorkerNode(WorkflowUtils.getUniqueIdentity(workflowDefinitionInternal.getNamespace()));
        workflowInstance.setVersion(0);
        workflowInstance.setCreateTime(new Date());
        Optional<Pair<Long, Map<String, Object>>> parentWorkflowInstanceInfo = WorkflowUtils.extractParentWorkflowInstanceInfo();
        if (parentWorkflowInstanceInfo.isPresent()) {
            workflowInstance.setParentId(parentWorkflowInstanceInfo.get().getLeft());
            Long parentActivityId = (Long) parentWorkflowInstanceInfo.get().getRight().get(WorkflowConstants.PARENT_ACTIVITY_INSTANCE_ID);
            workflowInstance.setParentActivityId(parentActivityId);
            Map<String, Object> mergeContextParams = parentWorkflowInstanceInfo.get().getRight();
            if (MapUtils.isNotEmpty(mergeContextParams)) {
                mergeContextParams.putAll(workflowInstance.getContextParams());
                workflowInstance.setContextParams(mergeContextParams);
            }
        }

        if (Objects.nonNull(WorkflowUtils.getCurrentWorkflowContext())) {
            workflowInstance.setBizId(WorkflowUtils.getCurrentWorkflowContext().getWorkflowInstance().getBizId());
        }


        return workflowInstance;
    }
}
