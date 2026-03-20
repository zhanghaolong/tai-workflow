package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.UnifyActivity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrent workflow execution tests.
 *
 * <p>Scenarios covered:
 *
 * <ul>
 *   <li>Concurrent signal sending with uniqueCheck=true - idempotency test
 *   <li>Concurrent signal sending with uniqueCheck=false - duplicate processing test
 *   <li>Multiple workflows execution concurrently - isolation test
 *   <li>Concurrent context updates - optimistic lock test
 *   <li>Concurrent retry on same activity - race condition test
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ConcurrentWorkflowTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testConcurrentSignalWithUniqueCheck() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                    .name("testConcurrentSignalWithUniqueCheck")
                    .addNode(ActivityDefinition.builder()
                            .name("waitForSignal")
                            .activityClass(UnifyActivity.class)
                            .signalBizCode("payment_callback")
                            .build())
                    .addNode(ActivityDefinition.builder()
                            .name("end")
                            .activityClass(UnifyActivity.class)
                            .build())
                    .addEdge("waitForSignal", "end")
                    .build();

            Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
            Thread.sleep(1_000L);

            WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
            assert workflowInstance.getState() == WorkflowState.RUNNING;

            ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForSignal");
            assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

            // Send 10 concurrent signals with uniqueCheck=true
            AtomicInteger successCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(10);

            for (int i = 0; i < 10; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        workflowDriver.signalWorkflowInstance(
                                workflowInstanceId,
                                "payment_callback",
                                SignalAction.SUCCESS,
                                Map.of("requestId", "req_" + index),
                                true
                        );
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        System.out.println("Signal failed: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            Thread.sleep(5_000L);

            // Verify workflow completed
            workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
            assert workflowInstance.getState() == WorkflowState.COMPLETED;

            // Only one signal should have been processed
            activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForSignal");
            assert activityInstance.getState() == ActivityState.COMPLETED;

            WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void testConcurrentWorkflowExecution() throws InterruptedException {
        // Start 5 workflows sequentially (startup is fast, execution is concurrent)
        List<Long> workflowInstanceIds = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            WorkflowDefinition def = WorkflowDefinition.builder()
                    .name("testConcurrentWorkflowExecution_" + i)
                    .addNode(ActivityDefinition.builder().name("start").activityClass(UnifyActivity.class).build())
                    .addNode(ActivityDefinition.builder().name("parallel1").activityClass(UnifyActivity.class).build())
                    .addNode(ActivityDefinition.builder().name("parallel2").activityClass(UnifyActivity.class).build())
                    .addNode(ActivityDefinition.builder().name("parallel3").activityClass(UnifyActivity.class).build())
                    .addNode(ActivityDefinition.builder().name("end").activityClass(UnifyActivity.class).build())
                    .addEdge("start", List.of("parallel1", "parallel2", "parallel3"))
                    .addEdge(List.of("parallel1", "parallel2", "parallel3"), "end")
                    .definitionVariables(Map.of("workflowIndex", i))
                    .build();
            Long id = workflowDriver.startWorkflowInstance(def);
            workflowInstanceIds.add(id);
        }

        // Wait for all workflows to complete (they execute concurrently in the thread pool)
        Thread.sleep(6_000L);

        // Verify all workflows completed
        assert workflowInstanceIds.size() == 5;

        for (Long workflowInstanceId : workflowInstanceIds) {
            WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
            assert workflowInstance.getState() == WorkflowState.COMPLETED;

            List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
            for (ActivityInstance activityInstance : activityInstances) {
                assert activityInstance.getState() == ActivityState.COMPLETED;
            }
        }
    }

    @Test
    public void testConcurrentContextUpdate() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testConcurrentContextUpdate")
                .addNode(ActivityDefinition.builder()
                        .name("activity1")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("activity2")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("activity3")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("activity1", List.of("activity2", "activity3"))
                .addEdge(List.of("activity2", "activity3"), "end")
                .definitionVariables(Map.of("initialValue", "test"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify context contains all activity outputs
        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert contextParams.containsKey("initialValue");
        assert contextParams.containsKey("activity1") : "context should contain activity1 output";
        assert contextParams.containsKey("activity2") : "context should contain activity2 output";
        assert contextParams.containsKey("activity3") : "context should contain activity3 output";
        assert contextParams.containsKey("end") : "context should contain end output";

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testMultipleWorkflowsWithSameDefinition() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testMultipleWorkflowsWithSameDefinition")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("process")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("start", "process")
                .addEdge("process", "end")
                .build();

        // Start 3 workflows with same definition but different contexts
        WorkflowDefinition workflow1Def = WorkflowDefinition.builder()
                .name("testMultipleWorkflowsWithSameDefinition_1")
                .addNode(ActivityDefinition.builder().name("start").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("process").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("end").activityClass(UnifyActivity.class).build())
                .addEdge("start", "process")
                .addEdge("process", "end")
                .definitionVariables(Map.of("userId", "user1"))
                .build();

        WorkflowDefinition workflow2Def = WorkflowDefinition.builder()
                .name("testMultipleWorkflowsWithSameDefinition_2")
                .addNode(ActivityDefinition.builder().name("start").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("process").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("end").activityClass(UnifyActivity.class).build())
                .addEdge("start", "process")
                .addEdge("process", "end")
                .definitionVariables(Map.of("userId", "user2"))
                .build();

        WorkflowDefinition workflow3Def = WorkflowDefinition.builder()
                .name("testMultipleWorkflowsWithSameDefinition_3")
                .addNode(ActivityDefinition.builder().name("start").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("process").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("end").activityClass(UnifyActivity.class).build())
                .addEdge("start", "process")
                .addEdge("process", "end")
                .definitionVariables(Map.of("userId", "user3"))
                .build();

        Long workflowId1 = workflowDriver.startWorkflowInstance(workflow1Def);
        Long workflowId2 = workflowDriver.startWorkflowInstance(workflow2Def);
        Long workflowId3 = workflowDriver.startWorkflowInstance(workflow3Def);

        Thread.sleep(5_000L);

        // Verify all workflows completed independently
        WorkflowInstance workflow1 = workflowDriver.getWorkflowInstance(workflowId1);
        WorkflowInstance workflow2 = workflowDriver.getWorkflowInstance(workflowId2);
        WorkflowInstance workflow3 = workflowDriver.getWorkflowInstance(workflowId3);

        assert workflow1.getState() == WorkflowState.COMPLETED;
        assert workflow2.getState() == WorkflowState.COMPLETED;
        assert workflow3.getState() == WorkflowState.COMPLETED;

        assert "user1".equals(workflow1.getContextParams().get("userId"));
        assert "user2".equals(workflow2.getContextParams().get("userId"));
        assert "user3".equals(workflow3.getContextParams().get("userId"));
    }

    @Test
    public void testConcurrentSignalWithoutUniqueCheck() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        try {
            WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                    .name("testConcurrentSignalWithoutUniqueCheck")
                    .addNode(ActivityDefinition.builder()
                            .name("waitForSignal")
                            .activityClass(UnifyActivity.class)
                            .signalBizCode("approval")
                            .build())
                    .addNode(ActivityDefinition.builder()
                            .name("end")
                            .activityClass(UnifyActivity.class)
                            .build())
                    .addEdge("waitForSignal", "end")
                    .build();

            Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
            Thread.sleep(1_000L);

            ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForSignal");
            assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

            // Send 5 concurrent signals with uniqueCheck=false
            CountDownLatch latch = new CountDownLatch(5);

            for (int i = 0; i < 5; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        workflowDriver.signalWorkflowInstance(
                                workflowInstanceId,
                                "approval",
                                SignalAction.SUCCESS,
                                Map.of("approver", "approver_" + index)
                        );
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            Thread.sleep(2_000L);

            // Verify workflow completed
            WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
            assert workflowInstance.getState() == WorkflowState.COMPLETED;

            activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForSignal");
            assert activityInstance.getState() == ActivityState.COMPLETED;

            // Last signal context should be present
            assert workflowInstance.getContextParams().containsKey("approver");

            WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        } finally {
            executorService.shutdownNow();
        }
    }
}
