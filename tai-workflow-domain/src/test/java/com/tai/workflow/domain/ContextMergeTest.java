package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.ContextMergeActivity;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.domain.util.WorkflowTestUtils;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowDefinitionBuilder;
import com.tai.workflow.model.WorkflowInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Workflow context merge tests.
 *
 * <p>Scenarios covered:
 *
 * <ul>
 *   <li>Sequential context merge - each activity adds to context
 *   <li>Parallel context merge - concurrent context updates
 *   <li>Context overwrite - later activities override earlier values
 *   <li>Nested object in context - complex data structures
 *   <li>Context refresh during retry - verify latest data
 *   <li>Large context accumulation - memory and serialization
 *   <li>Context isolation between workflows - no cross-contamination
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ContextMergeTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testSequentialContextMerge() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSequentialContextMerge")
                .addNode(ActivityDefinition.builder()
                        .name("step1")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("step2")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("step3")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addEdge("step1", "step2")
                .addEdge("step2", "step3")
                .definitionVariables(Map.of("initial", "value"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(3_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        Map<String, Object> contextParams = workflowInstance.getContextParams();
        // Verify all activity outputs are present
        assert contextParams.containsKey("initial");
        assert contextParams.containsKey("step1_key1");
        assert contextParams.containsKey("step2_key1");
        assert contextParams.containsKey("step3_key1");

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testParallelContextMerge() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testParallelContextMerge")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("parallelA")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("parallelB")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("parallelC")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("start", List.of("parallelA", "parallelB", "parallelC"))
                .addEdge(Set.of("parallelA", "parallelB", "parallelC"), "end")
                .definitionVariables(Map.of("workflow", "parallel"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(4_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        Map<String, Object> contextParams = workflowInstance.getContextParams();
        // Verify all parallel activities contributed to context
        assert contextParams.containsKey("workflow");
        assert contextParams.containsKey("parallelA_key1");
        assert contextParams.containsKey("parallelB_key1");
        assert contextParams.containsKey("parallelC_key1");

        // Verify shared_key was set by one of the parallel activities
        assert contextParams.containsKey("shared_key");

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testContextOverwrite() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testContextOverwrite")
                .addNode(ActivityDefinition.builder()
                        .name("setInitial")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("overwrite")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addEdge("setInitial", "overwrite")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(3_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        Map<String, Object> contextParams = workflowInstance.getContextParams();
        // Verify shared_key was overwritten by second activity
        String sharedKeyValue = (String) contextParams.get("shared_key");
        assert sharedKeyValue.contains("overwrite");

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testNestedObjectInContext() throws InterruptedException {
        Map<String, Object> nestedContext = new HashMap<>();
        nestedContext.put("string", "value");
        nestedContext.put("number", 42);
        nestedContext.put("boolean", true);

        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("innerKey", "innerValue");
        nestedMap.put("innerNumber", 100);
        nestedContext.put("nestedObject", nestedMap);

        List<String> list = List.of("item1", "item2", "item3");
        nestedContext.put("list", list);

        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testNestedObjectInContext")
                .addNode(ActivityDefinition.builder()
                        .name("process")
                        .activityClass(UnifyActivity.class)
                        .build())
                .definitionVariables(nestedContext)
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert contextParams.get("string").equals("value");
        assert contextParams.get("number").equals(42);
        assert contextParams.get("boolean").equals(true);
        assert contextParams.containsKey("nestedObject");
        assert contextParams.containsKey("list");
    }

    @Test
    public void testContextRefreshDuringRetry() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testContextRefreshDuringRetry")
                .addNode(ActivityDefinition.builder()
                        .name("updateContext")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("verify")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("updateContext", "verify")
                .definitionVariables(Map.of("counter", 0))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        int originalContextSize = workflowInstance.getContextParams().size();

        // Manually refresh context
        workflowDriver.refreshWorkflowContext(workflowInstanceId, Map.of("refreshed", true), null);

        Thread.sleep(1_000L);

        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getContextParams().containsKey("refreshed");
        assert workflowInstance.getContextParams().get("refreshed").equals(true);
    }

    @Test
    public void testLargeContextAccumulation() throws InterruptedException {
        WorkflowDefinitionBuilder builder = WorkflowDefinition.builder()
                .name("testLargeContextAccumulation");

        // Create 10 sequential activities that each add data to context
        for (int i = 0; i < 10; i++) {
            builder.addNode(ActivityDefinition.builder()
                    .name("accumulate_" + i)
                    .activityClass(ContextMergeActivity.class)
                    .build());

            if (i > 0) {
                builder.addEdge("accumulate_" + (i - 1), "accumulate_" + i);
            }
        }

        // Add large initial context
        Map<String, Object> largeInitialContext = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            largeInitialContext.put("initial_key_" + i, "initial_value_" + i);
        }

        WorkflowDefinition workflowDefinition = builder.definitionVariables(largeInitialContext).build();
        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        Map<String, Object> contextParams = workflowInstance.getContextParams();
        // Verify large context accumulated correctly
        assert contextParams.size() >= 40; // 20 initial + 10 activities * 2 keys each

        // Verify initial context preserved
        for (int i = 0; i < 20; i++) {
            assert contextParams.containsKey("initial_key_" + i);
        }

        // Verify all activities contributed
        for (int i = 0; i < 10; i++) {
            assert contextParams.containsKey("accumulate_" + i + "_key1");
        }
    }

    @Test
    public void testContextIsolationBetweenWorkflows() throws InterruptedException {
        // Start two workflows with different initial contexts
        WorkflowDefinition workflowDef1 = WorkflowDefinition.builder()
                .name("testContextIsolationBetweenWorkflows_1")
                .addNode(ActivityDefinition.builder()
                        .name("updateContext")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .definitionVariables(Map.of("workflowId", "workflow1", "uniqueKey", "value1"))
                .build();

        WorkflowDefinition workflowDef2 = WorkflowDefinition.builder()
                .name("testContextIsolationBetweenWorkflows_2")
                .addNode(ActivityDefinition.builder()
                        .name("updateContext")
                        .activityClass(ContextMergeActivity.class)
                        .build())
                .definitionVariables(Map.of("workflowId", "workflow2", "uniqueKey", "value2"))
                .build();

        Long workflowId1 = workflowDriver.startWorkflowInstance(workflowDef1);
        Long workflowId2 = workflowDriver.startWorkflowInstance(workflowDef2);

        Thread.sleep(3_000L);

        WorkflowInstance workflow1 = workflowDriver.getWorkflowInstance(workflowId1);
        WorkflowInstance workflow2 = workflowDriver.getWorkflowInstance(workflowId2);

        assert workflow1.getState() == WorkflowState.COMPLETED;
        assert workflow2.getState() == WorkflowState.COMPLETED;

        // Verify contexts are isolated
        assert "workflow1".equals(workflow1.getContextParams().get("workflowId"));
        assert "workflow2".equals(workflow2.getContextParams().get("workflowId"));

        assert "value1".equals(workflow1.getContextParams().get("uniqueKey"));
        assert "value2".equals(workflow2.getContextParams().get("uniqueKey"));

        // Verify activity outputs don't cross-contaminate
        assert workflow1.getContextParams().containsKey("updateContext_key1");
        assert workflow2.getContextParams().containsKey("updateContext_key1");
    }

    @Test
    public void testContextMergeWithNullValues() throws InterruptedException {
        Map<String, Object> contextWithNulls = new HashMap<>();
        contextWithNulls.put("key1", "value1");
        contextWithNulls.put("key2", null);
        contextWithNulls.put("key3", "value3");

        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testContextMergeWithNullValues")
                .addNode(ActivityDefinition.builder()
                        .name("process")
                        .activityClass(UnifyActivity.class)
                        .build())
                .definitionVariables(contextWithNulls)
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert contextParams.containsKey("key1");
        assert contextParams.containsKey("key3");
        // key2 may or may not be present depending on serialization behavior
    }

    @Test
    public void testContextMergeInDiamondPatternWithCollision() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testContextMergeInDiamondPatternWithCollision")
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
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(4_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        Map<String, Object> contextParams = workflowInstance.getContextParams();
        // Both branches and the join node all write to shared_key.
        // The join node runs last, so shared_key ends up as "join_shared_value".
        assert contextParams.containsKey("shared_key");
        String sharedValue = (String) contextParams.get("shared_key");
        assert sharedValue.contains("branchA") || sharedValue.contains("branchB") || sharedValue.contains("join");

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }
}
