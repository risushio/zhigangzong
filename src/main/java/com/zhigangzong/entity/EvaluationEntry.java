package com.zhigangzong.entity;
import lombok.Data;
import java.math.BigDecimal;
@Data public class EvaluationEntry {
 private Long id,placementId,evaluatorId;
 private String type,status,comment;
 private BigDecimal score;
 private Integer revision;
}
