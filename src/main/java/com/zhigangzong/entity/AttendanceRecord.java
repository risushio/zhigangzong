package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 出勤与补签。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class AttendanceRecord {
    private Long id;
    private Long placementId;
    private LocalDate attendanceDate;
    private String recordType;
    private String status;
    private String note;
    private Long reviewerId;
    private LocalDateTime createdAt;
}
