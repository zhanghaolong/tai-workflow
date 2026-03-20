package com.tai.workflow.domain;

import com.tai.workflow.configuration.WorkflowConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {SpringBootApplicationTest.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class WorkflowConfigurationTest {
    @Autowired
    private WorkflowConfiguration workflowConfiguration;

    @Test
    public void testGlobalConfiguration() {
        assert "test".equals(workflowConfiguration.getNamespace());
    }
}
