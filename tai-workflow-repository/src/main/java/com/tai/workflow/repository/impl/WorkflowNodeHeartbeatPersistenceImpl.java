package com.tai.workflow.repository.impl;

import com.tai.workflow.repository.entity.WorkflowNodeHeartbeatEntity;
import com.tai.workflow.repository.mapper.WorkflowNodeHeartbeatMapper;
import com.tai.workflow.repository.persistence.WorkflowNodeHeartbeatPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author zhanghaolong1989@163.com
 */
@Service
@RequiredArgsConstructor
public class WorkflowNodeHeartbeatPersistenceImpl implements WorkflowNodeHeartbeatPersistence {
    private final WorkflowNodeHeartbeatMapper mapper;

    @Override
    public void save(WorkflowNodeHeartbeatEntity workflowNodeHeartbeatEntity) {
        mapper.insert(workflowNodeHeartbeatEntity);
    }

    @Override
    public void deleteByNamespaceAndWorkerNodeIn(String namespace, List<String> offlineWorkerNodes) {
        mapper.deleteByNamespaceAndWorkerNodeIn(namespace, offlineWorkerNodes);
    }

    @Override
    public List<WorkflowNodeHeartbeatEntity> getOnlineWorkerNodeList(String namespace, Date timeThreshold) {
        return mapper.selectByNamespaceAndHeartbeatTimeGreaterThan(namespace, timeThreshold);
    }

    @Override
    public List<WorkflowNodeHeartbeatEntity> getOfflineWorkerNodeList(String namespace, Date timeThreshold) {
        return mapper.selectByNamespaceAndHeartbeatTimeLessThanEqual(namespace, timeThreshold);
    }

    @Override
    public WorkflowNodeHeartbeatEntity findByNamespaceAndWorkerNode(String namespace, String selfNodeId) {
        return mapper.selectByNamespaceAndWorkerNode(namespace, selfNodeId);
    }

    @Override
    public void updateHeartbeatTime(String namespace, String selfNodeId, Date heartbeatTime) {
        mapper.updateHeartbeatTime(namespace, selfNodeId, heartbeatTime);
    }
}
