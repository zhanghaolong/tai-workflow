package com.tai.workflow.repository.persistence;

import com.tai.workflow.repository.entity.WorkflowNodeHeartbeatEntity;

import java.util.Date;
import java.util.List;

/**
 * @author zhanghaolong1989@163.com
 */
public interface WorkflowNodeHeartbeatPersistence {
    void save(WorkflowNodeHeartbeatEntity workflowNodeHeartbeatEntity);

    void deleteByNamespaceAndWorkerNodeIn(String namespace, List<String> offlineWorkerNodes);

    List<WorkflowNodeHeartbeatEntity> getOnlineWorkerNodeList(String namespace, Date timeThreshold);

    List<WorkflowNodeHeartbeatEntity> getOfflineWorkerNodeList(String namespace, Date timeThreshold);

    WorkflowNodeHeartbeatEntity findByNamespaceAndWorkerNode(String namespace, String selfNodeId);

    void updateHeartbeatTime(String namespace, String selfNodeId, Date heartbeatTime);
}
