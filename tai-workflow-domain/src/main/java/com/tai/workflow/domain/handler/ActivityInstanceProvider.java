package com.tai.workflow.domain.handler;

import com.tai.workflow.model.ActivityInstance;

/**
 * @author zhanghaolong1989@163.com
 */
public interface ActivityInstanceProvider {
    ActivityInstance provide();
}
