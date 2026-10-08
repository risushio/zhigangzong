package com.zhigangzong.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class TransferRequests {
    private TransferRequests() {}
    public record Apply(@NotBlank @Pattern(regexp="PLATFORM|SELF") String source,
        @Positive Long applicationId, @Valid PortalRequests.SelfPlacement self,
        @NotNull LocalDate startDate, @NotNull LocalDate endDate,
        @NotBlank @Size(max=480) String reason) {}
    public record Review(@NotBlank @Pattern(regexp="APPROVED|RETURNED|REJECTED") String decision,
        @NotBlank @Size(max=480) String comment) {}
    public record Original(Long enterpriseId,String enterpriseName,Long jobId,String positionTitle,
        String approvalStatus,Long teacherId,Long enterpriseMentorId,LocalDate startDate,LocalDate endDate,Long terminationRequestId) {}
    public record Target(String source,Long applicationId,Long enterpriseId,String enterpriseName,
        Long jobId,String positionTitle,LocalDate startDate,LocalDate endDate,PortalRequests.SelfPlacement self) {}
}
