package com.tai.workflow.graph;

import com.tai.workflow.graph.activity.NullableActivity;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowDefinitionBuilder;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WorkflowDagTest {
    @Test
    public void testTopologicalSort() {
        WorkflowDefinitionBuilder workflowDefinitionBuilder = WorkflowDefinition.builder()
                .name("testTopologicalSort")
                .addNode(ActivityDefinition.builder().name("startActivity").activityClass(NullableActivity.class).build())
                .addNode(ActivityDefinition.builder().name("reserveHotelActivity").activityClass(NullableActivity.class).build())
                .addNode(ActivityDefinition.builder().name("reserveAirlineActivity").activityClass(NullableActivity.class).build())
                .addNode(ActivityDefinition.builder().name("endActivity").activityClass(NullableActivity.class).build());
        workflowDefinitionBuilder.addEdge("startActivity", Set.of("reserveHotelActivity", "reserveAirlineActivity"));
        workflowDefinitionBuilder.addEdge(Set.of("reserveHotelActivity", "reserveAirlineActivity"), "endActivity");
        WorkflowDefinition workflowDefinition = workflowDefinitionBuilder.build();
        WorkflowDefinitionInternal workflowDefinitionInternal = WorkflowDefinitionInternal.toWorkflowDefinition(workflowDefinition);
        List<String> activityNames = workflowDefinitionInternal.getWorkflowDag().stream().map(WorkflowDagNode::getName).collect(Collectors.toList());
        assert activityNames.get(0).equals("startActivity");
        assert activityNames.get(3).equals("endActivity");
        assert !activityNames.get(1).equals(activityNames.get(2));
        assert Set.of("reserveHotelActivity", "reserveAirlineActivity").contains(activityNames.get(1));
        assert Set.of("reserveHotelActivity", "reserveAirlineActivity").contains(activityNames.get(2));
    }

    @Test
    public void testCheckDagCycle() {
        WorkflowDefinitionBuilder workflowDefinitionBuilder = WorkflowDefinition.builder()
                .name("checkDagCycleWorkflow1")
                .addNode(ActivityDefinition.builder().name("startActivity").activityClass(NullableActivity.class).build())
                .addNode(ActivityDefinition.builder().name("endActivity").activityClass(NullableActivity.class).build());
        workflowDefinitionBuilder.addEdge("startActivity", "endActivity");
        WorkflowDefinition workflowDefinition = workflowDefinitionBuilder.build();
        WorkflowDefinitionInternal workflowDefinitionInternal = WorkflowDefinitionInternal.toWorkflowDefinition(workflowDefinition);
        assert !workflowDefinitionInternal.getWorkflowDag().checkDagCycle();

        workflowDefinitionBuilder = WorkflowDefinition.builder()
                .name("checkDagCycleWorkflow2")
                .addNode(ActivityDefinition.builder().name("startActivity").activityClass(NullableActivity.class).build())
                .addNode(ActivityDefinition.builder().name("endActivity").activityClass(NullableActivity.class).build());
        workflowDefinitionBuilder.addEdge("startActivity", "endActivity");
        workflowDefinitionBuilder.addEdge("endActivity", "startActivity");
        workflowDefinition = workflowDefinitionBuilder.build();
        String exceptionMsg = null;
        try {
            WorkflowDefinitionInternal.toWorkflowDefinition(workflowDefinition);
        } catch (Exception e) {
            exceptionMsg = e.getMessage();
        }

        assert exceptionMsg.contains("DAG should not has cycle!");

        workflowDefinitionBuilder = WorkflowDefinition.builder()
                .name("checkDagCycleWorkflow3")
                .addNode(ActivityDefinition.builder().name("startActivity").activityClass(NullableActivity.class).build())
                .addNode(ActivityDefinition.builder().name("reserveHotelActivity").activityClass(NullableActivity.class).build())
                .addNode(ActivityDefinition.builder().name("endActivity").activityClass(NullableActivity.class).build());
        workflowDefinitionBuilder.addEdge("startActivity", "reserveHotelActivity");
        workflowDefinitionBuilder.addEdge("reserveHotelActivity", "endActivity");
        workflowDefinitionBuilder.addEdge("endActivity", "startActivity");
        workflowDefinition = workflowDefinitionBuilder.build();
        exceptionMsg = null;
        try {
            WorkflowDefinitionInternal.toWorkflowDefinition(workflowDefinition);
        } catch (Exception e) {
            exceptionMsg = e.getMessage();
        }
        assert exceptionMsg.contains("DAG should not has cycle!");
    }
}
