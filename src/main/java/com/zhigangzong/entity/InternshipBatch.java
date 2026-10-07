package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 实习计划批次。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class InternshipBatch {
    private Long id;
    private Long departmentId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String learningObjectives;
    private String taskRequirements;
    private String materialRequirements;
    private String gradingCriteria;
    private LocalDateTime createdAt;
}
