package com.zhigangzong.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Confirmed profile fields only. Identity and school are resolved by the service. */
public record CreateStandaloneStudentProfileRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 40) String studentNo,
        @NotBlank @Size(max = 100) String major,
        @Positive Long departmentId,
        @Pattern(regexp = "1[3-9][0-9]{9}") String phone,
        @Size(max = 1000) String skills,
        @Size(max = 10000) String projectExperience,
        @Size(max = 100) String preferredCity,
        LocalDate availableFrom,
        LocalDate availableTo,
        @Min(1) @Max(7) Integer daysPerWeek,
        @Min(1900) @Max(2100) Integer gradeYear,
        @Size(max = 100) String className) {
}
