package com.tai.workflow.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhanghaolong1989@163.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityExecutionWrapperException extends RuntimeException {
    private Throwable throwable;
}
