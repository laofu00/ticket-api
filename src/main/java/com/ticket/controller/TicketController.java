package com.ticket.controller;

import com.ticket.common.PageResult;
import com.ticket.common.R;
import com.ticket.dto.request.TicketCreateRequest;
import com.ticket.dto.request.TicketQueryRequest;
import com.ticket.dto.request.TicketUpdateRequest;
import com.ticket.dto.response.TicketResponse;
import com.ticket.entity.Ticket;
import com.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 工单 Controller
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 创建工单
     */
    @PostMapping
    public R<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        Ticket ticket = ticketService.createTicket(request);
        return R.ok(convertToResponse(ticket));
    }

    /**
     * 更新工单（全量）
     */
    @PutMapping("/{id}")
    public R<Void> updateTicket(@PathVariable Integer id, @Valid @RequestBody TicketUpdateRequest request) {
        ticketService.updateTicket(id, request);
        return R.ok();
    }

    /**
     * 删除工单
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteTicket(@PathVariable Integer id) {
        ticketService.deleteTicket(id);
        return R.ok();
    }

    /**
     * 根据主键 id 查询工单
     */
    @GetMapping("/{id}")
    public R<TicketResponse> getTicketById(@PathVariable Integer id) {
        Ticket ticket = ticketService.getTicketById(id);
        if (ticket == null) {
            return R.error(404, "工单不存在");
        }
        return R.ok(convertToResponse(ticket));
    }

    /**
     * 根据工单号查询工单
     */
    @GetMapping("/ticketId/{ticketId}")
    public R<TicketResponse> getTicketByTicketId(@PathVariable String ticketId) {
        Ticket ticket = ticketService.getTicketByTicketId(ticketId);
        if (ticket == null) {
            return R.error(404, "工单不存在");
        }
        return R.ok(convertToResponse(ticket));
    }

    /**
     * 分页条件查询工单
     */
    @GetMapping
    public R<PageResult<TicketResponse>> queryTickets(@Valid TicketQueryRequest request) {
        PageResult<Ticket> pageResult = ticketService.queryTickets(request);

        // 转换实体列表为响应 DTO 列表
        PageResult<TicketResponse> responseResult = new PageResult<>();
        BeanUtils.copyProperties(pageResult, responseResult);
        responseResult.setRecords(pageResult.getRecords().stream()
                .map(this::convertToResponse)
                .toList());

        return R.ok(responseResult);
    }

    /**
     * 将 Ticket 实体转换为 TicketResponse DTO
     */
    private TicketResponse convertToResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        BeanUtils.copyProperties(ticket, response);
        return response;
    }
}
