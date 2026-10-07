package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 实习变更历史。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class ChangeRequest {
    private Long id;
    private Long placementId;
    private String changeType;
    private String originalSnapshot;
    private String requestedSnapshot;
    private String reason;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime createdAt;
}
