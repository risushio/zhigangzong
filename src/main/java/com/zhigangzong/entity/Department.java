package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 学院。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class Department {
    private Long id;
    private Long schoolId;
    private String name;
    private String code;
    private LocalDateTime createdAt;
}
