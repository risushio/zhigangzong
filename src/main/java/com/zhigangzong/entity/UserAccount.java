package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 用户角色。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class UserAccount {
    private Long id;
    private Long schoolId;
    private Long departmentId;
    private String displayName;
    private String role;
    private String email;
    private LocalDateTime createdAt;
}
