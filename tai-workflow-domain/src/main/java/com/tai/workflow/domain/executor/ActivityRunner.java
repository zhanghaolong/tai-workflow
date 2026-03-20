package com.tai.workflow.domain.executor;

import com.tai.workflow.api.Activity;
import com.tai.workflow.domain.api.SingleActivity;
import com.tai.workflow.domain.handler.EventFactory;
import com.tai.workflow.domain.handler.WorkflowEventPublisher;
import com.tai.workflow.domain.util.LoggerUtils;
import com.tai.workflow.exception.ActivityExecutionWrapperException;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.StopWatch;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
public class ActivityRunner implements Runnable {
    public static final ThreadLocal<WorkflowContext> WORKFLOW_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();
    private final ThreadPoolExecutor workerThreadPool;
    private final Function<WorkflowContext, Boolean> preFunc;
    private final WorkflowContext context;
    private final Activity invokeTarget;

    public ActivityRunner(WorkflowContext workflowContext, Activity invokeTarget,
            ThreadPoolExecutor workerThreadPool, Function<WorkflowContext, Boolean> preFunc) {
        this.context = workflowContext;
        this.invokeTarget = invokeTarget;
        this.workerThreadPool = workerThreadPool;
        this.preFunc = preFunc;
    }

    public void run() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        CompletableFuture<ActivityExecutionResult> future = CompletableFuture.supplyAsync(() -> {
            ActivityExecutionResult activityExecutionResult;
            try {
                if (!preFunc.apply(context)) {
                    activityExecutionResult = ActivityExecutionResult.IGNORE;
                    return activityExecutionResult;
                }

                log.info("ActivityWorker begin to execute activity {}", LoggerUtils.getTraceHeader(context));
                WORKFLOW_CONTEXT_THREAD_LOCAL.set(context);
                activityExecutionResult = invokeTarget.invoke(context);
                if (Objects.isNull(activityExecutionResult)) {
                    activityExecutionResult = ActivityExecutionResult.ofSucceeded();
                }
            } catch (Throwable throwable) {
                throw new ActivityExecutionWrapperException(throwable);
            } finally {
                WORKFLOW_CONTEXT_THREAD_LOCAL.remove();
            }

            return activityExecutionResult;
        }, workerThreadPool);
        future.thenApply((activityExecutionResult) -> {
            if (activityExecutionResult == ActivityExecutionResult.IGNORE) {
                return activityExecutionResult;
            } else {
                activityExecutionResult.setSingleActivity(invokeTarget instanceof SingleActivity);
                if (BooleanUtils.isTrue(activityExecutionResult.isSuccess())) {
                    WorkflowEventPublisher.publishEvent(EventFactory.createActivityCompletedEvent(context, activityExecutionResult));
                } else {
                    WorkflowEventPublisher.publishEvent(EventFactory.createActivityFailedEvent(context, activityExecutionResult.getMsg()));
                }

                return activityExecutionResult;
            }
        }).exceptionally((t) -> {
            Throwable throwable;
            if (t.getCause() instanceof ActivityExecutionWrapperException wrapperException) {
                throwable = wrapperException.getThrowable();
            } else {
                throwable = t;
            }

            log.error("ActivityWorker {} execute met with exception", LoggerUtils.getTraceHeader(context), throwable);
            WorkflowEventPublisher.publishEvent(EventFactory.createActivityFailedEvent(context, throwable));
            return null;
        }).whenComplete((__1, __2) -> {
            stopWatch.stop();
            log.info("ActivityWorker activity {} completed elapsed: {} ms", LoggerUtils.getTraceHeader(context), stopWatch.getTotalTimeMillis());
        });
    }
}
