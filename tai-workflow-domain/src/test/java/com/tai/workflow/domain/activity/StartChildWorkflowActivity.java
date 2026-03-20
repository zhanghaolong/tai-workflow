package com.tai.workflow.domain.activity;

import com.tai.workflow.api.Activity;
import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityExecutionResult;
import com.tai.workflow.model.WorkflowContext;
import com.tai.workflow.model.WorkflowDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

@Component
public class StartChildWorkflowActivity implements Activity {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Override
    public ActivityExecutionResult invoke(WorkflowContext context) throws InterruptedException {
        Boolean shouldFail = context.getContextParam("childShouldFail", Boolean.class);
        if (Boolean.TRUE.equals(shouldFail)) {
            return invokeChildWithFailedActivity(context);
        }
        return invokeChild(context);
    }

    public ActivityExecutionResult invokeChild(WorkflowContext context) throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("childWorkflow")
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
                        .name("sleep")
                        .activityClass(SleepActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addEdge("reserveCar", "reserveHotel")
                .addEdge("reserveHotel", "reservePark")
                .addEdge("reservePark", "sleep")
                .definitionVariables(Map.of("name", "test-value", "name1", "test-value-1"))
                .mergeToParentContextKeys(Set.of("name", "name1"))
                .build();

        workflowDriver.startWorkflowInstance(workflowDefinition);
        return ActivityExecutionResult.ofSucceeded(
                Map.of(StartChildWorkflowActivity.class.getSimpleName(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
    }

    public ActivityExecutionResult invokeChildWithFailedActivity(WorkflowContext context) throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("childWorkflowWithFailedActivity")
                .addNode(ActivityDefinition.builder()
                        .name("reserveCar")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.ROLLBACK)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("failAlwaysActivity")
                        .activityClass(FailAlwaysActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.ROLLBACK)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("reservePark")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.ROLLBACK)
                        .build())
                .addEdge("reserveCar", "failAlwaysActivity")
                .addEdge("failAlwaysActivity", "reservePark")
                .build();

        workflowDriver.startWorkflowInstance(workflowDefinition);
        return ActivityExecutionResult.ofSucceeded(
                Map.of(StartChildWorkflowActivity.class.getSimpleName(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));
    }
}
