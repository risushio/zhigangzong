package com.zhigangzong.entity;
import lombok.Data;
@Data public class GradeReview {
 private Long id,placementId,reviewerId;
 private String reason,status,originalSnapshot,requestedSnapshot,reviewComment;
}
