package com.tai.workflow.domain.convert;

import com.tai.workflow.utils.JsonUtils;
import com.tai.workflow.graph.WorkflowDag;
import com.tai.workflow.model.ActivityInstance;
import com.tai.workflow.model.BriefActivityInstance;
import com.tai.workflow.model.WorkflowDefinitionInternal;
import com.tai.workflow.model.WorkflowInstance;
import com.tai.workflow.repository.entity.ActivityInstanceEntity;
import com.tai.workflow.repository.entity.ActivityInstanceSimpleEntity;
import com.tai.workflow.repository.entity.WorkflowDefinitionEntity;
import com.tai.workflow.repository.entity.WorkflowInstanceEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Mapper(componentModel = "spring")
public interface WorkflowConvert {
    BriefActivityInstance toBriefActivityInstance(ActivityInstanceSimpleEntity activityInstanceSimpleEntity);

    @Mappings({@Mapping(source = "workflowDag", target = "dag", qualifiedByName = "fromDagToJson"),
            @Mapping(source = "definitionVariables", target = "variables", qualifiedByName = "fromMapToJson")})
    WorkflowDefinitionEntity convert(WorkflowDefinitionInternal workflowDefinitionInternal);

    @Mappings({@Mapping(source = "dag", target = "workflowDag", qualifiedByName = "fromJsonToDag"),
            @Mapping(source = "variables", target = "definitionVariables", qualifiedByName = "fromJsonToMap")})
    WorkflowDefinitionInternal convert(WorkflowDefinitionEntity workflowDefinitionEntity);

    @Mappings({@Mapping(source = "inputContext", target = "inputContext", qualifiedByName = "fromJsonToMap"),
            @Mapping(source = "outputContext", target = "outputContext", qualifiedByName = "fromJsonToMap")})
    ActivityInstance convert(ActivityInstanceEntity activityInstanceEntity);

    ActivityInstance convert(ActivityInstanceSimpleEntity activityInstanceEntity);

    @Mappings({@Mapping(source = "inputContext", target = "inputContext", qualifiedByName = "fromMapToJson"),
            @Mapping(source = "outputContext", target = "outputContext", qualifiedByName = "fromMapToJson")})
    ActivityInstanceEntity convert(ActivityInstance activityInstance);

    @Mappings({@Mapping(source = "context", target = "contextParams", qualifiedByName = "fromJsonToMap"),
            @Mapping(source = "definitionVariables", target = "definitionVariables", qualifiedByName = "fromJsonToMap")})
    WorkflowInstance convert(WorkflowInstanceEntity workflowInstanceEntity);

    @Mappings({@Mapping(source = "contextParams", target = "context", qualifiedByName = "fromMapToJson"),
            @Mapping(source = "definitionVariables", target = "definitionVariables", qualifiedByName = "fromMapToJson"),
            @Mapping(source = "version", target = "version", qualifiedByName = "changeNullToZero")})
    WorkflowInstanceEntity convert(WorkflowInstance workflowInstance);

    @Named("fromDagToJson")
    default String fromDagToJson(WorkflowDag workflowDag) {
        return JsonUtils.toJson(workflowDag);
    }

    @Named("fromMapToJson")
    default String fromMapToJson(Map<String, Object> variables) {
        return JsonUtils.toJson(MapUtils.isEmpty(variables) ? new HashMap<>() : variables);
    }

    @Named("fromJsonToDag")
    default WorkflowDag fromJsonToDag(String dagJson) {
        if (StringUtils.isNotBlank(dagJson)) {
            return JsonUtils.fromJson(dagJson, WorkflowDag.class);
        }

        return null;
    }

    @Named("fromJsonToMap")
    default Map<String, Object> fromJsonToMap(String variables) {
        if (StringUtils.isNotBlank(variables)) {
            return JsonUtils.fromJson(variables, new TypeReference<>() {
            });
        }

        return new HashMap<>();
    }

    @Named("changeNullToZero")
    default Integer changeNullToZero(Integer version) {
        return Objects.isNull(version) ? 0 : version;
    }
}
