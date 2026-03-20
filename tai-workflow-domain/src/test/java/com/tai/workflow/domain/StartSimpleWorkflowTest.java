package com.tai.workflow.domain;

import com.tai.workflow.domain.activity.FailAlwaysActivity;
import com.tai.workflow.domain.activity.SaveThreadActivity;
import com.tai.workflow.domain.activity.SignalSelfActivity;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.utils.JsonUtils;
import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.util.WorkflowTestUtils;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.SignalAction;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class StartSimpleWorkflowTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testStartWorkflowWithNoExceptionAndNoSignal() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("startWorkflowWithNoExceptionAndNoSignal")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .displayName("启动工作流")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reserveCar")
                        .displayName("预约汽车")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reserveHotel")
                        .displayName("预约酒店")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reservePark")
                        .displayName("预约公园")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .displayName("结束")
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", List.of("reserveCar", "reserveHotel", "reservePark"))
                .addEdge(Set.of("reserveCar", "reserveHotel", "reservePark"), "end")
                .definitionVariables(Map.of("name", "test-value"))
                .description("testStartWorkflowWithNoExceptionAndNoSignal")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        for (ActivityInstance activityInstance : activityInstances) {
            assert ActivityState.COMPLETED == activityInstance.getState();
        }

        ActivityInstance endActivityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "end");
        Map<String, Object> inputContext = endActivityInstance.getInputContext();
        System.out.println("inputContext:" + JsonUtils.toJson(inputContext));
        assert inputContext.size() == 5;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);

        Thread.sleep(5_000L);
    }

    @Test
    public void testStartWorkflowWithSignalAndNoException() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("startWorkflowWithSignalAndNoException")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .signalBizCode("wait_for_signal")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reserveCar")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reserveHotel")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reservePark")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", List.of("reserveCar", "reserveHotel", "reservePark"))
                .addEdge(Set.of("reserveCar", "reserveHotel", "reservePark"), "end")
                .definitionVariables(Map.of("name", "test-value"))
                .description("testStartWorkflowWithSignalAndNoException")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.RUNNING;

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "start");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        workflowDriver.signalWorkflowInstance(workflowInstanceId, "wait_for_signal", SignalAction.SUCCESS);
        Thread.sleep(5_000);
        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "start");
        assert activityInstance.getState() == ActivityState.COMPLETED;
    }

    @Test
    public void testStartWorkflowWithSignalAndExceptionAndRollback() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("startWorkflowWithSignalAndExceptionAndContinueRun")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reserveCar")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reserveHotel")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reservePark")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("failAlways")
                        .activityClass(FailAlwaysActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.ROLLBACK)
                        .retryIntervalMillis(10)
                        .maxRetry(10)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", List.of("reserveCar", "reserveHotel", "reservePark"))
                .addEdge(Set.of("reserveCar", "reserveHotel", "reservePark"), "failAlways")
                .addEdge("failAlways", "end")
                .definitionVariables(Map.of("name", "test-value"))
                .description("testStartWorkflowWithSignalAndExceptionAndRollback")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.FAILED;

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "failAlways");
        assert activityInstance.getState() == ActivityState.FAILED;

        assert activityInstance.getRetryCount() == 10;

        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "reservePark");
        assert activityInstance.getState() == ActivityState.COMPLETED;
    }

    @Test
    public void testStartWorkflowWithSignalAndExceptionAndContinueRun() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("startWorkflowWithSignalAndExceptionAndContinueRun")
                .addNode(ActivityDefinition.builder()
                        .name("failAlways")
                        .activityClass(FailAlwaysActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.CONTINUE_RUN)
                        .retryIntervalMillis(10)
                        .maxRetry(10)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reserveCar")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reserveHotel")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reservePark")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("failAlways", List.of("reserveCar", "reserveHotel", "reservePark"))
                .addEdge(Set.of("reserveCar", "reserveHotel", "reservePark"), "end")
                .definitionVariables(Map.of("name", "test-value"))
                .description("testStartWorkflowWithSignalAndExceptionAndContinueRun")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "failAlways");
        assert activityInstance.getState() == ActivityState.SKIPPED;

        assert activityInstance.getRetryCount() == 10;
        assert activityInstance.getExecutionMsg().contains("test exception");
    }

    @Test
    public void testStartWorkflowWithFailAlwaysActivityAndWorkflowStateCheck() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testStartWorkflowWithFailAlwaysActivityAndWorkflowStateCheck")
                .addNode(ActivityDefinition.builder()
                        .name("failAlways")
                        .activityClass(FailAlwaysActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.CONTINUE_RUN)
                        .retryIntervalMillis(3_000)
                        .maxRetry(2)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("failAlways", "end")
                .definitionVariables(Map.of("name", "test-value"))
                .description("testStartWorkflowWithFailAlwaysActivityAndWorkflowStateCheck")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.RUNNING;
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testStartWorkflowWithFailedContinueRun() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testStartWorkflowWithFailedContinueRun")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("failAlways")
                        .activityClass(FailAlwaysActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.CONTINUE_RUN)
                        .retryIntervalMillis(3_000)
                        .maxRetry(2)
                        .build())

                .addEdge("start", "failAlways")
                .definitionVariables(Map.of("name", "test-value"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(10_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testSignalWorkflowInstanceWithinActivity() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSignalWorkflowInstanceWithinActivity")
                .addNode(ActivityDefinition.builder()
                        .name("signalActivity")
                        .activityClass(SignalSelfActivity.class)
                        .signalBizCode("signalActivity")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("saveThreadActivity")
                        .activityClass(SaveThreadActivity.class)
                        .build())
                .addEdge("signalActivity", "saveThreadActivity")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;
        Set<String> runResults = workflowInstance.getContextParams().keySet().stream()
                .filter(key -> key.startsWith("currentThread_"))
                .collect(Collectors.toSet());
        assert runResults.size() == 1;
    }

    @Test
    public void testSignalWorkflowInstanceWithMultipleTimes() throws InterruptedException {
        ExecutorService es = null;
        try {
            es = Executors.newFixedThreadPool(2);
            WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                    .name("testSignalWorkflowInstanceWithMultipleTimes")
                    .addNode(ActivityDefinition.builder()
                            .name("reserveCar")
                            .activityClass(UnifyActivity.class)
                            .signalBizCode("reserveCar")
                            .build())
                    .addNode(ActivityDefinition.builder()
                            .name("testConcurrent")
                            .activityClass(UnifyActivity.class)
                            .build())
                    .addEdge("reserveCar", "testConcurrent")
                    .build();
            Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
            Thread.sleep(5_00L);
            WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
            assert workflowInstance.getState() == WorkflowState.RUNNING;
            ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "reserveCar");
            assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

            es.submit(() -> workflowDriver.signalWorkflowInstance(workflowInstanceId, "reserveCar", SignalAction.SUCCESS));

            es.submit(() -> workflowDriver.signalWorkflowInstance(workflowInstanceId, "reserveCar", SignalAction.SUCCESS));

            Thread.sleep(1_500L);
            workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
            assert workflowInstance.getState() == WorkflowState.COMPLETED;
            activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "reserveCar");
            assert activityInstance.getState() == ActivityState.COMPLETED;
            assert workflowInstance.getContextParams().keySet().stream().filter(key -> key.startsWith("COUNT_")).count() >= 1;

            // start the second workflow
            Long secondWorkflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
            Thread.sleep(500L);
            workflowInstance = workflowDriver.getWorkflowInstance(secondWorkflowInstanceId);
            assert workflowInstance.getState() == WorkflowState.RUNNING;
            activityInstance = workflowDriver.getActivityInstance(secondWorkflowInstanceId, "reserveCar");
            assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;
            es.submit(() -> workflowDriver.signalWorkflowInstance(secondWorkflowInstanceId, "reserveCar", SignalAction.SUCCESS, Boolean.TRUE));
            es.submit(() -> workflowDriver.signalWorkflowInstance(secondWorkflowInstanceId, "reserveCar", SignalAction.SUCCESS, Boolean.TRUE));

            Thread.sleep(1_500L);
            workflowInstance = workflowDriver.getWorkflowInstance(secondWorkflowInstanceId);
            assert workflowInstance.getState() == WorkflowState.COMPLETED;
            activityInstance = workflowDriver.getActivityInstance(secondWorkflowInstanceId, "reserveCar");
            assert activityInstance.getState() == ActivityState.COMPLETED;
            assert workflowInstance.getContextParams().keySet().stream().filter(key -> key.startsWith("COUNT_")).count() == 1;
        } finally {
            if (Objects.nonNull(es)) {
                es.shutdownNow();
            }
        }
    }
}
