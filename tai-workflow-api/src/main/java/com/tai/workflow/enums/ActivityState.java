package com.tai.workflow.enums;

import java.util.List;

/**
 * @author zhanghaolong1989@163.com
 */
public enum ActivityState {
    /**
     * 活动初始态
     */
    PENDING,

    /**
     * 活动运行时的状态
     */
    RUNNING,

    /**
     * 跳过状态
     */
    SKIPPED,

    /**
     * 取消状态
     */
    CANCELLED,

    /**
     * 成功运行结束后所处的状态
     */
    COMPLETED,

    /**
     * 信号等待状态
     */
    SIGNAL_WAITING,

    /**
     * 失败状态
     */
    FAILED;

    public boolean successOrSkipped() {
        return List.of(COMPLETED, SKIPPED).contains(this);
    }

    public boolean finalStage() {
        return List.of(COMPLETED, SKIPPED, FAILED, CANCELLED).contains(this);
    }

    public boolean requireRollback() {
        return List.of(COMPLETED, SKIPPED, FAILED, CANCELLED, SIGNAL_WAITING, RUNNING).contains(this);
    }
}
