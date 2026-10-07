package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 审批历史。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class ApprovalRecord {
    private Long id;
    private Long placementId;
    private Long approverId;
    private String decision;
    private String comment;
    private LocalDateTime createdAt;
}
