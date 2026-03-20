CREATE TABLE IF NOT EXISTS `workflow_definition`
(
  `id`              BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键',
  `namespace`       VARCHAR(128)          NOT NULL COMMENT '命名空间',
  `name`            VARCHAR(128)          NOT NULL COMMENT '流程的名称',
  `display_name`    VARCHAR(512)          COMMENT '显示名称',
  `description`     VARCHAR(1024)         COMMENT '流程描述',
  `worker_node`     VARCHAR(128)          DEFAULT NULL COMMENT '工作流被定义的节点身份 id',
  `variables`       CLOB                  NOT NULL COMMENT '定义的初始化变量',
  `dag`             CLOB                  NOT NULL COMMENT '存储流程的DAG图',
  `db_create_time`  TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time`  TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  INDEX `uk_namespace_name_worker_node` (`namespace`, `name`, `worker_node`)
);

CREATE TABLE IF NOT EXISTS `workflow_instance`
(
  `id`                     BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键',
  `token`                  VARCHAR(256)          DEFAULT NULL COMMENT '标识工作流实例Token',
  `namespace`              VARCHAR(128)          NOT NULL COMMENT '命名空间',
  `name`                   VARCHAR(128)          NOT NULL COMMENT '实例的名称',
  `display_name`           VARCHAR(128)          NOT NULL COMMENT '显示名称',
  `workflow_definition_id` BIGINT                NOT NULL COMMENT '定义的ID',
  `state`                  VARCHAR(32)           NOT NULL COMMENT '实例的状态',
  `start_time`             TIMESTAMP             NULL     DEFAULT NULL COMMENT '启动时间',
  `end_time`               TIMESTAMP             NULL     DEFAULT NULL COMMENT '结束时间',
  `parent_id`              BIGINT                DEFAULT NULL COMMENT '父工作流实例Id',
  `parent_activity_id`     BIGINT                DEFAULT NULL COMMENT '父工作流活动实例Id',
  `context`                CLOB                  COMMENT '流程实例context',
  `definition_variables`   CLOB                  COMMENT '定义的变量',
  `worker_node`            VARCHAR(128)          DEFAULT NULL COMMENT '工作流运行在的节点身份Id',
  `version`                INT UNSIGNED          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `biz_id`                 VARCHAR(128)          DEFAULT NULL COMMENT '业务id',
  `db_create_time`         TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time`         TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_namespace_token` (`namespace`, `token`),
  INDEX `idx_workflow_definition` (`workflow_definition_id`),
  INDEX `idx_parent` (`parent_id`, `parent_activity_id`),
  INDEX `idx_biz` (`biz_id`),
  INDEX `idx_namespace_start_worker_node` (`namespace`, `start_time`, `worker_node`)
);

CREATE TABLE IF NOT EXISTS `activity_instance`
(
  `id`                   BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键',
  `namespace`            VARCHAR(128)          NOT NULL COMMENT '命名空间',
  `name`                 VARCHAR(128)          NOT NULL COMMENT 'Activity 实例名称',
  `display_name`         VARCHAR(256)          DEFAULT NULL COMMENT 'Activity 实例展示名称',
  `workflow_instance_id` BIGINT                NOT NULL COMMENT '流程实例ID',
  `state`                VARCHAR(32)           NOT NULL COMMENT 'Activity 实例状态',
  `prev_state`           VARCHAR(32)           NULL     COMMENT 'Activity 实例前一状态',
  `execution_msg`        CLOB                  COMMENT '执行日志，主要以异常日志为主',
  `prev_execution_msg`   CLOB                  COMMENT '前一执行日志，主要以异常日志为主',
  `first_start_time`     TIMESTAMP             NULL     DEFAULT NULL COMMENT '第一次开始时间',
  `start_time`           TIMESTAMP             NULL     DEFAULT NULL COMMENT '开始时间',
  `end_time`             TIMESTAMP             NULL     DEFAULT NULL COMMENT '结束时间',
  `timeout_time`         TIMESTAMP             NULL     DEFAULT NULL COMMENT '超时时间',
  `retry_count`          INT                   NOT NULL DEFAULT '0' COMMENT '已经重试次数',
  `input_context`        CLOB                  COMMENT '输入的Context',
  `output_context`       CLOB                  COMMENT '输出的Context',
  `signal_biz_code`      VARCHAR(128)          DEFAULT NULL COMMENT '信号code',
  `db_create_time`       TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time`       TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_workflowinstanceid_name` (`workflow_instance_id`, `name`),
  INDEX `idx_workflowinstanceid_state` (`workflow_instance_id`, `state`),
  INDEX `idx_signal_biz_code` (`signal_biz_code`),
  INDEX `idx_namespace_timeout` (`namespace`, `timeout_time`)
);

CREATE TABLE IF NOT EXISTS `workflow_signal_record`
(
  `id`                   BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键',
  `workflow_instance_id` BIGINT                NOT NULL COMMENT '工作流实例Id',
  `signal_biz_code`      VARCHAR(128)          NOT NULL COMMENT '信号业务code',
  `signal_time`          TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '信号通知时间',
  `db_create_time`       TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time`       TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_workflowinstanceid_signalcode` (`workflow_instance_id`, `signal_biz_code`)
);

CREATE TABLE IF NOT EXISTS `workflow_node_heartbeat`
(
  `id`             BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键',
  `namespace`      VARCHAR(128)          NOT NULL COMMENT '命名空间',
  `worker_node`    VARCHAR(128)          NOT NULL COMMENT 'worker的身份信息',
  `heartbeat_time` TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近的心跳时间',
  `db_create_time` TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time` TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `namespace_worker_node` (`namespace`, `worker_node`)
);

CREATE TABLE IF NOT EXISTS `workflow_node_leader`
(
  `id`             BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键',
  `namespace`      VARCHAR(128)          NOT NULL COMMENT '命名空间',
  `leader_node`    VARCHAR(128)          NOT NULL COMMENT 'leader的身份信息',
  `refresh_time`   TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'leader的最新更新时间',
  `db_create_time` TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time` TIMESTAMP             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `namespace` (`namespace`)
);