package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 实习变更申请；延期支持审批、原日期快照及独立操作历史。 */
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
