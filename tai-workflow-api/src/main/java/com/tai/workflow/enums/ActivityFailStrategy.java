package com.tai.workflow.enums;

/**
 * @author zhanghaolong1989@163.com
 */
public enum ActivityFailStrategy {
    /**
     * Activity 失败后继续执行
     */
    CONTINUE_RUN,

    /**
     * Activity 失败后走回滚策略(失败次数超过最大执行次数后)
     */
    ROLLBACK,

    /**
     * 工作流进入人工处理中等待人工介入处理
     */
    HUMAN_PROCESSING
}
