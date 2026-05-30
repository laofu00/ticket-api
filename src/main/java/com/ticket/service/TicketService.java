package com.ticket.service;

import com.ticket.common.PageResult;
import com.ticket.dto.request.TicketCreateRequest;
import com.ticket.dto.request.TicketQueryRequest;
import com.ticket.dto.request.TicketUpdateRequest;
import com.ticket.entity.Ticket;

/**
 * 工单 Service 接口
 */
public interface TicketService {

    /**
     * 创建工单
     */
    Ticket createTicket(TicketCreateRequest request);

    /**
     * 更新工单（全量）
     */
    void updateTicket(Integer id, TicketUpdateRequest request);

    /**
     * 管理后台更新工单（仅状态和优先级）
     */
    void adminUpdateTicket(Integer id, String status, String priority);

    /**
     * 删除工单
     */
    void deleteTicket(Integer id);

    /**
     * 根据 ID 查询工单
     */
    Ticket getTicketById(Integer id);

    /**
     * 根据工单号查询工单
     */
    Ticket getTicketByTicketId(String ticketId);

    /**
     * 分页条件查询工单
     */
    PageResult<Ticket> queryTickets(TicketQueryRequest request);
}
