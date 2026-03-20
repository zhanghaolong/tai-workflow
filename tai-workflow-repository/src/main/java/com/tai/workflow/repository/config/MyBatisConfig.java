package com.tai.workflow.repository.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置类
 *
 * @author zhanghaolong1989@163.com
 */
@Configuration
@MapperScan("com.tai.workflow.repository.mapper")
@ConditionalOnProperty(
        prefix = "workflow.mybatis",
        name = "enable",
        havingValue = "true",
        matchIfMissing = false
)
public class MyBatisConfig {
}
