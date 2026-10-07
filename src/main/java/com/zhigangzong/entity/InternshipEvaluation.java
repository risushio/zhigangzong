package com.zhigangzong.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 多方评价。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class InternshipEvaluation {
    private Long id;
    private Long placementId;
    private Long evaluatorId;
    private String evaluationType;
    private BigDecimal score;
    private String comment;
    private String status;
    private LocalDateTime createdAt;
}
