package com.tai.workflow.domain.service;

import com.tai.workflow.api.Activity;
import com.tai.workflow.domain.api.SingleActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import java.util.Objects;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
@Service
public class InvokeLocator implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public boolean existInvoke(Class<? extends Activity> activityClass) {
        try {
            return Objects.nonNull(applicationContext.getBean(activityClass));
        } catch (Exception e) {
            return false;
        }
    }

    public Activity getInvokeTarget(Class<? extends Activity> activityClass, boolean singleActivity) {
        if (singleActivity) {
            return new SingleActivity(applicationContext.getBean(activityClass));
        }

        return applicationContext.getBean(activityClass);
    }
}
