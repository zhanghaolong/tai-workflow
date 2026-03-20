package com.tai.workflow.domain.executor;

/**
 * @author zhanghaolong1989@163.com
 */
public interface DagExecutionStrategy {
    /**
     * 执行具体策略
     *
     * @param dagExecutionContext
     */
    void execute(DagExecutionContext dagExecutionContext);

    /**
     * 该策略是否支持当前 Context
     *
     * @param dagExecutionContext
     * @return
     */
    boolean support(DagExecutionContext dagExecutionContext);
}
