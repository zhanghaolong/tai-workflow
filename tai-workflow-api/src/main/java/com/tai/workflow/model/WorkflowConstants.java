package com.tai.workflow.model;

/**
 * @author zhanghaolong1989@163.com
 */
public final class WorkflowConstants {
    public static final String STATUS = "STATUS";
    public static final String WORKFLOW_INSTANCE_ID = "WORKFLOW_INSTANCE_ID";
    public static final String ACTIVITY_INSTANCE_ID = "ACTIVITY_INSTANCE_ID";
    public static final String ACTIVITY_INSTANCE_NAME = "ACTIVITY_INSTANCE_NAME";
    public static final String PARENT_WORKFLOW_INSTANCE_ID = "PARENT_WORKFLOW_INSTANCE_ID";
    public static final String PARENT_WORKFLOW_INSTANCE_TOKEN = "PARENT_WORKFLOW_INSTANCE_TOKEN";
    public static final String PARENT_ACTIVITY_INSTANCE_ID = "PARENT_ACTIVITY_INSTANCE_ID";
    public static final String PARENT_ACTIVITY_INSTANCE_NAME = "PARENT_ACTIVITY_INSTANCE_NAME";

    public static final String MERGE_TO_PARENT_CONTEXT_KEYS = "MERGE_TO_PARENT_CONTEXT_KEYS";

    public static final String DASH = "-";

    public static final String DELIMITER = "#";

    public static final String CLUSTER_ID = "clusterId";

    public static final String INVALID_PARAMETER = "INVALID_PARAMETER";

    public static final String ROLLBACK = "ROLLBACK:%s";

    public static final String ROLLBACK_FAILED = "ROLLBACK_FAILED:%s";

    public static final String DUPLICATE_KEY = "Duplicate entry";

    public static final String UNIQUE_KEY = "Unique index or primary key violation";

    public static final Integer SECONDS_PER_MINUTE = 60;

    public static final Long MILLIS_PER_SECOND = 1_000L;
}
