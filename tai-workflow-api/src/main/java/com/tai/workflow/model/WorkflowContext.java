package com.tai.workflow.model;

import com.tai.workflow.utils.CheckUtils;
import com.tai.workflow.utils.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
@Slf4j
public class WorkflowContext {
    @JsonIgnore
    private WorkflowDefinitionInternal workflowDefinitionInternal;

    @JsonIgnore
    private ActivityDefinition activityDefinition;

    @JsonIgnore
    private WorkflowInstance workflowInstance;

    @JsonIgnore
    private ActivityInstance activityInstance;

    @JsonIgnore
    private Boolean resetRetryCount;

    private Map<String, Object> contextParams = new HashMap<>();

    public void setContextParam(String key, Object value) {
        this.contextParams.put(key, value);
    }

    public <T> T getContextParam(String key, Class<T> targetClass) {
        Object paramValue = this.contextParams.get(key);
        if (Objects.isNull(paramValue)) {
            return null;
        } else {
            try {
                return JsonUtils.convertValue(paramValue, targetClass);
            } catch (Exception e) {
                log.warn("fail to get context param for {}", key, e);
                return null;
            }
        }
    }

    public <T> T getContextParam(String key, final TypeReference<T> toValueTypeRef) {
        Object paramValue = this.contextParams.get(key);
        if (Objects.isNull(paramValue)) {
            return null;
        } else {
            try {
                return JsonUtils.convertValue(paramValue, toValueTypeRef);
            } catch (Exception e) {
                log.warn("fail to get context param for {}", key, e);
                return null;
            }
        }
    }

    public <T> T getContextParamFromJson(String key, final TypeReference<T> toValueTypeRef) {
        String paramValue = (String) this.contextParams.get(key);
        if (Objects.isNull(paramValue)) {
            return null;
        } else {
            try {
                return JsonUtils.fromJson(paramValue, toValueTypeRef);
            } catch (Exception e) {
                log.warn("fail to get context param for {}", key, e);
                return null;
            }
        }
    }

    public <T> T getActivityPayload(Class<T> targetClass) {
        try {
            return JsonUtils.convertValue(this.activityDefinition.getPayload(), targetClass);
        } catch (Exception e) {
            log.warn("fail to get context param for {}",
                    this.activityDefinition.getPayload() == null ? null : JsonUtils.toJson(this.activityDefinition.getPayload()), e);
            return null;
        }
    }

    public <T> T getActivityPayload(TypeReference<T> toValueTypeRef) {
        try {
            return JsonUtils.convertValue(this.activityDefinition.getPayload(), toValueTypeRef);
        } catch (Exception e) {
            log.warn("fail to get context param for {}", JsonUtils.toJson(this.activityDefinition.getPayload()), e);
            return null;
        }
    }

    public void mergeContext(final WorkflowContext workflowContext) {
        workflowContext.consumeContextParams(this::setContextParam);
    }

    public void mergeContext(final Map<String, Object> contextParams) {
        if (MapUtils.isNotEmpty(contextParams)) {
            contextParams.forEach(this::setContextParam);
        }
    }

    public Map<String, Object> getAllContextParams() {
        return Collections.unmodifiableMap(this.contextParams);
    }

    public void consumeContextParams(final BiConsumer<String, Object> biConsumer) {
        CheckUtils.checkNotNull(biConsumer, WorkflowConstants.INVALID_PARAMETER, "biConsumer should not be null!");
        this.contextParams.forEach(biConsumer);
    }
}
