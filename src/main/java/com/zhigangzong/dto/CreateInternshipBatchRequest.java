package com.zhigangzong.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateInternshipBatchRequest(
        @NotNull @Positive Long departmentId,
        @NotBlank @Size(max = 120) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 10000) String learningObjectives,
        @Size(max = 10000) String taskRequirements,
        @Size(max = 10000) String materialRequirements,
        @Size(max = 10000) String gradingCriteria) {
}
