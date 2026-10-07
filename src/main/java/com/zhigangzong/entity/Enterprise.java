package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 企业与基地。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class Enterprise {
    private Long id;
    private String name;
    private String creditCode;
    private String contactName;
    private String contactPhone;
    private String qualificationRef;
    private String cooperationNotes;
    private String inspectionNotes;
    private String reviewStatus;
    private String suspensionReason;
    private LocalDateTime createdAt;
}
