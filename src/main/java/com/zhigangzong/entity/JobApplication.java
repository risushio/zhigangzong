package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 投递与录用。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class JobApplication {
    private Long id;
    private Long studentId;
    private Long jobId;
    private String resumeRef;
    private String recruitmentStatus;
    private LocalDateTime interviewAt;
    private String offerDetails;
    private Boolean studentConfirmed;
    private LocalDateTime createdAt;
}
