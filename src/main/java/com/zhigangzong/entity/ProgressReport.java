package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 周报草稿、提交、退回与教师批阅；各次内容快照独立保存。 */
@Data
public class ProgressReport {
    private Long id;
    private Long placementId;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String content;
    private String attachmentRef;
    private String status;
    private Long reviewerId;
    private String feedback;
    private LocalDateTime createdAt;
}
