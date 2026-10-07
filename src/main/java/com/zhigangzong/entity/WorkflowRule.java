package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 专业批次流程配置。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class WorkflowRule {
    private Long id;
    private Long departmentId;
    private Long batchId;
    private String major;
    private Boolean attendanceEnabled;
    private Integer reportFrequencyDays;
    private String approvalSteps;
    private String materialTemplate;
    private String evaluationWeights;
    private LocalDateTime createdAt;
}
