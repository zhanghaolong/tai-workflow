package com.tai.workflow.repository.param;

import com.tai.workflow.enums.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity实例查询参数
 *
 * @author zhanghaolong1989@163.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListActivityInstancesParam {

    /**
     * 操作ID
     */
    private Long operationId;

    /**
     * 操作名称（模糊搜索）
     */
    private String operationName;

    /**
     * 操作日期筛选开始时间
     */
    private Long createTimeBefore;

    /**
     * 操作日期筛选结束时间
     */
    private Long createTimeAfter;

    /**
     * 操作记录状态.
     */
    private List<String> activityStates = new ArrayList<>();

    /**
     * 工作流实例ID
     */
    private List<Long> workflowInstanceIds;

    private Integer maxResults = 10;
    private Integer nextToken = 0;

    /**
     * 开始时间排序
     */
    private Order startTimeOrder;

    /**
     * 结束时间排序
     */
    private Order endTimeOrder;
}
