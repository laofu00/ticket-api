package com.ticket.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 工单ID生成工具类
 * <p>
 * 生成规则: IT-YYYYMMDD-XXX
 * 示例: IT-20260518-001
 */
public class TicketIdUtil {

    private static final String PREFIX = "IT";
    private static final String SEPARATOR = "-";
    private static final int SEQ_WIDTH = 3;

    private TicketIdUtil() {
    }

    /**
     * 获取当天日期部分
     */
    public static String getTodayDatePart() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 根据当天最大序号生成下一个工单ID
     *
     * @param maxTicketIdForToday 当天已存在的最大工单ID，没有则传 null
     * @return 新的工单ID，如 IT-20260518-001
     */
    public static String generateTicketId(String maxTicketIdForToday) {
        String datePart = getTodayDatePart();
        int nextSeq = 1;

        if (maxTicketIdForToday != null && !maxTicketIdForToday.isEmpty()) {
            String seqStr = maxTicketIdForToday.substring(maxTicketIdForToday.lastIndexOf(SEPARATOR) + 1);
            nextSeq = Integer.parseInt(seqStr) + 1;
        }

        return PREFIX + SEPARATOR + datePart + SEPARATOR + String.format("%0" + SEQ_WIDTH + "d", nextSeq);
    }
}
