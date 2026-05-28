package com.ticket.dto.request;

import lombok.Data;

/**
 * 管理后台工单更新请求 DTO（仅更新状态和优先级）
 */
@Data
public class AdminTicketUpdateRequest {

    /** 状态：pending / processing / completed / cancelled */
    private String status;

    /** 优先级：high / medium / low */
    private String priority;
}
