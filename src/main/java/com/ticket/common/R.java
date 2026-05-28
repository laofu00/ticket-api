package com.ticket.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果封装
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    private int code;
    private String message;
    private T data;

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    /**
     * 失败响应（自定义错误码和消息）
     */
    public static <T> R<T> error(int code, String message) {
        return new R<>(code, message, null);
    }

    /**
     * 失败响应（使用错误码枚举）
     */
    public static <T> R<T> error(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败响应（使用错误码枚举 + 自定义消息）
     */
    public static <T> R<T> error(ErrorCode errorCode, String message) {
        return new R<>(errorCode.getCode(), message, null);
    }
}
