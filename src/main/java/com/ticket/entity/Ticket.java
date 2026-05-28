package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单实体类
 */
@Data
@TableName("ticket")
public class Ticket {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 工单号 */
    private String ticketId;

    /** 状态: pending, processing, completed, cancelled */
    private String status;

    /** 优先级: high, medium, low */
    private String priority;

    /** 故障描述 */
    private String faultDescription;

    /** 工位位置 */
    private String location;

    /** 提交人 */
    private String reporter;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
