package com.tai.workflow.repository.impl;

import com.tai.workflow.repository.entity.WorkflowNodeLeaderEntity;
import com.tai.workflow.repository.mapper.WorkflowNodeLeaderMapper;
import com.tai.workflow.repository.persistence.WorkflowNodeLeaderPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author zhanghaolong1989@163.com
 */
@Service
@RequiredArgsConstructor
public class WorkflowNodeLeaderPersistenceImpl implements WorkflowNodeLeaderPersistence {
    private final WorkflowNodeLeaderMapper mapper;

    @Override
    public WorkflowNodeLeaderEntity findByNamespace(String namespace) {
        return mapper.selectByNamespace(namespace);
    }

    @Override
    public void save(WorkflowNodeLeaderEntity workflowNodeLeaderEntity) {
        mapper.insert(workflowNodeLeaderEntity);
    }

    @Override
    public int refreshLeader(String namespace, String currentLeaderId, Date refreshTime) {
        return mapper.updateLeaderRefreshTime(namespace, currentLeaderId, refreshTime);
    }

    @Override
    public int deleteExpiredLeader(String namespace, Date timeThreshold) {
        return mapper.deleteByNamespaceAndRefreshTimeLessThan(namespace, timeThreshold);
    }
}
