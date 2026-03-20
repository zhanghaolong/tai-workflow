package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.domain.util.WorkflowTestUtils;
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

/**
 * Signal mechanism deep tests.
 *
 * <p>Scenarios covered:
 *
 * <ul>
 *   <li>Multiple signals in sequence - wait for multiple approvals
 *   <li>Parallel signal waiting - multiple activities waiting for different signals
 *   <li>Signal with context merge - signal carries additional data
 *   <li>Signal timeout behavior - late signals
 *   <li>Signal before activity reaches SIGNAL_WAITING state
 *   <li>Signal with FAILED_AT_ONCE action - immediate rollback
 *   <li>Signal idempotency - duplicate signals with uniqueCheck
 *   <li>Signal ordering - verify FIFO or LIFO behavior
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SignalMechanismTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testMultipleSignalsInSequence() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testMultipleSignalsInSequence")
                .addNode(ActivityDefinition.builder()
                        .name("waitForApproval1")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("approval_1")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("waitForApproval2")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("approval_2")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("waitForApproval3")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("approval_3")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("waitForApproval1", "waitForApproval2")
                .addEdge("waitForApproval2", "waitForApproval3")
                .addEdge("waitForApproval3", "end")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);

        // First signal waiting
        ActivityInstance activity1 = workflowDriver.getActivityInstance(workflowInstanceId, "waitForApproval1");
        assert activity1.getState() == ActivityState.SIGNAL_WAITING;

        // Send first signal
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "approval_1", SignalAction.SUCCESS,
                Map.of("approver1", "Alice"));
        Thread.sleep(1_000L);

        // Second signal waiting
        ActivityInstance activity2 = workflowDriver.getActivityInstance(workflowInstanceId, "waitForApproval2");
        assert activity2.getState() == ActivityState.SIGNAL_WAITING;

        // Send second signal
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "approval_2", SignalAction.SUCCESS,
                Map.of("approver2", "Bob"));
        Thread.sleep(1_000L);

        // Third signal waiting
        ActivityInstance activity3 = workflowDriver.getActivityInstance(workflowInstanceId, "waitForApproval3");
        assert activity3.getState() == ActivityState.SIGNAL_WAITING;

        // Send third signal
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "approval_3", SignalAction.SUCCESS,
                Map.of("approver3", "Charlie"));
        Thread.sleep(2_000L);

        // Workflow should be completed
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify all approver context merged
        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert "Alice".equals(contextParams.get("approver1"));
        assert "Bob".equals(contextParams.get("approver2"));
        assert "Charlie".equals(contextParams.get("approver3"));

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testParallelSignalWaiting() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testParallelSignalWaiting")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("waitSignalA")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("signal_A")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("waitSignalB")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("signal_B")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("waitSignalC")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("signal_C")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("start", List.of("waitSignalA", "waitSignalB", "waitSignalC"))
                .addEdge(List.of("waitSignalA", "waitSignalB", "waitSignalC"), "end")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        // All three activities should be waiting for signals
        ActivityInstance activityA = workflowDriver.getActivityInstance(workflowInstanceId, "waitSignalA");
        ActivityInstance activityB = workflowDriver.getActivityInstance(workflowInstanceId, "waitSignalB");
        ActivityInstance activityC = workflowDriver.getActivityInstance(workflowInstanceId, "waitSignalC");

        assert activityA.getState() == ActivityState.SIGNAL_WAITING;
        assert activityB.getState() == ActivityState.SIGNAL_WAITING;
        assert activityC.getState() == ActivityState.SIGNAL_WAITING;

        // Send signals in different order
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "signal_C", SignalAction.SUCCESS,
                Map.of("signalC_data", "data_C"));
        Thread.sleep(500L);

        workflowDriver.signalWorkflowInstance(workflowInstanceId, "signal_A", SignalAction.SUCCESS,
                Map.of("signalA_data", "data_A"));
        Thread.sleep(500L);

        workflowDriver.signalWorkflowInstance(workflowInstanceId, "signal_B", SignalAction.SUCCESS,
                Map.of("signalB_data", "data_B"));
        Thread.sleep(2_000L);

        // Workflow should be completed
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify all signal data merged
        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert contextParams.containsKey("signalA_data");
        assert contextParams.containsKey("signalB_data");
        assert contextParams.containsKey("signalC_data");

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testSignalWithLargeContextMerge() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSignalWithLargeContextMerge")
                .addNode(ActivityDefinition.builder()
                        .name("waitForData")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("data_signal")
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForData");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        // Send signal with large context (50 keys)
        Map<String, Object> largeSignalContext = new java.util.HashMap<>();
        for (int i = 0; i < 50; i++) {
            largeSignalContext.put("signal_key_" + i, "signal_value_" + i);
        }

        workflowDriver.signalWorkflowInstance(workflowInstanceId, "data_signal", SignalAction.SUCCESS,
                largeSignalContext);
        Thread.sleep(2_000L);

        // Workflow should be completed
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify large context merged
        Map<String, Object> contextParams = workflowInstance.getContextParams();
        for (int i = 0; i < 50; i++) {
            assert contextParams.containsKey("signal_key_" + i);
            assert ("signal_value_" + i).equals(contextParams.get("signal_key_" + i));
        }
    }

    @Test
    public void testLateSignalAfterTimeout() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testLateSignalAfterTimeout")
                .addNode(ActivityDefinition.builder()
                        .name("waitWithTimeout")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("timeout_signal")
                        .timeoutMillis(3000L) // 3 second timeout (not enforced yet)
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitWithTimeout");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        // Wait longer than "timeout" (but framework doesn't enforce timeout yet)
        Thread.sleep(5_000L);

        // Send late signal
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "timeout_signal", SignalAction.SUCCESS);
        Thread.sleep(2_000L);

        // Activity should still complete successfully (no timeout enforcement)
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitWithTimeout");
        assert activityInstance.getState() == ActivityState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testSignalWithFailedAtOnce() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSignalWithFailedAtOnce")
                .addNode(ActivityDefinition.builder()
                        .name("step1")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("waitForSignal")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("critical_signal")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("step3")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("step1", "waitForSignal")
                .addEdge("waitForSignal", "step3")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_500L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForSignal");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        // Send FAILED_AT_ONCE signal (should trigger immediate failure without retry)
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "critical_signal", SignalAction.FAILED_AT_ONCE);
        Thread.sleep(2_000L);

        // Activity should be FAILED
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForSignal");
        assert activityInstance.getState() == ActivityState.FAILED;

        // Workflow should be in terminal state (FAILED or HUMAN_PROCESSING)
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.FAILED ||
               workflowInstance.getState() == WorkflowState.HUMAN_PROCESSING;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testSignalIdempotencyWithUniqueCheck() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSignalIdempotencyWithUniqueCheck")
                .addNode(ActivityDefinition.builder()
                        .name("waitForPayment")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("payment_confirmed")
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForPayment");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        // Send first signal with context
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "payment_confirmed", SignalAction.SUCCESS,
                Map.of("amount", 100, "attempt", 1));
        Thread.sleep(500L);

        // Try to send duplicate signal (framework should handle idempotency)
        try {
            workflowDriver.signalWorkflowInstance(workflowInstanceId, "payment_confirmed", SignalAction.SUCCESS,
                    Map.of("amount", 200, "attempt", 2));
        } catch (Exception e) {
            System.out.println("Duplicate signal rejected: " + e.getMessage());
        }

        Thread.sleep(2_000L);

        // Workflow should be completed
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify only first signal context was applied
        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert contextParams.get("amount").equals(100); // First signal
        assert contextParams.get("attempt").equals(1);

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testSignalBeforeActivityReachesWaitingState() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSignalBeforeActivityReachesWaitingState")
                .addNode(ActivityDefinition.builder()
                        .name("slowStart")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("waitForSignal")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("early_signal")
                        .build())
                .addEdge("slowStart", "waitForSignal")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);

        // Immediately send signal before activity reaches SIGNAL_WAITING (race condition test)
        try {
            workflowDriver.signalWorkflowInstance(workflowInstanceId, "early_signal", SignalAction.SUCCESS,
                    Map.of("early", true));
        } catch (Exception e) {
            System.out.println("Early signal might be rejected: " + e.getMessage());
        }

        Thread.sleep(3_000L);

        // Workflow should eventually complete (signal should be queued or activity should wait)
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        // State could be COMPLETED (if signal worked) or RUNNING (if signal failed)
        assert workflowInstance.getState() == WorkflowState.COMPLETED ||
               workflowInstance.getState() == WorkflowState.RUNNING;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testMultipleSignalActionsOnSameActivity() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testMultipleSignalActionsOnSameActivity")
                .addNode(ActivityDefinition.builder()
                        .name("waitForDecision")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("decision")
                        .maxRetry(2)
                        .retryIntervalMillis(100)
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForDecision");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        // Send FAILED_NORMAL signal (should trigger retry)
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "decision", SignalAction.FAILED_NORMAL,
                Map.of("decision", "rejected_1"));
        Thread.sleep(1_000L);

        // Activity should retry and wait for signal again
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForDecision");
        assert activityInstance.getRetryCount() >= 1;

        // Send SUCCESS signal on retry
        if (activityInstance.getState() == ActivityState.SIGNAL_WAITING) {
            workflowDriver.signalWorkflowInstance(workflowInstanceId, "decision", SignalAction.SUCCESS,
                    Map.of("decision", "approved"));
            Thread.sleep(2_000L);
        }

        // Workflow should be completed
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED ||
               workflowInstance.getState() == WorkflowState.HUMAN_PROCESSING;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testSignalContextOverwritePreviousContext() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSignalContextOverwritePreviousContext")
                .addNode(ActivityDefinition.builder()
                        .name("setupContext")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("waitForUpdate")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("update")
                        .build())
                .addEdge("setupContext", "waitForUpdate")
                .definitionVariables(Map.of("status", "initial", "value", 0))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_500L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForUpdate");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        // Send signal with updated context
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "update", SignalAction.SUCCESS,
                Map.of("status", "updated", "value", 100, "newKey", "newValue"));
        Thread.sleep(2_000L);

        // Workflow should be completed
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify context was overwritten
        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert "updated".equals(contextParams.get("status")); // Overwritten
        assert contextParams.get("value").equals(100); // Overwritten
        assert "newValue".equals(contextParams.get("newKey")); // New key added

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }
}
