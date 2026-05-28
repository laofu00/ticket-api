package com.ticket.dto.response;

import lombok.Data;

import java.util.Map;

/**
 * 管理后台仪表盘统计响应 DTO
 */
@Data
public class DashboardResponse {

    /** 工单总数（全部） */
    private long totalCount;

    /** 今日新增工单数 */
    private long todayCount;

    /** 统计时间区间内的工单总数 */
    private long periodTotal;

    /** 按状态分布：pending / processing / completed / cancelled */
    private Map<String, Long> statusDistribution;

    /** 按优先级分布：high / medium / low */
    private Map<String, Long> priorityDistribution;
}
