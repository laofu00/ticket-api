package com.ticket.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.R;
import com.ticket.dto.request.AdminTicketUpdateRequest;
import com.ticket.dto.response.DashboardResponse;
import com.ticket.entity.Ticket;
import com.ticket.mapper.TicketMapper;
import com.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台 Controller
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TicketMapper ticketMapper;
    private final TicketService ticketService;

    @Value("${dify.webapp-url}")
    private String difyWebappUrl;

    /**
     * 获取管理后台配置（如 Dify 工单助手 URL）
     */
    @GetMapping("/config")
    public R<Map<String, String>> getConfig() {
        return R.ok(Map.of("difyWebappUrl", difyWebappUrl));
    }

    /**
     * 管理后台仪表盘统计
     *
     * @param startDate 统计区间开始日期（可选，默认当天）
     * @param endDate   统计区间结束日期（可选，默认当天）
     */
    @GetMapping("/dashboard")
    public R<DashboardResponse> dashboard(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        // 默认统计区间为当天
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime periodStart = startDate.atStartOfDay();
        LocalDateTime periodEnd = endDate.atTime(LocalTime.MAX);

        // 今日起始时间
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        // 1. 工单总数（全部）
        long totalCount = ticketMapper.selectCount(null);

        // 2. 今日新增工单数
        long todayCount = ticketMapper.selectCount(
                new LambdaQueryWrapper<Ticket>()
                        .ge(Ticket::getCreateTime, todayStart)
                        .le(Ticket::getCreateTime, todayEnd));

        // 3. 统计区间内的工单
        List<Ticket> periodTickets = ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>()
                        .ge(Ticket::getCreateTime, periodStart)
                        .le(Ticket::getCreateTime, periodEnd));

        long periodTotal = periodTickets.size();

        // 4. 按状态分布
        Map<String, Long> statusDistribution = periodTickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus() != null ? t.getStatus() : "unknown",
                        LinkedHashMap::new,
                        Collectors.counting()));

        // 5. 按优先级分布
        Map<String, Long> priorityDistribution = periodTickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPriority() != null ? t.getPriority() : "unknown",
                        LinkedHashMap::new,
                        Collectors.counting()));

        DashboardResponse response = new DashboardResponse();
        response.setTotalCount(totalCount);
        response.setTodayCount(todayCount);
        response.setPeriodTotal(periodTotal);
        response.setStatusDistribution(statusDistribution);
        response.setPriorityDistribution(priorityDistribution);

        return R.ok(response);
    }

    /**
     * 管理后台更新工单状态和优先级
     */
    @PatchMapping("/tickets/{id}")
    public R<Void> updateTicket(@PathVariable Integer id,
                                @RequestBody AdminTicketUpdateRequest request) {
        ticketService.adminUpdateTicket(id, request.getStatus(), request.getPriority());
        return R.ok();
    }
}
