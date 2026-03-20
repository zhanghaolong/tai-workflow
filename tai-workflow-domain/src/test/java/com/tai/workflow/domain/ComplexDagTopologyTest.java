package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.ContextMergeActivity;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.domain.util.WorkflowTestUtils;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowDefinitionBuilder;
import com.tai.workflow.model.WorkflowInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Complex DAG topology tests.
 *
 * <p>Scenarios covered:
 *
 * <ul>
 *   <li>Diamond DAG - single split and join
 *   <li>Multi-layer DAG - multiple split-join layers
 *   <li>Asymmetric DAG - uneven branch lengths
 *   <li>Multiple join points - complex convergence
 *   <li>Wide and deep DAG - both parallelism and depth
 *   <li>Context merge in diamond pattern - verify data flow
 *   <li>Fan-out fan-in pattern - 1-to-N-to-1
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ComplexDagTopologyTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testDiamondDagTopology() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testDiamondDagTopology")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("branchA")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("branchB")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("join")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("start", List.of("branchA", "branchB"))
                .addEdge(Set.of("branchA", "branchB"), "join")
                .definitionVariables(Map.of("pattern", "diamond"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(3_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify join activity received inputs from both branches
        ActivityInstance joinActivity = workflowDriver.getActivityInstance(workflowInstanceId, "join");
        assert joinActivity.getState() == ActivityState.COMPLETED;
        assert joinActivity.getInputContext().containsKey("pattern");

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testMultiLayerDagTopology() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testMultiLayerDagTopology")
                // Layer 0
                .addNode(ActivityDefinition.builder().name("L0").activityClass(UnifyActivity.class).build())
                // Layer 1
                .addNode(ActivityDefinition.builder().name("L1_A").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("L1_B").activityClass(UnifyActivity.class).build())
                // Layer 2
                .addNode(ActivityDefinition.builder().name("L2_A").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("L2_B").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("L2_C").activityClass(UnifyActivity.class).build())
                // Layer 3
                .addNode(ActivityDefinition.builder().name("L3").activityClass(UnifyActivity.class).build())
                // Edges
                .addEdge("L0", List.of("L1_A", "L1_B"))
                .addEdge("L1_A", List.of("L2_A", "L2_B"))
                .addEdge("L1_B", "L2_C")
                .addEdge(Set.of("L2_A", "L2_B", "L2_C"), "L3")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        assert activityInstances.size() == 7;

        for (ActivityInstance activityInstance : activityInstances) {
            assert activityInstance.getState() == ActivityState.COMPLETED;
        }

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testAsymmetricDagTopology() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testAsymmetricDagTopology")
                .addNode(ActivityDefinition.builder().name("start").activityClass(UnifyActivity.class).build())
                // Short branch: 1 activity
                .addNode(ActivityDefinition.builder().name("shortBranch").activityClass(UnifyActivity.class).build())
                // Long branch: 3 activities
                .addNode(ActivityDefinition.builder().name("longBranch1").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("longBranch2").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("longBranch3").activityClass(UnifyActivity.class).build())
                // Join
                .addNode(ActivityDefinition.builder().name("end").activityClass(UnifyActivity.class).build())
                // Edges
                .addEdge("start", List.of("shortBranch", "longBranch1"))
                .addEdge("longBranch1", "longBranch2")
                .addEdge("longBranch2", "longBranch3")
                .addEdge(Set.of("shortBranch", "longBranch3"), "end")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify end activity waited for both branches
        ActivityInstance endActivity = workflowDriver.getActivityInstance(workflowInstanceId, "end");
        assert endActivity.getState() == ActivityState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testMultipleJoinPointsDag() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testMultipleJoinPointsDag")
                .addNode(ActivityDefinition.builder().name("start").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("A").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("B").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("C").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("joinAB").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("joinBC").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("finalJoin").activityClass(UnifyActivity.class).build())
                // Edges
                .addEdge("start", List.of("A", "B", "C"))
                .addEdge(Set.of("A", "B"), "joinAB")
                .addEdge(Set.of("B", "C"), "joinBC")
                .addEdge(Set.of("joinAB", "joinBC"), "finalJoin")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        assert activityInstances.size() == 7;

        for (ActivityInstance activityInstance : activityInstances) {
            assert activityInstance.getState() == ActivityState.COMPLETED;
        }

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testWideAndDeepDagTopology() throws InterruptedException {
        WorkflowDefinitionBuilder builder = WorkflowDefinition.builder()
                .name("testWideAndDeepDagTopology")
                .addNode(ActivityDefinition.builder().name("start").activityClass(UnifyActivity.class).build());

        // Layer 1: 5 parallel branches
        for (int i = 0; i < 5; i++) {
            builder.addNode(ActivityDefinition.builder()
                    .name("L1_" + i)
                    .activityClass(UnifyActivity.class)
                    .build());
        }

        // Layer 2: 5 parallel branches (each dependent on corresponding L1)
        for (int i = 0; i < 5; i++) {
            builder.addNode(ActivityDefinition.builder()
                    .name("L2_" + i)
                    .activityClass(UnifyActivity.class)
                    .build());
        }

        // End node
        builder.addNode(ActivityDefinition.builder().name("end").activityClass(UnifyActivity.class).build());

        // Connect start to L1
        for (int i = 0; i < 5; i++) {
            builder.addEdge("start", "L1_" + i);
        }

        // Connect L1 to L2
        for (int i = 0; i < 5; i++) {
            builder.addEdge("L1_" + i, "L2_" + i);
        }

        // Connect all L2 to end
        for (int i = 0; i < 5; i++) {
            builder.addEdge("L2_" + i, "end");
        }

        WorkflowDefinition workflowDefinition = builder.build();
        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(7_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        assert activityInstances.size() == 12; // start + 5*L1 + 5*L2 + end

        for (ActivityInstance activityInstance : activityInstances) {
            assert activityInstance.getState() == ActivityState.COMPLETED;
        }
    }

    @Test
    public void testDiamondDagWithContextMerge() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testDiamondDagWithContextMerge")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("branchA")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("branchB")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("join")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addEdge("start", List.of("branchA", "branchB"))
                .addEdge(Set.of("branchA", "branchB"), "join")
                .definitionVariables(Map.of("initial_key", "initial_value"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(3_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify context contains outputs from all activities
        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert contextParams.containsKey("initial_key");
        assert contextParams.containsKey("start_key1");
        assert contextParams.containsKey("branchA_key1");
        assert contextParams.containsKey("branchB_key1");
        assert contextParams.containsKey("join_key1");

        // Verify shared_key was overwritten by last activity
        assert contextParams.containsKey("shared_key");

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testFanOutFanInPattern() throws InterruptedException {
        WorkflowDefinitionBuilder builder = WorkflowDefinition.builder()
                .name("testFanOutFanInPattern")
                .addNode(ActivityDefinition.builder().name("source").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("sink").activityClass(UnifyActivity.class).build());

        // Create 10 parallel processing activities
        for (int i = 0; i < 10; i++) {
            builder.addNode(ActivityDefinition.builder()
                    .name("processor_" + i)
                    .activityClass(UnifyActivity.class)
                    .build());
        }

        // Fan-out: connect source to all processors
        for (int i = 0; i < 10; i++) {
            builder.addEdge("source", "processor_" + i);
        }

        // Fan-in: connect all processors to sink
        for (int i = 0; i < 10; i++) {
            builder.addEdge("processor_" + i, "sink");
        }

        WorkflowDefinition workflowDefinition = builder.build();
        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(8_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        assert activityInstances.size() == 12; // source + 10 processors + sink

        for (ActivityInstance activityInstance : activityInstances) {
            assert activityInstance.getState() == ActivityState.COMPLETED;
        }
    }

    @Test
    public void testComplexMultiBranchConvergence() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testComplexMultiBranchConvergence")
                .addNode(ActivityDefinition.builder().name("root").activityClass(UnifyActivity.class).build())
                // Branch 1: A -> A1 -> A2
                .addNode(ActivityDefinition.builder().name("A").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("A1").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("A2").activityClass(UnifyActivity.class).build())
                // Branch 2: B -> B1
                .addNode(ActivityDefinition.builder().name("B").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("B1").activityClass(UnifyActivity.class).build())
                // Branch 3: C (single node)
                .addNode(ActivityDefinition.builder().name("C").activityClass(UnifyActivity.class).build())
                // Convergence nodes
                .addNode(ActivityDefinition.builder().name("merge1").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("merge2").activityClass(UnifyActivity.class).build())
                .addNode(ActivityDefinition.builder().name("final").activityClass(UnifyActivity.class).build())
                // Edges
                .addEdge("root", List.of("A", "B", "C"))
                .addEdge("A", "A1")
                .addEdge("A1", "A2")
                .addEdge("B", "B1")
                .addEdge(Set.of("A2", "B1"), "merge1")
                .addEdge(Set.of("C", "merge1"), "merge2")
                .addEdge("merge2", "final")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(6_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        assert activityInstances.size() == 10;

        for (ActivityInstance activityInstance : activityInstances) {
            assert activityInstance.getState() == ActivityState.COMPLETED;
        }

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }
}
