package com.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单响应 DTO
 */
@Data
public class TicketResponse {

    private Integer id;
    private String ticketId;
    private String status;
    private String priority;
    private String faultDescription;
    private String location;
    private String reporter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
