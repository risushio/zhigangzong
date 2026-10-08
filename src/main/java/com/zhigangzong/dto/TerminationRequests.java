package com.zhigangzong.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class TerminationRequests {
    private TerminationRequests() {}
    public record Apply(@NotBlank @Size(max=480) String reason) {}
    public record Review(@NotBlank @Pattern(regexp="APPROVED|RETURNED|REJECTED") String decision,
        @NotBlank @Size(max=480) String comment) {}
    public record Arrangement(Long enterpriseId,Long jobId,String positionTitle,String approvalStatus,
        Long teacherId,Long enterpriseMentorId,LocalDate startDate,LocalDate endDate) {}
}
