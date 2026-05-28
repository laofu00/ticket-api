package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工单请求 DTO
 * <p>
 * 注意：ticketId 由后端自动生成，无需客户端传入。
 * 生成规则: IT-YYYYMMDD-XXX（如 IT-20260526-001）
 */
@Data
public class TicketCreateRequest {

    /** 状态，默认 pending */
    private String status;

    /** 优先级，默认 medium */
    private String priority;

    @NotBlank(message = "故障描述不能为空")
    @Size(max = 255, message = "故障描述长度不能超过255")
    private String faultDescription;

    @NotBlank(message = "工位位置不能为空")
    @Size(max = 100, message = "工位位置长度不能超过100")
    private String location;

    // @NotBlank(message = "提交人不能为空")
    @Size(max = 20, message = "提交人长度不能超过20")
    private String reporter;
}
