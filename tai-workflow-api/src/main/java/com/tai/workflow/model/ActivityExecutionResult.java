package com.tai.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class ActivityExecutionResult {
    public static final ActivityExecutionResult IGNORE = new ActivityExecutionResult();
    private boolean isSuccess = Boolean.TRUE;
    private String msg;
    private Map<String, Object> extValues = new HashMap<>();
    private boolean singleActivity = false;

    public static ActivityExecutionResult ofSucceeded() {
        ActivityExecutionResult result = new ActivityExecutionResult();
        result.setSuccess(true);
        return result;
    }

    public static ActivityExecutionResult ofSucceeded(final Map<String, Object> extValues) {
        ActivityExecutionResult result = new ActivityExecutionResult();
        result.setSuccess(true);
        if (MapUtils.isNotEmpty(extValues)) {
            result.extValues.putAll(extValues);
        }
        return result;
    }

    public static ActivityExecutionResult ofFailed(String msg) {
        ActivityExecutionResult result = new ActivityExecutionResult();
        result.setSuccess(false);
        result.setMsg(msg);
        return result;
    }
}
