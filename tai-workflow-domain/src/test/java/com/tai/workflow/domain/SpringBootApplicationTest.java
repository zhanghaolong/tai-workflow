package com.tai.workflow.domain;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EntityScan("com.tai.workflow")
@SpringBootApplication(scanBasePackages = {"com.tai.workflow"})
public class SpringBootApplicationTest {
}
