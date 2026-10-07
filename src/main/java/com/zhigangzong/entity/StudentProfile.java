package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 学生档案。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class StudentProfile {
    private Long id;
    private Long userId;
    private String studentNo;
    private String major;
    private String skills;
    private String projectExperience;
    private String resumeRef;
    private String preferredCity;
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private Integer daysPerWeek;
    private LocalDateTime createdAt;
}
