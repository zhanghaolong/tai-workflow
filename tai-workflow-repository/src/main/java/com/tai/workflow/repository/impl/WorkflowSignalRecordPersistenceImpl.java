package com.tai.workflow.repository.impl;

import com.tai.workflow.repository.entity.WorkflowSignalRecordEntity;
import com.tai.workflow.repository.mapper.WorkflowSignalRecordMapper;
import com.tai.workflow.repository.persistence.WorkflowSignalRecordPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author zhanghaolong1989@163.com
 */
@Service
@RequiredArgsConstructor
public class WorkflowSignalRecordPersistenceImpl implements WorkflowSignalRecordPersistence {
    private final WorkflowSignalRecordMapper mapper;

    @Override
    public boolean existsByWorkflowInstanceIdAndSignalBizCode(Long workflowInstanceId, String signalBizCode) {
        return mapper.existsByWorkflowInstanceIdAndSignalBizCode(workflowInstanceId, signalBizCode) > 0;
    }

    @Override
    public WorkflowSignalRecordEntity findByWorkflowInstanceIdAndSignalBizCode(Long workflowInstanceId, String signalBizCode) {
        return mapper.selectByWorkflowInstanceIdAndSignalBizCode(workflowInstanceId, signalBizCode);
    }

    @Override
    public void deleteByWorkflowInstanceId(Long workflowInstanceId) {
        mapper.deleteByWorkflowInstanceId(workflowInstanceId);
    }

    @Override
    public void deleteByWorkflowInstanceIdAndSignalBizCode(Long workflowInstanceId, String signalBizCode) {
        mapper.deleteByWorkflowInstanceIdAndSignalBizCode(workflowInstanceId, signalBizCode);
    }

    @Override
    public void upsert(WorkflowSignalRecordEntity workflowSignalRecordEntity) {
        mapper.upsert(workflowSignalRecordEntity.getWorkflowInstanceId(),
                workflowSignalRecordEntity.getSignalBizCode(), workflowSignalRecordEntity.getSignalTime());
    }

    @Override
    public void insert(WorkflowSignalRecordEntity workflowSignalRecordEntity) {
        workflowSignalRecordEntity.setCreateTime(new Date());
        mapper.insert(workflowSignalRecordEntity);
    }
}
