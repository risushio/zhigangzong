package com.zhigangzong.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateStudentProfileRequest(
        @NotNull @Positive Long userId,
        @NotBlank @Size(max = 40) String studentNo,
        @NotBlank @Size(max = 100) String major,
        @Size(max = 1000) String skills,
        @Size(max = 10000) String projectExperience,
        @Size(max = 500) String resumeRef,
        @Size(max = 100) String preferredCity,
         LocalDate availableFrom,
         LocalDate availableTo,
        @Min(1) @Max(7) Integer daysPerWeek) {
}
