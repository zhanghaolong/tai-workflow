package com.tai.workflow.enums;

import java.util.Set;

/**
 * @author zhanghaolong1989@163.com
 */
public enum WorkflowState {
    /**
     * 工作流初始化的状态
     */
    PENDING,

    /**
     * 工作流启动后的状态
     */
    RUNNING,

    /**
     * 人工处理中
     */
    HUMAN_PROCESSING,

    /**
     * 成功完成
     */
    COMPLETED,

    /**
     * 执行失败
     */
    FAILED,

    /**
     * 终止执行状态 调用 terminateWorkflow 后的状态
     */
    TERMINATED;

    public boolean finalState() {
        return Set.of(COMPLETED, FAILED, TERMINATED).contains(this);
    }

    public boolean completeOrTerminatedState() {
        return Set.of(COMPLETED, TERMINATED).contains(this);
    }
}
