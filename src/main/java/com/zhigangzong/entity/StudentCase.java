package com.zhigangzong.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class StudentCase {
    private Long id,schoolId,studentId,placementId,ownerId;
    private String kind,title,description,status,resolution,ruleKey,evidence;
    private Boolean studentConfirmed;
    private LocalDateTime createdAt;
}
