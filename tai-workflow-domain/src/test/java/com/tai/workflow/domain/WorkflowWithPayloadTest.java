package com.tai.workflow.domain;

import com.tai.workflow.domain.activity.StartExecutionActivity;
import com.tai.workflow.utils.JsonUtils;
import com.tai.workflow.api.WorkflowDriver;
import com.tai.workflow.enums.ActivityFailStrategy;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.WorkflowDefinition;
import com.tai.workflow.model.WorkflowInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class WorkflowWithPayloadTest {
    @Autowired
    private WorkflowDriver workflowDriver;

    @Test
    public void testWorkflowWithPayload() throws InterruptedException {
        WorkflowDefinition workflowDefinition = WorkflowDefinition.builder()
                .name("testWorkflowWithPayload")
                .addNode(ActivityDefinition.builder()
                        .name("installNameNode")
                        .activityClass(StartExecutionActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .payload("nameNode")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("installDataNode")
                        .activityClass(StartExecutionActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .payload("dataNode")
                        .build())
                .addNode(ActivityDefinition.builder()
                        .name("installYarnResourceManager")
                        .activityClass(StartExecutionActivity.class)
                        .activityFailStrategy(ActivityFailStrategy.HUMAN_PROCESSING)
                        .payload("resourceManager")
                        .build())
                .build();

        Long workflowInstanceId = workflowDriver.startWorkflowInstance(workflowDefinition);
        Thread.sleep(2_000L);
        WorkflowInstance workflowInstance = workflowDriver.getWorkflowInstance(workflowInstanceId);
        Map<String, Object> context = workflowInstance.getContextParams();
        System.out.println(JsonUtils.toJson(context));
        assert context.get("installDataNode").equals("dataNode");
        assert context.get("installNameNode").equals("nameNode");
        assert context.get("installYarnResourceManager").equals("resourceManager");
    }
}
