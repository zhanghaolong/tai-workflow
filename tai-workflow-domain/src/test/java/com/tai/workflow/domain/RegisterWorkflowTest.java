package com.tai.workflow.domain;

import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.utils.JsonUtils;
import com.tai.workflow.api.WorkflowDriver;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RegisterWorkflowTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testRegisterWorkflowInstance() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("registerWorkflowInstance")
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
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .maxRetry(10)
                        .retryIntervalMillis(10_000L)
                        .build())

                .addEdge("start", List.of("reserveCar", "reserveHotel", "reservePark"))
                .addEdge(Set.of("reserveCar", "reserveHotel", "reservePark"), "end")
                .definitionVariables(Map.of("name", "test-value"))
                .build();

        workflowDriver.registerWorkflowDefinition(workflowDefinition);
        Long workflowInstanceId = workflowDriver.startWorkflowInstance("registerWorkflowInstance", Map.of("name", "test-value"));

        Thread.sleep(3_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        for (ActivityInstance activityInstance : activityInstances) {
            assert ActivityState.COMPLETED == activityInstance.getState();
        }

        System.out.println(JsonUtils.toJson(workflowInstance.getContextParams()));
        assert workflowInstance.getContextParams().get("name").equals("test-value");
    }
}
