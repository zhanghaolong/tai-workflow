package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.FailAlwaysActivity;
import com.tai.workflow.domain.activity.FailOnceActivityForResume;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * Activity state transition tests.
 *
 * <p>Scenarios covered:
 *
 * <ul>
 *   <li>Normal state flow: PENDING → RUNNING → COMPLETED
 *   <li>Failed state flow: PENDING → RUNNING → FAILED
 *   <li>Signal wait flow: PENDING → RUNNING → SIGNAL_WAITING → COMPLETED
 *   <li>Retry flow: FAILED → PENDING → RUNNING → COMPLETED
 *   <li>Skipped state: FAILED → SKIPPED (with CONTINUE_RUN strategy)
 *   <li>Cancelled state: SIGNAL_WAITING → CANCELLED (with TERMINATED signal)
 *   <li>State transitions during workflow termination
 *   <li>State consistency after rollback
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ActivityStateTransitionTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @BeforeEach
    void resetFailOnceCounters() {
        FailOnceActivityForResume.count = 0;
    }

    @Test
    public void testNormalStateTransition() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testNormalStateTransition")
                .addNode(ActivityDefinition.builder()
                        .name("normalActivity")
                        .activityClass(UnifyActivity.class)
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);

        // Check initial state
        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "normalActivity");
        assert activityInstance.getState() == ActivityState.PENDING || activityInstance.getState() == ActivityState.RUNNING;

        Thread.sleep(2_000L);

        // Check final state
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "normalActivity");
        assert activityInstance.getState() == ActivityState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testFailedStateTransition() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testFailedStateTransition")
                .addNode(ActivityDefinition.builder()
                        .name("failActivity")
                        .activityClass(FailAlwaysActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .maxRetry(0) // No retry
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "failActivity");
        assert activityInstance.getState() == ActivityState.FAILED;

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.HUMAN_PROCESSING;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testSignalWaitStateTransition() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSignalWaitStateTransition")
                .addNode(ActivityDefinition.builder()
                        .name("signalActivity")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("test_signal")
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);

        // Check SIGNAL_WAITING state
        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "signalActivity");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        // Send signal
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "test_signal", SignalAction.SUCCESS);
        Thread.sleep(2_000L);

        // Check COMPLETED state
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "signalActivity");
        assert activityInstance.getState() == ActivityState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testRetryStateTransition() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testRetryStateTransition")
                .addNode(ActivityDefinition.builder()
                        .name("retryActivity")
                        .activityClass(FailOnceActivityForResume.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .maxRetry(0)
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        // Should be FAILED after first execution
        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "retryActivity");
        assert activityInstance.getState() == ActivityState.FAILED;
        assert activityInstance.getRetryCount() == 0;

        // Retry the activity
        workflowDriver.retrySingleActivityInstance(activityInstance.getId());
        Thread.sleep(2_000L);

        // Should be COMPLETED after retry
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "retryActivity");
        assert activityInstance.getState() == ActivityState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testSkippedStateWithContinueRun() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSkippedStateWithContinueRun")
                .addNode(ActivityDefinition.builder()
                        .name("failAndSkip")
                        .activityClass(FailAlwaysActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.CONTINUE_RUN)
                        .maxRetry(2)
                        .retryIntervalMillis(100)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("nextActivity")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("failAndSkip", "nextActivity")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(3_000L);

        // Failed activity should be SKIPPED
        ActivityInstance failedActivity = workflowDriver.getActivityInstance(workflowInstanceId, "failAndSkip");
        assert failedActivity.getState() == ActivityState.SKIPPED;
        assert failedActivity.getRetryCount() == 2;

        // Next activity should be COMPLETED
        ActivityInstance nextActivity = workflowDriver.getActivityInstance(workflowInstanceId, "nextActivity");
        assert nextActivity.getState() == ActivityState.COMPLETED;

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testCancelledStateWithTerminatedSignal() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testCancelledStateWithTerminatedSignal")
                .addNode(ActivityDefinition.builder()
                        .name("waitForSignal")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("cancellable_signal")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("nextActivity")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("waitForSignal", "nextActivity")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForSignal");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        // Send TERMINATED signal
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "cancellable_signal", SignalAction.TERMINATED);
        Thread.sleep(2_000L);

        // Activity should be CANCELLED
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForSignal");
        assert activityInstance.getState() == ActivityState.CANCELLED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testStateTransitionDuringWorkflowTermination() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testStateTransitionDuringWorkflowTermination")
                .addNode(ActivityDefinition.builder()
                        .name("activity1")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("activity2")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("wait_signal")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("activity3")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("activity1", "activity2")
                .addEdge("activity2", "activity3")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_500L);

        // activity1 should be COMPLETED, activity2 should be SIGNAL_WAITING
        ActivityInstance activity1 = workflowDriver.getActivityInstance(workflowInstanceId, "activity1");
        assert activity1.getState() == ActivityState.COMPLETED;

        ActivityInstance activity2 = workflowDriver.getActivityInstance(workflowInstanceId, "activity2");
        assert activity2.getState() == ActivityState.SIGNAL_WAITING;

        // Terminate workflow
        workflowDriver.terminateWorkflowInstance(workflowInstanceId);
        Thread.sleep(1_000L);

        // Workflow should be TERMINATED
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.TERMINATED;

        // activity2 should be CANCELLED
        activity2 = workflowDriver.getActivityInstance(workflowInstanceId, "activity2");
        assert activity2.getState() == ActivityState.CANCELLED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testStateConsistencyAfterRollback() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testStateConsistencyAfterRollback")
                .addNode(ActivityDefinition.builder()
                        .name("step1")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("step2")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("failStep")
                        .activityClass(FailAlwaysActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.ROLLBACK)
                        .maxRetry(1)
                        .retryIntervalMillis(100)
                        .build())
                .addEdge("step1", "step2")
                .addEdge("step2", "failStep")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(3_000L);

        // Workflow should be FAILED
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.FAILED;

        // failStep should be FAILED
        ActivityInstance failStep = workflowDriver.getActivityInstance(workflowInstanceId, "failStep");
        assert failStep.getState() == ActivityState.FAILED;

        // Previous steps should still be COMPLETED (rollback is logical, not state change)
        ActivityInstance step1 = workflowDriver.getActivityInstance(workflowInstanceId, "step1");
        ActivityInstance step2 = workflowDriver.getActivityInstance(workflowInstanceId, "step2");
        assert step1.getState() == ActivityState.COMPLETED;
        assert step2.getState() == ActivityState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testMultipleStateTransitionsOnRetry() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testMultipleStateTransitionsOnRetry")
                .addNode(ActivityDefinition.builder()
                        .name("multiRetry")
                        .activityClass(FailOnceActivityForResume.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .maxRetry(0)
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "multiRetry");
        Long activityId = activityInstance.getId();

        // State 1: FAILED
        assert activityInstance.getState() == ActivityState.FAILED;
        assert activityInstance.getRetryCount() == 0;

        // Retry 1: Should succeed
        workflowDriver.retrySingleActivityInstance(activityId);
        Thread.sleep(2_000L);

        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "multiRetry");
        assert activityInstance.getState() == ActivityState.COMPLETED;

        // Retry 2: Retry completed activity
        workflowDriver.retrySingleActivityInstance(activityId);
        Thread.sleep(2_000L);

        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "multiRetry");
        // Activity should remain COMPLETED or be re-executed
        assert activityInstance.getState() == ActivityState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testSignalActionFailed() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSignalActionFailed")
                .addNode(ActivityDefinition.builder()
                        .name("waitForApproval")
                        .activityClass(UnifyActivity.class)
                        .signalBizCode("approval")
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .maxRetry(1)
                        .retryIntervalMillis(100)
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForApproval");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        // Send FAILED_NORMAL signal to trigger retry
        workflowDriver.signalWorkflowInstance(workflowInstanceId, "approval", SignalAction.FAILED_NORMAL);
        Thread.sleep(2_000L);

        // Activity should retry and go to SIGNAL_WAITING again
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "waitForApproval");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING ||
               activityInstance.getState() == ActivityState.FAILED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }
}
