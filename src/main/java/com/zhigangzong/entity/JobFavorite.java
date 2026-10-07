package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 岗位收藏。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class JobFavorite {
    private Long id;
    private Long studentId;
    private Long jobId;
    private LocalDateTime createdAt;
}
