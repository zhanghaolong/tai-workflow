package com.tai.workflow.domain.service;

import com.tai.workflow.api.Activity;
import com.tai.workflow.configuration.WorkflowConfiguration;
import com.tai.workflow.domain.executor.ActivityRunner;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.WorkflowContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionPool {
    private static final Integer SHUTDOWN_MAX_WAIT_SEC = 10;
    private final InvokeLocator invokeLocator;
    private final WorkflowConfiguration workflowConfiguration;
    @Getter
    private ThreadPoolExecutor workerThreadPool;
    @Getter
    private ScheduledThreadPoolExecutor scheduleExecutor;
    @Getter
    private ExecutorService submitWorkflowRootNodesPool;

    @PostConstruct
    public void init() {
        this.workerThreadPool = new ThreadPoolExecutor(
                workflowConfiguration.getActivityRunningPoolCoreSize(),
                workflowConfiguration.getActivityRunningPoolMaxSize(),
                workflowConfiguration.getKeepAliveMinutes(), TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(workflowConfiguration.getActivityRunningPoolQueueSize()),
                new ThreadPoolExecutor.CallerRunsPolicy());

        this.scheduleExecutor = new ScheduledThreadPoolExecutor(
                workflowConfiguration.getScheduleRunningPoolCoreSize());
    }

    public void submitTask(final WorkflowContext workflowContext, final Function<WorkflowContext, Boolean> preFunc, boolean singleActivity) {
        ActivityDefinition activityDefinition = workflowContext.getActivityDefinition();
        Activity invokeTarget = invokeLocator.getInvokeTarget(activityDefinition.getActivityClass(), singleActivity);
        ActivityRunner worker = new ActivityRunner(workflowContext, invokeTarget, this.workerThreadPool, preFunc);
        this.workerThreadPool.submit(worker);
    }

    @PreDestroy
    public void destroy() {
        List.of(this.scheduleExecutor, this.workerThreadPool)
                .parallelStream()
                .forEach(ThreadPoolExecutor::shutdownNow);
    }
}
