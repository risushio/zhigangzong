package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 异常预警与求助。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class RiskAlert {
    private Long id;
    private Long studentId;
    private Long placementId;
    private String alertType;
    private String description;
    private Long ownerId;
    private String status;
    private String resolution;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
