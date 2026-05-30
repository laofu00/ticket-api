package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.BusinessException;
import com.ticket.common.ErrorCode;
import com.ticket.common.PageResult;
import com.ticket.common.TicketIdUtil;
import com.ticket.dto.request.TicketCreateRequest;
import com.ticket.dto.request.TicketQueryRequest;
import com.ticket.dto.request.TicketUpdateRequest;
import com.ticket.entity.Ticket;
import com.ticket.mapper.TicketMapper;
import com.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 工单 Service 实现
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;

    @Override
    public Ticket createTicket(TicketCreateRequest request) {
        // 查询当天最大工单号，自动生成新工单号
        String datePrefix = "IT-" + TicketIdUtil.getTodayDatePart();
        String maxTicketId = ticketMapper.selectMaxTicketIdForToday(datePrefix);
        String ticketId = TicketIdUtil.generateTicketId(maxTicketId);

        // 创建实体并设置默认值
        Ticket ticket = new Ticket();
        ticket.setTicketId(ticketId);
        ticket.setFaultDescription(request.getFaultDescription());
        ticket.setLocation(request.getLocation());
        ticket.setReporter(request.getReporter());
        ticket.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "pending");
        ticket.setPriority(StringUtils.hasText(request.getPriority()) ? request.getPriority() : "medium");

        ticketMapper.insert(ticket);

        return ticket;
    }

    @Override
    public void updateTicket(Integer id, TicketUpdateRequest request) {
        // 检查工单是否存在
        Ticket existing = ticketMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在，id: " + id);
        }

        // 如果修改了工单号，检查新工单号是否与其他记录冲突
        if (!existing.getTicketId().equals(request.getTicketId())) {
            Long count = ticketMapper.selectCount(
                    new LambdaQueryWrapper<Ticket>()
                            .eq(Ticket::getTicketId, request.getTicketId())
                            .ne(Ticket::getId, id));
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.DUPLICATE_TICKET_ID, "工单号 " + request.getTicketId() + " 已存在");
            }
        }

        // 更新实体
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTicketId(request.getTicketId());
        ticket.setStatus(request.getStatus());
        ticket.setPriority(request.getPriority());
        ticket.setFaultDescription(request.getFaultDescription());
        ticket.setLocation(request.getLocation());
        ticket.setReporter(request.getReporter());

        ticketMapper.updateById(ticket);
    }

    @Override
    public void adminUpdateTicket(Integer id, String status, String priority) {
        // 检查工单是否存在
        Ticket existing = ticketMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在，id: " + id);
        }

        // 更新实体（只设置需要修改的字段）
        Ticket updateEntity = new Ticket();
        updateEntity.setId(id);
        if (StringUtils.hasText(status)) {
            updateEntity.setStatus(status);
        }
        if (StringUtils.hasText(priority)) {
            updateEntity.setPriority(priority);
        }

        ticketMapper.updateById(updateEntity);
    }

    @Override
    public void deleteTicket(Integer id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在，id: " + id);
        }
        ticketMapper.deleteById(id);
    }

    @Override
    public Ticket getTicketById(Integer id) {
        return ticketMapper.selectById(id);
    }

    @Override
    public Ticket getTicketByTicketId(String ticketId) {
        return ticketMapper.selectOne(
                new LambdaQueryWrapper<Ticket>().eq(Ticket::getTicketId, ticketId));
    }

    @Override
    public PageResult<Ticket> queryTickets(TicketQueryRequest request) {
        // 构建分页条件
        Page<Ticket> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<Ticket>()
                .eq(StringUtils.hasText(request.getStatus()), Ticket::getStatus, request.getStatus())
                .eq(StringUtils.hasText(request.getPriority()), Ticket::getPriority, request.getPriority())
                .eq(StringUtils.hasText(request.getReporter()), Ticket::getReporter, request.getReporter())
                .like(StringUtils.hasText(request.getTicketId()), Ticket::getTicketId, request.getTicketId())
                .ge(request.getCreateTimeStart() != null, Ticket::getCreateTime, request.getCreateTimeStart())
                .le(request.getCreateTimeEnd() != null, Ticket::getCreateTime, request.getCreateTimeEnd())
                .orderByDesc(Ticket::getCreateTime);

        IPage<Ticket> result = ticketMapper.selectPage(page, wrapper);

        // 封装分页结果
        PageResult<Ticket> pageResult = new PageResult<>();
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum(result.getCurrent());
        pageResult.setPageSize(result.getSize());
        pageResult.setRecords(result.getRecords());

        return pageResult;
    }
}
