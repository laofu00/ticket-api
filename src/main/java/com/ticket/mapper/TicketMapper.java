package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工单 Mapper 接口
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 查询今天最大的工单号（用于生成自增序号）
     *
     * @param datePrefix 日期前缀，如 "IT-20260526"
     * @return 当天最大的工单号，没有则返回 null
     */
    @Select("SELECT ticket_id FROM ticket WHERE ticket_id LIKE CONCAT(#{datePrefix}, '-%') ORDER BY ticket_id DESC LIMIT 1")
    String selectMaxTicketIdForToday(@Param("datePrefix") String datePrefix);
}
