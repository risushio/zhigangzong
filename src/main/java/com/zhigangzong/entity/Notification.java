package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 通知与待办。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class Notification {
    private Long id;
    private Long recipientId;
    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private LocalDateTime dueAt;
    private LocalDateTime readAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
