package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 招聘状态历史。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class RecruitmentEvent {
    private Long id;
    private Long applicationId;
    private Long actorId;
    private String fromStatus;
    private String toStatus;
    private String note;
    private LocalDateTime createdAt;
}
