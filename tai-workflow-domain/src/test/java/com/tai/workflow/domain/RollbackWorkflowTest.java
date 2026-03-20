package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.FailAlwaysActivity;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.domain.util.WorkflowTestUtils;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RollbackWorkflowTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testRollbackWorkflowInstance() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("rollbackWorkflowInstance")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("failAlways")
                        .activityClass(FailAlwaysActivity.class)
                        .retryIntervalMillis(10)
                        .maxRetry(5)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", "failAlways")
                .addEdge("failAlways", "end")
                .description("testRollbackWorkflowInstance")
                .definitionVariables(Map.of("name", "test-value"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        // Wait enough time for the initial execution + 5 retries (retryIntervalMillis=10)
        Thread.sleep(2_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.HUMAN_PROCESSING;

        workflowDriver.rollbackWorkflowInstance(workflowInstanceId);
        Thread.sleep(1_000L);
        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.FAILED;

        assert workflowInstance.getContextParams().get("object") == null;

        ActivityInstance start = workflowDriver.getActivityInstance(workflowInstanceId, "start");
        assert start.getState() == ActivityState.COMPLETED;

        ActivityInstance fail = workflowDriver.getActivityInstance(workflowInstanceId, "failAlways");
        assert fail.getState() == ActivityState.FAILED;
        // The executionMsg contains the original failure exception stacktrace, not rollback output.
        // Rollback output is only appended when rollback() itself throws an exception.
        assert fail.getRetryCount() == 5;

        ActivityInstance end = workflowDriver.getActivityInstance(workflowInstanceId, "end");
        assert end.getState() == ActivityState.PENDING;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testMaterializeWorkflowInstance() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("materializeWorkflowInstance")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("failAlwaysWithMaterialize")
                        .activityClass(FailAlwaysActivity.class)
                        .retryIntervalMillis(10)
                        .maxRetry(5)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", "failAlwaysWithMaterialize")
                .addEdge("failAlwaysWithMaterialize", "end")
                .description("materializeWorkflowInstance")
                .definitionVariables(Map.of("name", "test-value", "materializeBeforeFail", true))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        // FailAlwaysActivity calls materialize() before throwing, so context changes are persisted
        assert workflowInstance.getState() == WorkflowState.HUMAN_PROCESSING;
        assert workflowInstance.getContextParams().get("object") != null;
    }
}
