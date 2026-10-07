package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 异常跟进记录。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class RiskFollowUp {
    private Long id;
    private Long alertId;
    private Long actorId;
    private String content;
    private String result;
    private LocalDateTime createdAt;
}
