package com.tai.workflow.repository.persistence;

import com.tai.workflow.repository.entity.WorkflowSignalRecordEntity;

/**
 * @author zhanghaolong1989@163.com
 */
public interface WorkflowSignalRecordPersistence {
    boolean existsByWorkflowInstanceIdAndSignalBizCode(Long workflowInstanceId, String signalBizCode);

    WorkflowSignalRecordEntity findByWorkflowInstanceIdAndSignalBizCode(Long workflowInstanceId, String signalBizCode);

    void deleteByWorkflowInstanceId(Long workflowInstanceId);

    void deleteByWorkflowInstanceIdAndSignalBizCode(Long workflowInstanceId, String signalBizCode);

    void upsert(WorkflowSignalRecordEntity workflowSignalRecordEntity);

    void insert(WorkflowSignalRecordEntity workflowSignalRecordEntity);
}
