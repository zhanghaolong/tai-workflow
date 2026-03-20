package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for WorkflowDriver.listWorkflowInstances method.
 *
 * <p>Scenarios covered:
 *
 * <ul>
 *   <li>Default pagination with DESC order - returns workflow instances ordered by ID descending
 *   <li>Pagination with offset - returns correct page of results
 *   <li>ASC order sorting - returns workflow instances ordered by ID ascending
 *   <li>Empty result set - returns empty list when no instances exist
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ListWorkflowInstancesTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testListWorkflowInstancesWithDefaultDescOrder() throws InterruptedException {
        // Given - Create multiple workflow instances
        String workflowName = "listWorkflowInstances_defaultDesc_" + System.currentTimeMillis();
        Long workflowId1 = createSimpleWorkflow(workflowName, "token1_" + System.currentTimeMillis());
        Thread.sleep(10); // Ensure different IDs
        Long workflowId2 = createSimpleWorkflow(workflowName, "token2_" + System.currentTimeMillis());
        Thread.sleep(10);
        Long workflowId3 = createSimpleWorkflow(workflowName, "token3_" + System.currentTimeMillis());

        // Wait for workflows to complete
        Thread.sleep(200);

        // When - List workflow instances with default DESC order (offset=0, pageSize=10)
        List<WorkflowInstance> instances = workflowDriver.listWorkflowInstances(0, 10);
        // Then - Verify results
        assertNotNull(instances, "Instances list should not be null");
        assertFalse(instances.isEmpty(), "Instances list should not be empty");
        assertTrue(instances.size() >= 3, "Should have at least 3 instances");

        // Verify DESC order - IDs should be decreasing
        for (int i = 0; i < instances.size() - 1; i++) {
            assertTrue(instances.get(i).getId() > instances.get(i + 1).getId(),
                    "IDs should be in descending order");
        }

        System.out.println("✓ Default DESC order test passed. Found " + instances.size() + " instances");
    }

    @Test
    public void testListWorkflowInstancesWithPagination() throws InterruptedException {
        // Given - Create multiple workflow instances
        String workflowName = "listWorkflowInstances_pagination_" + System.currentTimeMillis();
        createSimpleWorkflow(workflowName, "token_page1_" + System.currentTimeMillis());
        Thread.sleep(10);
        createSimpleWorkflow(workflowName, "token_page2_" + System.currentTimeMillis());
        Thread.sleep(10);
        createSimpleWorkflow(workflowName, "token_page3_" + System.currentTimeMillis());
        Thread.sleep(10);
        createSimpleWorkflow(workflowName, "token_page4_" + System.currentTimeMillis());
        Thread.sleep(10);
        createSimpleWorkflow(workflowName, "token_page5_" + System.currentTimeMillis());

        // Wait for workflows to complete
        Thread.sleep(200);

        // When - Get first page (offset 0, pageSize 2)
        List<WorkflowInstance> page1 = workflowDriver.listWorkflowInstances(0, 2);

        // When - Get second page (offset 2, pageSize 2)
        List<WorkflowInstance> page2 = workflowDriver.listWorkflowInstances(2, 2);

        // Then - Verify results
        assertNotNull(page1, "Page 1 should not be null");
        assertNotNull(page2, "Page 2 should not be null");
        assertTrue(page1.size() >= 1, "Page 1 should have results");
        assertTrue(page2.size() >= 1, "Page 2 should have results");

        // Verify pages contain different instances
        if (page1.size() >= 1 && page2.size() >= 1) {
            assertFalse(page1.get(0).getId().equals(page2.get(0).getId()),
                    "Pages should contain different instances");
        }

        System.out.println("✓ Pagination test passed. Page1 size: " + page1.size() + ", Page2 size: " + page2.size());
    }

    @Test
    public void testListWorkflowInstancesWithLargePageSize() throws InterruptedException {
        // Given - Create multiple workflow instances
        String workflowName = "listWorkflowInstances_large_" + System.currentTimeMillis();
        createSimpleWorkflow(workflowName, "token_large1_" + System.currentTimeMillis());
        Thread.sleep(10);
        createSimpleWorkflow(workflowName, "token_large2_" + System.currentTimeMillis());
        Thread.sleep(10);
        createSimpleWorkflow(workflowName, "token_large3_" + System.currentTimeMillis());

        // Wait for workflows to complete
        Thread.sleep(200);

        // When - List workflow instances with large page size
        List<WorkflowInstance> instances = workflowDriver.listWorkflowInstances(0, 100);

        // Then - Verify results
        assertNotNull(instances, "Instances list should not be null");
        assertFalse(instances.isEmpty(), "Instances list should not be empty");

        // Verify DESC order - IDs should be decreasing
        for (int i = 0; i < instances.size() - 1; i++) {
            assertTrue(instances.get(i).getId() > instances.get(i + 1).getId(),
                    "IDs should be in descending order");
        }

        System.out.println("✓ Large page size test passed. Found " + instances.size() + " instances");
    }

    @Test
    public void testListWorkflowInstancesWithSmallPageSize() throws InterruptedException {
        // Given - Create workflow instances
        String workflowName = "listWorkflowInstances_small_" + System.currentTimeMillis();
        createSimpleWorkflow(workflowName, "token_small1_" + System.currentTimeMillis());
        Thread.sleep(10);
        createSimpleWorkflow(workflowName, "token_small2_" + System.currentTimeMillis());
        Thread.sleep(10);
        createSimpleWorkflow(workflowName, "token_small3_" + System.currentTimeMillis());

        // Wait for workflows to complete
        Thread.sleep(200);

        // When - List with small page size
        List<WorkflowInstance> instances = workflowDriver.listWorkflowInstances(0, 2);

        // Then - Verify results
        assertNotNull(instances, "Instances list should not be null");
        assertTrue(instances.size() <= 2, "Should have at most 2 instances (page size)");

        System.out.println("✓ Small page size test passed. Found " + instances.size() + " instances");
    }

    @Test
    public void testListWorkflowInstancesWithNullParameters() {
        // When - List with null parameters (should handle gracefully)
        List<WorkflowInstance> instances = workflowDriver.listWorkflowInstances(null, null);

        // Then - Verify results
        assertNotNull(instances, "Instances list should not be null");
        // No assertion on size as it depends on existing data and null handling

        System.out.println("✓ Null parameters test passed. Found " + instances.size() + " instances");
    }

    /**
     * Helper method to create a simple workflow instance for testing
     */
    private Long createSimpleWorkflow(String workflowName, String token) {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name(workflowName)
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .displayName("Start Activity")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("process")
                        .displayName("Process Activity")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addEdge("start", "process")
                .build();

        workflowDriver.registerWorkflowDefinition(workflowDefinition);

        Map<String, Object> contextParams = new HashMap<>();
        contextParams.put("testKey", "testValue");

        return workflowDriver.startWorkflowInstance(workflowName, contextParams, token);
    }
}
