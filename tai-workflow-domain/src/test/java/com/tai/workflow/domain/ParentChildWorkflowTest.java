package com.tai.workflow.domain;

import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.domain.activity.StartChildWorkflowActivity;
import com.tai.workflow.domain.activity.UnifyActivity;
import com.tai.workflow.domain.util.WorkflowTestUtils;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.enums.SignalAction;
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
import java.util.Objects;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ParentChildWorkflowTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testParentChildWorkflow() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("parentWorkflow")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("child")
                        .activityClass(StartChildWorkflowActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", "child")
                .addEdge("child", "end")
                .definitionVariables(Map.of("name1", "test-value-1", "name2", "test-value-2"))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId + 1);
        ActivityInstance start = workflowDriver.getActivityInstance(workflowInstanceId, "start");
        assert start.getState() == ActivityState.COMPLETED;
        ActivityInstance child = workflowDriver.getActivityInstance(workflowInstanceId, "child");
        assert child.getState() == ActivityState.SIGNAL_WAITING;
        ActivityInstance end = workflowDriver.getActivityInstance(workflowInstanceId, "end");
        assert end.getState() == ActivityState.PENDING;

        Thread.sleep(5_000);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;
        List<ActivityInstance> activityInstances = workflowDriver.listActivityInstances(workflowInstanceId);

        for (ActivityInstance activityInstance : activityInstances) {
            assert activityInstance.getState() == ActivityState.COMPLETED;
        }

        WorkflowInstance childWorkflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId + 1);
        assert childWorkflowInstance.getState() == WorkflowState.COMPLETED;
        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        System.out.println("childContextParams:" + childWorkflowInstance.getContextParams());
        System.out.println("parentContextParams:" + workflowInstance.getContextParams());
        assert Objects.equals(workflowInstance.getContextParams().get("name"), "test-value");
        assert Objects.equals(workflowInstance.getContextParams().get("name1"), "test-value-1");
    }

    @Test
    public void testParentChildWorkflowWithIsolateConfig() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("parentWorkflowWithIsolateConfig")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("child")
                        .activityClass(StartChildWorkflowActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .isolate(true)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", "child")
                .addEdge("child", "end")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId + 1);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;
        WorkflowInstance childWorkflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId + 1);
        assert childWorkflowInstance.getState() == WorkflowState.RUNNING;
    }

    @Test
    public void testParentChildWorkflowWithIsolateConfigAndSignalBizCode() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("parentWorkflowWithIsolateConfig")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("child")
                        .activityClass(StartChildWorkflowActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .isolate(true)
                        .signalBizCode("child")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", "child")
                .addEdge("child", "end")
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId + 1);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.RUNNING;
        Thread.sleep(5_000);
        WorkflowInstance childWorkflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId + 1);
        assert childWorkflowInstance.getState() == WorkflowState.COMPLETED;
        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.RUNNING;

        ActivityInstance activityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "child");
        assert activityInstance.getState() == ActivityState.SIGNAL_WAITING;

        workflowDriver.signalWorkflowInstance(workflowInstanceId, "child", SignalAction.SUCCESS, Map.of("emr", "test-value"));
        Thread.sleep(2_000);
        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;
        assert workflowInstance.getContextParams().get("emr").equals("test-value");
    }

    @Test
    public void testRetryWorkflowWhenSubWorkflowInstanceFailed() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testRetryWorkflowWhenSubWorkflowInstanceFailed")
                .addNode(ActivityDefinition.builder()
                        .name("start")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("child")
                        .activityClass(StartChildWorkflowActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .maxRetry(3)
                        .retryIntervalMillis(100)
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("end")
                        .activityClass(UnifyActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .build())

                .addEdge("start", "child")
                .addEdge("child", "end")
                .definitionVariables(Map.of("childShouldFail", true))
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(5_000);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId);
        WorkflowTestUtils.printActivityDetail(workflowDriver, workflowInstanceId + 1);
        assert workflowInstance.getState() == WorkflowState.HUMAN_PROCESSING;
        List<WorkflowInstance> childWorkflowInstances = workflowDriver.findWorkflowInstanceByParentId(workflowInstanceId);
        assert childWorkflowInstances.size() == 1;
        WorkflowTestUtils.printActivityDetail(workflowDriver, childWorkflowInstances.get(0).getId());
        ActivityInstance childActivityInstance = workflowDriver.getActivityInstance(workflowInstanceId, "child");
        assert childActivityInstance.getState() == ActivityState.FAILED;
        assert childActivityInstance.getRetryCount() == 3;

        workflowDriver.signalWorkflowInstance(workflowInstanceId, childActivityInstance.getSignalBizCode(), SignalAction.SUCCESS);
        Thread.sleep(2_000);
        workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        assert workflowInstance.getState() == WorkflowState.COMPLETED;

        WorkflowInstance childWorkflowInstance = workflowDriver.getWorkflowInstance(childWorkflowInstances.get(0).getId());
        assert childWorkflowInstance.getState() == WorkflowState.FAILED;
    }
}
