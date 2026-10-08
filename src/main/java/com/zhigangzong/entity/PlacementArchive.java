package com.zhigangzong.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class PlacementArchive {
 private Long id,placementId,resultFileId,reviewerId;
 private String summary,status,snapshot,reviewComment;
 private LocalDateTime archivedAt;
}
