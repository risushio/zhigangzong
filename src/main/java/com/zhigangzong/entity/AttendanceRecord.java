package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 签到、单日请假与补签；每次申请和审批保留独立历史。 */
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
