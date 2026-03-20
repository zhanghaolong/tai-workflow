package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.FailOnceActivityForResume;
import com.tai.workflow.domain.activity.FailOnceActivityForStartWithSameToken;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.domain.util.WorkflowTestUtils;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ResumeWorkflowTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @BeforeEach
    void resetFailOnceCounters() {
        FailOnceActivityForResume.count = 0;
        FailOnceActivityForStartWithSameToken.count = 0;
    }

    @Test
    public void testResumeWorkflowInstance() throws InterruptedException {
        FailOnceActivityForResume.count = 0;
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("resumeWorkflowInstance")
                .addNode(ActivityDefinition.builder()
                        .name("failOnceActivity")
                        .activityClass(FailOnceActivityForResume.class)
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
                        .build())

                .addEdge("failOnceActivity", List.of("reserveCar", "reserveHotel", "reservePark"))
                .addEdge(Set.of("reserveCar", "reserveHotel", "reservePark"), "end")
                .definitionVariables(Map.of("name", "test-value"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(1_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.HUMAN_PROCESSING;

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        Optional<ActivityInstance> activityInstance1 = activityInstances.stream().filter(ac -> "failOnceActivity".equals(ac.getName())).findFirst();
        assert activityInstance1.get().getState() == ActivityState.FAILED;

        for (ActivityInstance activityInstance : activityInstances.stream()
                .filter(ac -> !"failOnceActivity".equals(ac.getName()))
                .collect(Collectors.toList())) {
            assert ActivityState.PENDING == activityInstance.getState();
        }

        workflowDriver.retryWorkflowInstance(workflowInstanceId);
        Thread.sleep(2_000);
        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        for (ActivityInstance activityInstance : activityInstances) {
            assert ActivityState.COMPLETED == activityInstance.getState();
        }
    }

    // 由于使用同一 token 第二次启动时等价于 resume 故放到同一测试套中
    @Test
    public void testStartWorkflowInstanceWithSameToken() throws InterruptedException {
        FailOnceActivityForStartWithSameToken.count = 0;
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("startWorkflowInstanceWithSameToken")
                .addNode(ActivityDefinition.builder()
                        .name("failOnceActivity")
                        .activityClass(FailOnceActivityForStartWithSameToken.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .maxRetry(0)
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
                        .build())

                .addEdge("failOnceActivity", List.of("reserveCar", "reserveHotel", "reservePark"))
                .addEdge(Set.of("reserveCar", "reserveHotel", "reservePark"), "end")
                .definitionVariables(Map.of("name", "test-value"))
                .build();

        final String token = "token" + UUID.randomUUID().toString();
        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition, token);
        Thread.sleep(3_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.HUMAN_PROCESSING;
        assert token.equals(workflowInstance.getToken());

        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        Optional<ActivityInstance> activityInstance1 = activityInstances.stream().filter(ac -> "failOnceActivity".equals(ac.getName())).findFirst();
        assert activityInstance1.get().getState() == ActivityState.FAILED;

        for (ActivityInstance activityInstance : activityInstances.stream()
                .filter(ac -> !"failOnceActivity".equals(ac.getName()))
                .collect(Collectors.toList())) {
            assert ActivityState.PENDING == activityInstance.getState();
        }

        workflowDriver.startWorkflowInstance(workflowDefinition, token);
        Thread.sleep(2_000);
        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);
        for (ActivityInstance activityInstance : activityInstances) {
            assert ActivityState.COMPLETED == activityInstance.getState();
        }
    }
}
