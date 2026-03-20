package com.tai.workflow.domain.executor;

import com.tai.workflow.domain.service.ActivityInstanceService;
import com.tai.workflow.domain.service.WorkflowDefinitionService;
import com.tai.workflow.enums.ActivityState;
import com.tai.workflow.model.ActivityDefinition;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.WorkflowInstance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Component
public class DagExecutionStrategyExecutor implements ApplicationContextAware, InitializingBean {
    private final WorkflowDefinitionService workflowDefinitionService;
    private final ActivityInstanceService activityInstanceService;
    private ApplicationContext applicationContext;
    private Collection<? extends DagExecutionStrategy> allSupportedExecutionStrategies;

    @Autowired
    public DagExecutionStrategyExecutor(WorkflowDefinitionService workflowDefinitionService, ActivityInstanceService activityInstanceService) {
        this.workflowDefinitionService = workflowDefinitionService;
        this.activityInstanceService = activityInstanceService;
    }

    public void afterPropertiesSet() {
        allSupportedExecutionStrategies = Optional.of(applicationContext.getBeansOfType(DagExecutionStrategy.class))
                .orElse(new HashMap<>())
                .values()
                .stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .collect(Collectors.toList());
    }

    public void execute(WorkflowInstance workflowInstance, String lastActivityName, ActivityState lastActivityState) {
        DagExecutionContext dagExecutionContext = makeDagExecutionContext(workflowInstance, lastActivityName, lastActivityState);
        Optional<? extends DagExecutionStrategy> dagExecutionStrategyOptional = allSupportedExecutionStrategies.stream()
                .filter((dagExecutionStrategy) -> dagExecutionStrategy.support(dagExecutionContext))
                .findFirst();
        dagExecutionStrategyOptional.ifPresent((dagExecutionStrategy) -> dagExecutionStrategy.execute(dagExecutionContext));
    }

    private DagExecutionContext makeDagExecutionContext(WorkflowInstance workflowInstance, String activityName, ActivityState activityState) {
        DagExecutionContext dagExecutionContext = new DagExecutionContext();
        dagExecutionContext.setLastActivityName(activityName);
        dagExecutionContext.setWorkflowInstance(workflowInstance);
        dagExecutionContext.setLastActivityState(activityState);
        if (StringUtils.isNotBlank(activityName)) {
            ActivityDefinition activityDefinition = workflowDefinitionService.findActivityDefinition(workflowInstance.getWorkflowDefinitionId(),
                    activityName);
            ActivityInstance activityInstance = activityInstanceService.findActivityInstance(workflowInstance.getId(), activityName);
            dagExecutionContext.setActivityDefinition(activityDefinition);
            dagExecutionContext.setActivityInstance(activityInstance);
        }

        return dagExecutionContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
