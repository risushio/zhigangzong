package com.zhigangzong.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 学校。当前仅提供基础数据结构，业务状态流转在后续阶段实现。 */
@Data
public class School {
    private Long id;
    private String name;
    private String code;
    private LocalDateTime createdAt;
}
