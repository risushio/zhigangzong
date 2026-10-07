package com.zhigangzong.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;

public record CreateJobPositionRequest(
        @NotNull @Positive Long enterpriseId,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 10000) String description,
        @Size(max = 100) String requiredMajor,
        @Size(max = 1000) String requiredSkills,
        @NotBlank @Size(max = 100) String city,
         LocalDate startDate,
         LocalDate endDate,
        @Min(1) @Max(7) Integer daysPerWeek,
        @NotNull @Min(1) Integer headcount,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal monthlyPay,
        @Size(max = 120) String workingHours,
         LocalDate applicationDeadline) {
}
