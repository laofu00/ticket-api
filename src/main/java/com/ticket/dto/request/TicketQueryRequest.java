package com.ticket.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单分页条件查询请求 DTO
 */
@Data
public class TicketQueryRequest {

    /** 当前页码（默认第1页） */
    private int pageNum = 1;

    /** 每页条数（默认10条） */
    private int pageSize = 10;

    /** 状态 */
    private String status;

    /** 优先级 */
    private String priority;

    /** 提交人 */
    private String reporter;

    /** 工单号（模糊匹配） */
    private String ticketId;

    /** 创建时间范围-开始 */
    private LocalDateTime createTimeStart;

    /** 创建时间范围-结束 */
    private LocalDateTime createTimeEnd;
}
