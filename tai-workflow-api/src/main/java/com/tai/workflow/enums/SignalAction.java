package com.tai.workflow.enums;

/**
 * @author zhanghaolong1989@163.com
 */
public enum SignalAction {
    /**
     * 信号通知成功
     */
    SUCCESS,

    /**
     * 信号通知立刻走失败处理流程
     */
    FAILED_AT_ONCE,

    /**
     * 信号通知正常失败（如果设置了重试会先重试）
     */
    FAILED_NORMAL,

    /**
     * 信号通知直接终止
     */
    TERMINATED
}
