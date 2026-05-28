package com.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.BusinessException;
import com.ticket.common.ErrorCode;
import com.ticket.common.PageResult;
import com.ticket.dto.request.TicketCreateRequest;
import com.ticket.dto.request.TicketQueryRequest;
import com.ticket.dto.request.TicketUpdateRequest;
import com.ticket.entity.Ticket;
import com.ticket.mapper.TicketMapper;
import com.ticket.service.impl.TicketServiceImpl;
import java.io.Serializable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceImpl 单元测试")
class TicketServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<Ticket>> wrapperCaptor;

    private TicketServiceImpl ticketService;

    private Ticket sampleTicket;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(ticketMapper, redisTemplate);

        sampleTicket = new Ticket();
        sampleTicket.setId(1);
        sampleTicket.setTicketId("IT-20260526-001");
        sampleTicket.setStatus("pending");
        sampleTicket.setPriority("medium");
        sampleTicket.setFaultDescription("电脑无法开机");
        sampleTicket.setLocation("A-101");
        sampleTicket.setReporter("张三");
        sampleTicket.setCreateTime(LocalDateTime.of(2026, 5, 26, 10, 0, 0));
        sampleTicket.setUpdateTime(LocalDateTime.of(2026, 5, 26, 10, 0, 0));

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("创建工单 createTicket")
    class CreateTicket {

        @Test
        @DisplayName("成功创建，自动生成工单号，默认状态/优先级，写入缓存")
        void createSuccess() {
            TicketCreateRequest request = new TicketCreateRequest();
            request.setFaultDescription("电脑无法开机");
            request.setLocation("A-101");
            request.setReporter("张三");

            when(ticketMapper.selectMaxTicketIdForToday("IT-20260526")).thenReturn(null);
            when(ticketMapper.insert(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket t = invocation.getArgument(0);
                t.setId(1);
                return 1;
            });

            Ticket result = ticketService.createTicket(request);

            assertNotNull(result);
            assertEquals("IT-20260526-001", result.getTicketId());
            assertEquals("pending", result.getStatus());
            assertEquals("medium", result.getPriority());
            assertEquals("张三", result.getReporter());

            verify(ticketMapper).selectMaxTicketIdForToday("IT-20260526");
            verify(ticketMapper).insert(ticketCaptor.capture());
            verify(valueOperations).set(eq("ticket:id:1"), any(), eq(30L), eq(TimeUnit.MINUTES));
            verify(valueOperations).set(eq("ticket:ticketId:IT-20260526-001"), any(), eq(30L), eq(TimeUnit.MINUTES));

            Ticket captured = ticketCaptor.getValue();
            assertEquals("IT-20260526-001", captured.getTicketId());
            assertEquals("pending", captured.getStatus());
            assertEquals("medium", captured.getPriority());
        }

        @Test
        @DisplayName("成功创建，使用指定状态和优先级，序号自增")
        void createSuccessWithCustomStatus() {
            TicketCreateRequest request = new TicketCreateRequest();
            request.setStatus("processing");
            request.setPriority("high");
            request.setFaultDescription("网络故障");
            request.setLocation("B-202");
            request.setReporter("李四");

            when(ticketMapper.selectMaxTicketIdForToday("IT-20260526")).thenReturn("IT-20260526-001");
            when(ticketMapper.insert(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket t = invocation.getArgument(0);
                t.setId(2);
                return 1;
            });

            Ticket result = ticketService.createTicket(request);

            assertEquals("IT-20260526-002", result.getTicketId());
            assertEquals("processing", result.getStatus());
            assertEquals("high", result.getPriority());

            verify(ticketMapper).insert(ticketCaptor.capture());
            Ticket captured = ticketCaptor.getValue();
            assertEquals("IT-20260526-002", captured.getTicketId());
            assertEquals("processing", captured.getStatus());
            assertEquals("high", captured.getPriority());
        }
    }

    @Nested
    @DisplayName("更新工单 updateTicket")
    class UpdateTicket {

        @Test
        @DisplayName("成功更新，清除旧缓存，写入新缓存")
        void updateSuccess() {
            TicketUpdateRequest request = new TicketUpdateRequest();
            request.setTicketId("IT-20260526-001");
            request.setStatus("processing");
            request.setPriority("high");
            request.setFaultDescription("电脑无法开机-已处理");
            request.setLocation("A-101");
            request.setReporter("张三");

            when(ticketMapper.selectById(1)).thenReturn(sampleTicket);
            when(ticketMapper.updateById(any(Ticket.class))).thenReturn(1);
            when(ticketMapper.selectById(1)).thenReturn(sampleTicket);

            ticketService.updateTicket(1, request);

            verify(ticketMapper).updateById(ticketCaptor.capture());
            Ticket captured = ticketCaptor.getValue();
            assertEquals("processing", captured.getStatus());
            assertEquals("high", captured.getPriority());

            verify(redisTemplate).delete("ticket:id:1");
            verify(redisTemplate).delete("ticket:ticketId:IT-20260526-001");
            verify(valueOperations, atLeast(1)).set(anyString(), any(), eq(30L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("更新不存在的工单时抛出 BusinessException")
        void updateNotFound() {
            when(ticketMapper.selectById(999)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> ticketService.updateTicket(999, new TicketUpdateRequest()));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("更新工单号时检查冲突")
        void updateWithConflictingTicketId() {
            TicketUpdateRequest request = new TicketUpdateRequest();
            request.setTicketId("IT-20260526-002");
            request.setStatus("processing");
            request.setPriority("high");
            request.setFaultDescription("测试");
            request.setLocation("A-101");
            request.setReporter("张三");

            when(ticketMapper.selectById(1)).thenReturn(sampleTicket);
            when(ticketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> ticketService.updateTicket(1, request));
            assertEquals(ErrorCode.DUPLICATE_TICKET_ID, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("删除工单 deleteTicket")
    class DeleteTicket {

        @Test
        @DisplayName("成功删除，清除缓存")
        void deleteSuccess() {
            when(ticketMapper.selectById(1)).thenReturn(sampleTicket);

            ticketService.deleteTicket(1);

            verify(ticketMapper).deleteById(Integer.valueOf(1));
            verify(redisTemplate).delete("ticket:id:1");
            verify(redisTemplate).delete("ticket:ticketId:IT-20260526-001");
        }

        @Test
        @DisplayName("删除不存在的工单时抛出 BusinessException")
        void deleteNotFound() {
            when(ticketMapper.selectById(999)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> ticketService.deleteTicket(999));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());

            verify(ticketMapper, never()).deleteById(any(Serializable.class));
        }
    }

    @Nested
    @DisplayName("按 ID 查询 getTicketById")
    class GetTicketById {

        @Test
        @DisplayName("缓存命中，直接返回缓存数据")
        void cacheHit() {
            when(valueOperations.get("ticket:id:1")).thenReturn(sampleTicket);

            Ticket result = ticketService.getTicketById(1);

            assertNotNull(result);
            assertEquals("IT-20260526-001", result.getTicketId());
            verify(ticketMapper, never()).selectById(any());
        }

        @Test
        @DisplayName("缓存未命中，查数据库并写入缓存")
        void cacheMiss() {
            when(valueOperations.get("ticket:id:1")).thenReturn(null);
            when(ticketMapper.selectById(1)).thenReturn(sampleTicket);

            Ticket result = ticketService.getTicketById(1);

            assertNotNull(result);
            assertEquals("IT-20260526-001", result.getTicketId());
            verify(ticketMapper).selectById(1);
            verify(valueOperations).set(eq("ticket:id:1"), eq(sampleTicket), eq(30L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("数据库也没有数据，返回 null")
        void notFound() {
            when(valueOperations.get("ticket:id:999")).thenReturn(null);
            when(ticketMapper.selectById(999)).thenReturn(null);

            Ticket result = ticketService.getTicketById(999);

            assertNull(result);
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("按工单号查询 getTicketByTicketId")
    class GetTicketByTicketId {

        @Test
        @DisplayName("缓存命中，直接返回缓存数据")
        void cacheHit() {
            when(valueOperations.get("ticket:ticketId:IT-20260526-001")).thenReturn(sampleTicket);

            Ticket result = ticketService.getTicketByTicketId("IT-20260526-001");

            assertNotNull(result);
            assertEquals(1, result.getId());
            verify(ticketMapper, never()).selectOne(any());
        }

        @Test
        @DisplayName("缓存未命中，查数据库并写入缓存")
        void cacheMiss() {
            when(valueOperations.get("ticket:ticketId:IT-20260526-001")).thenReturn(null);
            when(ticketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(sampleTicket);

            Ticket result = ticketService.getTicketByTicketId("IT-20260526-001");

            assertNotNull(result);
            assertEquals("IT-20260526-001", result.getTicketId());
            verify(ticketMapper).selectOne(any(LambdaQueryWrapper.class));
            verify(valueOperations).set(eq("ticket:ticketId:IT-20260526-001"), eq(sampleTicket), eq(30L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("工单不存在，返回 null")
        void notFound() {
            when(valueOperations.get("ticket:ticketId:NOT_EXIST")).thenReturn(null);
            when(ticketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            Ticket result = ticketService.getTicketByTicketId("NOT_EXIST");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("分页条件查询 queryTickets")
    class QueryTickets {

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("无条件分页查询")
        void queryAll() {
            Ticket ticket2 = new Ticket();
            ticket2.setId(2);
            ticket2.setTicketId("IT-20260526-002");

            Page<Ticket> page = new Page<>(1, 10);
            page.setRecords(List.of(sampleTicket, ticket2));
            page.setTotal(2);

            when(ticketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            TicketQueryRequest request = new TicketQueryRequest();
            request.setPageNum(1);
            request.setPageSize(10);

            PageResult<Ticket> result = ticketService.queryTickets(request);

            assertEquals(2, result.getTotal());
            assertEquals(1, result.getPageNum());
            assertEquals(10, result.getPageSize());
            assertEquals(2, result.getRecords().size());
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("带条件过滤的分页查询")
        void queryWithFilters() {
            Page<Ticket> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0);

            when(ticketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            TicketQueryRequest request = new TicketQueryRequest();
            request.setPageNum(1);
            request.setPageSize(10);
            request.setStatus("completed");
            request.setPriority("high");
            request.setReporter("张三");
            request.setTicketId("TK");
            request.setCreateTimeStart(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
            request.setCreateTimeEnd(LocalDateTime.of(2026, 12, 31, 23, 59, 59));

            PageResult<Ticket> result = ticketService.queryTickets(request);

            assertEquals(0, result.getTotal());
            verify(ticketMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("分页查询使用正确页码和大小")
        void queryWithCustomPagination() {
            Page<Ticket> page = new Page<>(2, 5);
            page.setRecords(List.of());
            page.setTotal(0);

            when(ticketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            TicketQueryRequest request = new TicketQueryRequest();
            request.setPageNum(2);
            request.setPageSize(5);

            ticketService.queryTickets(request);

            ArgumentCaptor<Page<Ticket>> pageCaptor = ArgumentCaptor.forClass(Page.class);
            verify(ticketMapper).selectPage(pageCaptor.capture(), any());
            assertEquals(2, pageCaptor.getValue().getCurrent());
            assertEquals(5, pageCaptor.getValue().getSize());
        }
    }
}
