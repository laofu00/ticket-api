package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新工单请求 DTO
 */
@Data
public class TicketUpdateRequest {

    @NotBlank(message = "工单号不能为空")
    @Size(max = 20, message = "工单号长度不能超过20")
    private String ticketId;

    @NotBlank(message = "状态不能为空")
    @Size(max = 20, message = "状态长度不能超过20")
    private String status;

    @NotBlank(message = "优先级不能为空")
    @Size(max = 10, message = "优先级长度不能超过10")
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
