package com.tai.workflow.repository.persistence;

import com.tai.workflow.repository.entity.WorkflowNodeLeaderEntity;

import java.util.Date;

/**
 * @author zhanghaolong1989@163.com
 */
public interface WorkflowNodeLeaderPersistence {
    WorkflowNodeLeaderEntity findByNamespace(String namespace);

    void save(WorkflowNodeLeaderEntity workflowNodeLeaderEntity);

    int refreshLeader(String namespace, String currentLeaderId, Date refreshTime);

    int deleteExpiredLeader(String namespace, Date timeThreshold);
}
