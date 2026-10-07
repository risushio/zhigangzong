package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 双导师指导与联系。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class GuidanceRecord {
    private Long id;
    private Long placementId;
    private Long mentorId;
    private LocalDateTime contactAt;
    private String content;
    private LocalDateTime nextContactAt;
    private LocalDateTime createdAt;
}
