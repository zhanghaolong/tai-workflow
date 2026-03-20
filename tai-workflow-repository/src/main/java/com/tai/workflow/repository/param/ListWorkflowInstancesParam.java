package com.tai.workflow.repository.param;

import com.tai.workflow.enums.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 参数类用于查询工作流实例列表
 *
 * @author claude-code
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListWorkflowInstancesParam {

    /**
     * 分页大小
     */
    private Integer maxResults;

    /**
     * 分页偏移量
     */
    private Integer nextToken;

    /**
     * ID排序方向（默认降序）
     */
    @Builder.Default
    private Order idOrder = Order.DESC;
}
