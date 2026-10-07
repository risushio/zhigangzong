package com.zhigangzong.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class StoredFile {
    private Long id,ownerUserId,schoolId,placementId;
    private String kind,originalName,contentType,sha256,reviewStatus,reviewComment;
    @JsonIgnore private String storageKey;
    private Long byteSize;
    private LocalDateTime createdAt;
}
