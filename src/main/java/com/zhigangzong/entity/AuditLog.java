package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 操作留痕。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class AuditLog {
    private Long id;
    private Long actorId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String description;
    private LocalDateTime createdAt;
}
