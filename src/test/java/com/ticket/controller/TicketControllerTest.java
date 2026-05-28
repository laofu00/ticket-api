package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.BusinessException;
import com.ticket.common.ErrorCode;
import com.ticket.common.PageResult;
import com.ticket.dto.request.TicketCreateRequest;
import com.ticket.dto.request.TicketQueryRequest;
import com.ticket.dto.request.TicketUpdateRequest;
import com.ticket.dto.response.TicketResponse;
import com.ticket.entity.Ticket;
import com.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@DisplayName("TicketController 单元测试")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    private Ticket sampleTicket;
    private TicketCreateRequest createRequest;
    private TicketUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
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

        createRequest = new TicketCreateRequest();
        createRequest.setFaultDescription("电脑无法开机");
        createRequest.setLocation("A-101");
        createRequest.setReporter("张三");

        updateRequest = new TicketUpdateRequest();
        updateRequest.setTicketId("IT-20260526-001");
        updateRequest.setStatus("processing");
        updateRequest.setPriority("high");
        updateRequest.setFaultDescription("电脑无法开机-已维修");
        updateRequest.setLocation("A-101");
        updateRequest.setReporter("张三");
    }

    @Nested
    @DisplayName("POST /api/tickets - 创建工单")
    class CreateTicket {

        @Test
        @DisplayName("创建成功 → 200")
        void createSuccess() throws Exception {
            when(ticketService.createTicket(any(TicketCreateRequest.class))).thenReturn(sampleTicket);

            mockMvc.perform(post("/api/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)))
                    .andExpect(jsonPath("$.message", is("success")))
                    .andExpect(jsonPath("$.data.ticketId", is("IT-20260526-001")))
                    .andExpect(jsonPath("$.data.status", is("pending")))
                    .andExpect(jsonPath("$.data.priority", is("medium")))
                    .andExpect(jsonPath("$.data.faultDescription", is("电脑无法开机")))
                    .andExpect(jsonPath("$.data.location", is("A-101")))
                    .andExpect(jsonPath("$.data.reporter", is("张三")));
        }

        @Test
        @DisplayName("参数校验失败：faultDescription 为空 → 400")
        void createValidationFail() throws Exception {
            createRequest.setFaultDescription("");

            mockMvc.perform(post("/api/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is(400)));
        }
    }

    @Nested
    @DisplayName("PUT /api/tickets/{id} - 更新工单")
    class UpdateTicket {

        @Test
        @DisplayName("更新成功 → 200")
        void updateSuccess() throws Exception {
            doNothing().when(ticketService).updateTicket(eq(1), any(TicketUpdateRequest.class));

            mockMvc.perform(put("/api/tickets/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)))
                    .andExpect(jsonPath("$.message", is("success")));
        }

        @Test
        @DisplayName("工单不存在 → BusinessException(NOT_FOUND)")
        void updateNotFound() throws Exception {
            doThrow(new BusinessException(ErrorCode.NOT_FOUND, "工单不存在，id: 999"))
                    .when(ticketService).updateTicket(eq(999), any(TicketUpdateRequest.class));

            mockMvc.perform(put("/api/tickets/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is(404)))
                    .andExpect(jsonPath("$.message", containsString("不存在")));
        }

        @Test
        @DisplayName("参数校验失败：status 为空 → 400")
        void updateValidationFail() throws Exception {
            updateRequest.setStatus("");

            mockMvc.perform(put("/api/tickets/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is(400)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/tickets/{id} - 删除工单")
    class DeleteTicket {

        @Test
        @DisplayName("删除成功 → 200")
        void deleteSuccess() throws Exception {
            doNothing().when(ticketService).deleteTicket(1);

            mockMvc.perform(delete("/api/tickets/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)));
        }

        @Test
        @DisplayName("工单不存在 → BusinessException(NOT_FOUND)")
        void deleteNotFound() throws Exception {
            doThrow(new BusinessException(ErrorCode.NOT_FOUND, "工单不存在，id: 999"))
                    .when(ticketService).deleteTicket(999);

            mockMvc.perform(delete("/api/tickets/999"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is(404)))
                    .andExpect(jsonPath("$.message", containsString("不存在")));
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/{id} - 按 ID 查询工单")
    class GetTicketById {

        @Test
        @DisplayName("查询成功 → 200")
        void getSuccess() throws Exception {
            when(ticketService.getTicketById(1)).thenReturn(sampleTicket);

            mockMvc.perform(get("/api/tickets/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)))
                    .andExpect(jsonPath("$.data.ticketId", is("IT-20260526-001")))
                    .andExpect(jsonPath("$.data.status", is("pending")));
        }

        @Test
        @DisplayName("工单不存在 → 404")
        void getNotFound() throws Exception {
            when(ticketService.getTicketById(999)).thenReturn(null);

            mockMvc.perform(get("/api/tickets/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(404)))
                    .andExpect(jsonPath("$.message", is("工单不存在")));
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/ticketId/{ticketId} - 按工单号查询")
    class GetTicketByTicketId {

        @Test
        @DisplayName("查询成功 → 200")
        void getSuccess() throws Exception {
            when(ticketService.getTicketByTicketId("IT-20260526-001")).thenReturn(sampleTicket);

            mockMvc.perform(get("/api/tickets/ticketId/IT-20260526-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)))
                    .andExpect(jsonPath("$.data.ticketId", is("IT-20260526-001")));
        }

        @Test
        @DisplayName("工单不存在 → 404")
        void getNotFound() throws Exception {
            when(ticketService.getTicketByTicketId("NOT_EXIST")).thenReturn(null);

            mockMvc.perform(get("/api/tickets/ticketId/NOT_EXIST"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(404)))
                    .andExpect(jsonPath("$.message", is("工单不存在")));
        }
    }

    @Nested
    @DisplayName("GET /api/tickets - 分页条件查询")
    class QueryTickets {

        @Test
        @DisplayName("查询成功，返回分页数据 → 200")
        void querySuccess() throws Exception {
            Ticket ticket2 = new Ticket();
            BeanUtils.copyProperties(sampleTicket, ticket2);
            ticket2.setId(2);
            ticket2.setTicketId("IT-20260526-002");
            ticket2.setReporter("李四");

            PageResult<Ticket> pageResult = new PageResult<>();
            pageResult.setTotal(2);
            pageResult.setPageNum(1);
            pageResult.setPageSize(10);
            pageResult.setRecords(List.of(sampleTicket, ticket2));

            when(ticketService.queryTickets(any(TicketQueryRequest.class))).thenReturn(pageResult);

            mockMvc.perform(get("/api/tickets")
                            .param("pageNum", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)))
                    .andExpect(jsonPath("$.data.total", is(2)))
                    .andExpect(jsonPath("$.data.pageNum", is(1)))
                    .andExpect(jsonPath("$.data.pageSize", is(10)))
                    .andExpect(jsonPath("$.data.records", hasSize(2)))
                    .andExpect(jsonPath("$.data.records[0].ticketId", is("IT-20260526-001")))
                    .andExpect(jsonPath("$.data.records[1].ticketId", is("IT-20260526-002")));
        }

        @Test
        @DisplayName("查询成功，支持条件过滤 → 200")
        void queryWithFilters() throws Exception {
            PageResult<Ticket> emptyResult = new PageResult<>();
            emptyResult.setTotal(0);
            emptyResult.setPageNum(1);
            emptyResult.setPageSize(10);
            emptyResult.setRecords(List.of());

            when(ticketService.queryTickets(any(TicketQueryRequest.class))).thenReturn(emptyResult);

            mockMvc.perform(get("/api/tickets")
                            .param("status", "completed")
                            .param("priority", "high")
                            .param("reporter", "张三"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)))
                    .andExpect(jsonPath("$.data.total", is(0)))
                    .andExpect(jsonPath("$.data.records", hasSize(0)));
        }
    }
}
