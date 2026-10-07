package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 结项与归档。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class ArchiveRecord {
    private Long id;
    private Long placementId;
    private String summaryRef;
    private String appraisalRef;
    private String status;
    private Long reviewerId;
    private LocalDateTime archivedAt;
    private LocalDateTime createdAt;
}
