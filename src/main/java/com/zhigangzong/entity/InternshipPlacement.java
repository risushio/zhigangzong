package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 学校实习审批、双导师及到岗状态；自主申报提交仍待实现。 */
@Data
public class InternshipPlacement {
    private Long id;
    private Long previousPlacementId;
    private Long replacementPlacementId;
    private Long archiveId;
    private String archiveStatus;
    private Long terminationRequestId;
    private LocalDateTime terminatedAt;
    private Long studentId;
    private Long batchId;
    private String source;
    private Long applicationId;
    private Long enterpriseId;
    private Long jobId;
    private String positionTitle;
    private String schoolApprovalStatus;
    private String arrivalStatus;
    private Long teacherId;
    private Long enterpriseMentorId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
