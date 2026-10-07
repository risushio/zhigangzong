package com.zhigangzong.dto;

import jakarta.validation.constraints.*;


public record CreateUserAccountRequest(
        @NotNull @Positive Long schoolId,
        @Positive Long departmentId,
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Pattern(regexp = "STUDENT|TEACHER|RECRUITER|ENTERPRISE_MENTOR|DEPARTMENT_ADMIN|SCHOOL_ADMIN") String role,
        @Email @Size(max = 160) String email) {
}
