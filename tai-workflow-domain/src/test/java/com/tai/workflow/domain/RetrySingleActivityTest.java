package com.tai.workflow.domain;

import com.tai.workflow.domain.activity.FailOnceActivityForResume;
import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.domain.util.WorkflowTestUtils;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.enums.WorkflowState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RetrySingleActivityTest {
    @Autowired
    private WorkflowDriver workflowDriver;
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void resetFailOnceCounters() {
        FailOnceActivityForResume.count = 0;
    }

    @Test
    public void testRetrySingleActivity() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testRetrySingleActivity")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .displayName("启动工作流")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("failOnce")
                        .displayName("失败一次")
                        .activityClass(FailOnceActivityForResume.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .displayName("结束")
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", "failOnce")
                .addEdge("failOnce", "end")
                .definitionVariables(Map.of("name", "test-value"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000L);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "failOnce");
        workflowDriver.retrySingleActivityInstance(activityInstance.getId());
        Thread.sleep(5_000L);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);

        workflowDriver.retryWorkflowInstance(workflowInstanceId);
        Thread.sleep(5_000L);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        Assertions.assertEquals(WorkflowState.COMPLETED, workflowInstance.getState());

        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "failOnce");
        Date endTime1 = activityInstance.getEndTime();

        Date endTimeOfEndActivity1 = workflowDriver.getActivityInstance(workflowInstanceId, "end").getEndTime();

        workflowDriver.retrySingleActivityInstance(activityInstance.getId());
        Thread.sleep(5_000L);
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "failOnce");
        Date endTime2 = activityInstance.getEndTime();

        Date endTimeOfEndActivity2 = workflowDriver.getActivityInstance(workflowInstanceId, "end").getEndTime();

        Assertions.assertNotEquals(endTime1, endTime2);
        Assertions.assertEquals(endTimeOfEndActivity1, endTimeOfEndActivity2);

        Assertions.assertEquals("test-value", workflowInstance.getContextParams().get("name"));

        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "failOnce");
        System.out.println(activityInstance.getInputContext());
        System.out.println(activityInstance.getOutputContext());
        System.out.println(workflowDriver.getWorkflowInstance(workflowInstanceId).getContextParams());

        workflowDriver.refreshWorkflowContext(workflowInstanceId, null, Set.of("name"));
        workflowDriver.retrySingleActivityInstance(activityInstance.getId());
        Thread.sleep(5_000L);
        activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "failOnce");
        System.out.println(activityInstance.getInputContext());
        System.out.println(activityInstance.getOutputContext());
        System.out.println(workflowDriver.getWorkflowInstance(workflowInstanceId).getContextParams());
    }
}
