package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 协议保险等入岗材料。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class InternshipMaterial {
    private Long id;
    private Long placementId;
    private String materialType;
    private String fileRef;
    private String reviewStatus;
    private String reviewComment;
    private LocalDateTime createdAt;
}
