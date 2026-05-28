package com.ticket.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定义错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    PARAM_ERROR(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    DUPLICATE_TICKET_ID(409, "工单号已存在"),
    BUSINESS_ERROR(500, "业务异常");

    private final int code;
    private final String message;
}
