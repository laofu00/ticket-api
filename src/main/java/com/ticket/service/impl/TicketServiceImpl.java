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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 工单 Service 实现
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis 缓存 key 前缀（ID） */
    private static final String CACHE_KEY_ID = "ticket:id:";
    /** Redis 缓存 key 前缀（工单号） */
    private static final String CACHE_KEY_TICKET_ID = "ticket:ticketId:";
    /** 缓存过期时间（分钟） */
    private static final long CACHE_EXPIRE_MINUTES = 30;

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

        // 写入缓存
        redisTemplate.opsForValue().set(CACHE_KEY_ID + ticket.getId(), ticket, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(CACHE_KEY_TICKET_ID + ticket.getTicketId(), ticket, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);

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

        // 清除旧缓存
        clearTicketCache(existing);
        // 写入新缓存
        Ticket updated = ticketMapper.selectById(id);
        if (updated != null) {
            redisTemplate.opsForValue().set(CACHE_KEY_ID + id, updated, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(CACHE_KEY_TICKET_ID + updated.getTicketId(), updated, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
    }

    @Override
    public void deleteTicket(Integer id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在，id: " + id);
        }
        ticketMapper.deleteById(id);
        // 清除缓存
        clearTicketCache(ticket);
    }

    @Override
    public Ticket getTicketById(Integer id) {
        // 先查缓存
        String cacheKey = CACHE_KEY_ID + id;
        Ticket ticket = (Ticket) redisTemplate.opsForValue().get(cacheKey);
        if (ticket != null) {
            return ticket;
        }

        // 缓存未命中，查数据库
        ticket = ticketMapper.selectById(id);
        if (ticket != null) {
            redisTemplate.opsForValue().set(cacheKey, ticket, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        return ticket;
    }

    @Override
    public Ticket getTicketByTicketId(String ticketId) {
        // 先查缓存
        String cacheKey = CACHE_KEY_TICKET_ID + ticketId;
        Ticket ticket = (Ticket) redisTemplate.opsForValue().get(cacheKey);
        if (ticket != null) {
            return ticket;
        }

        // 缓存未命中，查数据库
        ticket = ticketMapper.selectOne(
                new LambdaQueryWrapper<Ticket>().eq(Ticket::getTicketId, ticketId));
        if (ticket != null) {
            redisTemplate.opsForValue().set(cacheKey, ticket, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        return ticket;
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

    /**
     * 清除工单相关的所有缓存
     */
    private void clearTicketCache(Ticket ticket) {
        redisTemplate.delete(CACHE_KEY_ID + ticket.getId());
        redisTemplate.delete(CACHE_KEY_TICKET_ID + ticket.getTicketId());
    }
}
