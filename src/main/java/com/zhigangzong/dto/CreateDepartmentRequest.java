package com.zhigangzong.dto;

import jakarta.validation.constraints.*;


public record CreateDepartmentRequest(
        @NotNull @Positive Long schoolId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String code) {
}
