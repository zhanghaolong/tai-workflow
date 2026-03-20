package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.domain.util.WorkflowTestUtils;
import com.tai.workflow.enums.ActivityFailStrategy;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow boundary condition tests.
 *
 * <p>Scenarios covered:
 *
 * <ul>
 *   <li>Empty context workflow execution
 *   <li>Null context parameters handling
 *   <li>Large context (10+ keys) serialization
 *   <li>Single activity workflow (minimal DAG)
 *   <li>Linear workflow (no parallelism)
 *   <li>Wide workflow (20+ parallel activities)
 *   <li>Deep workflow (10+ sequential activities)
 *   <li>Empty string in context values
 *   <li>Special characters in context values
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class WorkflowBoundaryConditionTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testWorkflowWithEmptyContext() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testWorkflowWithEmptyContext")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("start", "end")
                .build();

        // Start workflow with empty context
        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(3_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testWorkflowWithNullContext() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testWorkflowWithNullContext")
                .addNode(ActivityDefinition.builder()
                        .name("activity")
                        .activityClass(UnifyActivity.class)
                        .build())
                .build();

        // Start workflow with null context
        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition, null);
        Thread.sleep(2_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;
    }

    @Test
    public void testWorkflowWithLargeContext() throws InterruptedException {
        // Create large context with 50 keys
        Map<String, Object> largeContext = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            largeContext.put("key_" + i, "value_" + i + "_" + "x".repeat(100)); // 100 chars per value
        }

        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testWorkflowWithLargeContext")
                .addNode(ActivityDefinition.builder()
                        .name("process")
                        .activityClass(UnifyActivity.class)
                        .build())
                .definitionVariables(largeContext)
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify context preserved
        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert contextParams.size() >= 50;
        assert contextParams.containsKey("key_0");
        assert contextParams.containsKey("key_49");
    }

    @Test
    public void testSingleActivityWorkflow() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testSingleActivityWorkflow")
                .addNode(ActivityDefinition.builder()
                        .name("onlyActivity")
                        .activityClass(UnifyActivity.class)
                        .build())
                .definitionVariables(Map.of("test", "single"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        assert activityInstances.size() == 1;
        assert activityInstances.get(0).getState() == ActivityState.COMPLETED;
    }

    @Test
    public void testLinearWorkflow() throws InterruptedException {
        WorkflowDefinitionBuilder builder = WorkflowDefinition.builder()
                .name("testLinearWorkflow");

        // Create 10 sequential activities
        for (int i = 0; i < 10; i++) {
            builder.addNode(ActivityDefinition.builder()
                    .name("activity_" + i)
                    .activityClass(UnifyActivity.class)
                    .build());

            if (i > 0) {
                builder.addEdge("activity_" + (i - 1), "activity_" + i);
            }
        }

        WorkflowDefinition workflowDefinition = builder.build();
        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        assert activityInstances.size() == 10;

        for (ActivityInstance activityInstance : activityInstances) {
            assert activityInstance.getState() == ActivityState.COMPLETED;
        }

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }

    @Test
    public void testWideWorkflow() throws InterruptedException {
        WorkflowDefinitionBuilder builder = WorkflowDefinition.builder()
                .name("testWideWorkflow")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .build());

        // Create 20 parallel activities
        for (int i = 0; i < 20; i++) {
            builder.addNode(ActivityDefinition.builder()
                    .name("parallel_" + i)
                    .activityClass(UnifyActivity.class)
                    .build());
        }

        // Connect start to all parallel activities
        for (int i = 0; i < 20; i++) {
            builder.addEdge("start", "parallel_" + i);
        }

        // Connect all parallel activities to end
        for (int i = 0; i < 20; i++) {
            builder.addEdge("parallel_" + i, "end");
        }

        WorkflowDefinition workflowDefinition = builder.build();
        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(10_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        assert activityInstances.size() == 22; // start + 20 parallel + end

        for (ActivityInstance activityInstance : activityInstances) {
            assert activityInstance.getState() == ActivityState.COMPLETED;
        }
    }

    @Test
    public void testWorkflowWithSpecialCharactersInContext() throws InterruptedException {
        Map<String, Object> contextWithSpecialChars = new HashMap<>();
        contextWithSpecialChars.put("key_with_space", "value with spaces");
        contextWithSpecialChars.put("key\"with\"quotes", "value\"with\"quotes");
        contextWithSpecialChars.put("key\nwith\nnewline", "value\nwith\nnewline");
        contextWithSpecialChars.put("key\\with\\backslash", "value\\with\\backslash");
        contextWithSpecialChars.put("unicode_key", "中文测试Value");

        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testWorkflowWithSpecialCharactersInContext")
                .addNode(ActivityDefinition.builder()
                        .name("process")
                        .activityClass(UnifyActivity.class)
                        .build())
                .definitionVariables(contextWithSpecialChars)
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        // Verify special characters preserved
        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert "value with spaces".equals(contextParams.get("key_with_space"));
        assert "中文测试Value".equals(contextParams.get("unicode_key"));
    }

    @Test
    public void testWorkflowWithEmptyStringValues() throws InterruptedException {
        Map<String, Object> contextWithEmptyValues = new HashMap<>();
        contextWithEmptyValues.put("empty_string", "");
        contextWithEmptyValues.put("blank_string", "   ");
        contextWithEmptyValues.put("normal_string", "value");

        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testWorkflowWithEmptyStringValues")
                .addNode(ActivityDefinition.builder()
                        .name("process")
                        .activityClass(UnifyActivity.class)
                        .build())
                .definitionVariables(contextWithEmptyValues)
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        Map<String, Object> contextParams = workflowInstance.getContextParams();
        assert contextParams.containsKey("empty_string");
        assert contextParams.containsKey("blank_string");
    }

    @Test
    public void testDeepSequentialWorkflow() throws InterruptedException {
        WorkflowDefinitionBuilder builder = WorkflowDefinition.builder()
                .name("testDeepSequentialWorkflow");

        // Create 15 sequential activities (deep workflow)
        for (int i = 0; i < 15; i++) {
            builder.addNode(ActivityDefinition.builder()
                    .name("step_" + i)
                    .activityClass(UnifyActivity.class)
                    .build());

            if (i > 0) {
                builder.addEdge("step_" + (i - 1), "step_" + i);
            }
        }

        WorkflowDefinition workflowDefinition = builder.build();
        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(8_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        assert activityInstances.size() == 15;

        for (ActivityInstance activityInstance : activityInstances) {
            assert activityInstance.getState() == ActivityState.COMPLETED;
        }
    }

    @Test
    public void testWorkflowWithNullActivityOutput() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testWorkflowWithNullActivityOutput")
                .addNode(ActivityDefinition.builder()
                        .name("activityWithNullOutput")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("nextActivity")
                        .activityClass(UnifyActivity.class)
                        .build())
                .addEdge("activityWithNullOutput", "nextActivity")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(3_000L);

        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
    }
}
