package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 推荐解释反馈。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class MatchFeedback {
    private Long id;
    private Long studentId;
    private Long jobId;
    private String feedbackType;
    private String reason;
    private LocalDateTime createdAt;
}
