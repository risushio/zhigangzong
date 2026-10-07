package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 岗位。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class JobPosition {
    private Long id;
    private Long enterpriseId;
    private String title;
    private String description;
    private String requiredMajor;
    private String requiredSkills;
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysPerWeek;
    private Integer headcount;
    private BigDecimal monthlyPay;
    private String workingHours;
    private LocalDate applicationDeadline;
    private String reviewStatus;
    private String publishStatus;
    private LocalDateTime createdAt;
}
