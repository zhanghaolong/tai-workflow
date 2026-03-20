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
import java.util.Objects;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class TerminateWorkflowTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testTerminateWorkflowInstance() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("terminateWorkflowInstance")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("failAlways")
                        .activityClass(FailAlwaysActivity.class)
                        .retryIntervalMillis(150)
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
                .description("testTerminateWorkflowInstance")
                .definitionVariables(Map.of("name", "test-value"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        int i = 1;
        while ((i++) < 10) {
            try {
                ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "failAlways");
                if (Objects.nonNull(activityInstance)) {
                    assert Set.of(ActivityState.RUNNING, ActivityState.PENDING).contains(activityInstance.getState());
                    break;
                }
            } catch (Exception ignore) {
            }
        }

        Thread.sleep(3_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.HUMAN_PROCESSING;

        workflowDriver.terminateWorkflowInstance(workflowInstanceId);
        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.TERMINATED;

        ActivityInstance start = workflowDriver.getActivityInstance(workflowInstanceId, "start");
        assert start.getState() == ActivityState.COMPLETED;
        ActivityInstance fail = workflowDriver.getActivityInstance(workflowInstanceId, "failAlways");
        assert fail.getState() == ActivityState.FAILED;
        ActivityInstance end = workflowDriver.getActivityInstance(workflowInstanceId, "end");
        assert end.getState() == ActivityState.CANCELLED;
    }
}
