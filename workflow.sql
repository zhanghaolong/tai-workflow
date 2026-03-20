CREATE TABLE IF NOT EXISTS `workflow_definition`
(
  `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace`    varchar(128)        NOT NULL COMMENT '命名空间',
  `name`         varchar(128)        NOT NULL COMMENT '流程的名称',
  `display_name` varchar(512) COMMENT '显示名称',
  `description`  varchar(1024) COMMENT '流程描述',
  `worker_node`  varchar(128) DEFAULT NULL COMMENT '工作流被定义的节点身份 id',
  `variables`    mediumtext          NOT NULL COMMENT '定义的初始化变量',
  `dag`          mediumtext          NOT NULL COMMENT '存储流程的DAG图',
  `db_create_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `uk_namespace_name_worker_node` (`namespace`, `name`, `worker_node`)
);

CREATE TABLE IF NOT EXISTS `workflow_instance`
(
  `id`                     bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `token`                  varchar(256)                 DEFAULT NULL COMMENT '标识工作流实例Token',
  `namespace`              varchar(128)        NOT NULL COMMENT '命名空间',
  `name`                   varchar(128)        NOT NULL COMMENT '实例的名称',
  `display_name`           varchar(128)        NOT NULL COMMENT '显示名称',
  `workflow_definition_id` bigint(20) unsigned NOT NULL COMMENT '定义的ID',
  `state`                  varchar(32)         NOT NULL COMMENT '实例的状态',
  `start_time`             datetime            NULL     DEFAULT NULL COMMENT '启动时间',
  `end_time`               datetime            NULL     DEFAULT NULL COMMENT '结束时间',
  `parent_id`              bigint(20) unsigned          DEFAULT NULL COMMENT '父工作流实例Id',
  `parent_activity_id`     bigint(20) unsigned          DEFAULT NULL COMMENT '父工作流活动实例Id',
  `context`                mediumtext COMMENT '流程实例context',
  `definition_variables`   mediumtext COMMENT '定义的变量',
  `worker_node`            varchar(128)                 DEFAULT NULL COMMENT '工作流运行在的节点身份Id',
  `version`                int(11) unsigned    NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `biz_id`                 varchar(128)                 DEFAULT NULL COMMENT '业务id',
  `db_create_time`         datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time`         datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_namespace_token` (`namespace`, `token`),
  KEY `idx_workflow_definition` (`workflow_definition_id`),
  KEY `idx_parent` (`parent_id`, `parent_activity_id`),
  KEY `idx_biz` (`biz_id`),
  KEY `idx_namespace_start_worker_node` (`namespace`, `start_time`, `worker_node`)
);

CREATE TABLE IF NOT EXISTS `activity_instance`
(
  `id`                   bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace`            varchar(128)        NOT NULL COMMENT '命名空间',
  `name`                 varchar(128)        NOT NULL COMMENT 'Activity 实例名称',
  `display_name`         varchar(256)                 DEFAULT NULL COMMENT 'Activity 实例展示名称',
  `workflow_instance_id` bigint(20) unsigned NOT NULL COMMENT '流程实例ID',
  `state`                varchar(32)         NOT NULL COMMENT 'Activity 实例状态',
  `prev_state`           varchar(32)         NULL COMMENT 'Activity 实例前一状态',
  `execution_msg`        mediumtext COMMENT '执行日志，主要以异常日志为主',
  `prev_execution_msg`   mediumtext COMMENT '前一执行日志，主要以异常日志为主',
  `first_start_time`     datetime            NULL     DEFAULT NULL COMMENT '第一次开始时间',
  `start_time`           datetime            NULL     DEFAULT NULL COMMENT '开始时间',
  `end_time`             datetime            NULL     DEFAULT NULL COMMENT '结束时间',
  `timeout_time`         datetime            NULL     DEFAULT NULL COMMENT '超时时间',
  `retry_count`          int(11)             NOT NULL DEFAULT '0' COMMENT '已经重试次数',
  `input_context`        mediumtext COMMENT '输入的Context',
  `output_context`       mediumtext COMMENT '输出的Context',
  `signal_biz_code`      varchar(128)                 DEFAULT NULL COMMENT '信号code',
  `db_create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflowinstanceid_name` (`workflow_instance_id`, `name`),
  KEY `idx_workflowinstanceid_state` (`workflow_instance_id`, `state`),
  KEY `idx_signal_biz_code` (`signal_biz_code`),
  KEY `idx_namespace_timeout` (`namespace`, `timeout_time`)
);

CREATE TABLE IF NOT EXISTS `workflow_signal_record`
(
  `id`                   bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_instance_id` bigint(20) unsigned NOT NULL COMMENT '工作流实例Id',
  `signal_biz_code`      varchar(128)        NOT NULL COMMENT '信号业务code',
  `signal_time`          datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '信号通知时间',
  `db_create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `db_modify_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflowinstanceid_signalcode` (`workflow_instance_id`, `signal_biz_code`)
);

CREATE TABLE IF NOT EXISTS `workflow_node_heartbeat`
(
  `id`             bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace`      varchar(128)        NOT NULL COMMENT '命名空间',
  `worker_node`    varchar(128)        NOT NULL COMMENT 'worker的身份信息',
  `heartbeat_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近的心跳时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `namespace_worker_node` (`namespace`, `worker_node`)
);

CREATE TABLE IF NOT EXISTS `workflow_node_leader`
(
  `id`           bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `namespace`    varchar(128)        NOT NULL COMMENT '命名空间',
  `leader_node`  varchar(128)        NOT NULL COMMENT 'leader的身份信息',
  `refresh_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'leader的最新更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `namespace` (`namespace`)
);