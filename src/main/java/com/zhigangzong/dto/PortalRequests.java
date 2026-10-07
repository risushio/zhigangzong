package com.zhigangzong.dto;

import jakarta.validation.constraints.*;
import java.time.*;

/** Requests for the scoped student/recruiter workflow. */
public final class PortalRequests {
    private PortalRequests() {}
    public record Mentors(@NotNull @Positive Long teacherId, @NotNull @Positive Long enterpriseMentorId,
        @NotBlank @Size(max=480) String note) {}
    public record Arrival(@NotNull LocalDate arrivalDate, @NotBlank @Size(max=480) String note) {}
    public record Report(@NotBlank @Size(max=160) String title, @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd, @NotBlank @Size(max=10000) String content) {}
    public record ReportReview(@NotBlank @Pattern(regexp="REVIEWED|RETURNED") String decision,
        @NotBlank @Size(max=480) String feedback) {}
    public record Account(@NotNull @Positive Long userId,
        @NotBlank @Pattern(regexp="[a-zA-Z0-9_.-]{3,80}") String username,
        @NotBlank @Size(min=12,max=72) String password, @Positive Long enterpriseId) {}
    public record Apply(@NotNull @Positive Long jobId, @Size(max=500) String resumeRef, @Positive Long resumeFileId) {}
    public record SelfPlacement(@NotNull @Positive Long batchId,
        @NotBlank @Size(max=160) String enterpriseName, @NotBlank @Pattern(regexp="[A-Z0-9-]{6,32}") String creditCode,
        @NotBlank @Size(max=80) String contactName, @NotBlank @Size(max=40) String contactPhone,
        @NotBlank @Size(max=300) String address, @NotBlank @Size(max=120) String positionTitle,
        @NotBlank @Size(max=10000) String duties, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
    public record FileReview(@NotBlank @Pattern(regexp="APPROVED|RETURNED") String decision,
        @NotBlank @Size(max=480) String comment) {}
    public record Transition(@NotBlank @Pattern(regexp="INTERVIEW|OFFERED|ACCEPTED|REJECTED|WITHDRAWN") String status,
        LocalDateTime interviewAt, @Size(max=10000) String offerDetails, @NotBlank @Size(max=480) String note) {}
    public record Placement(@NotNull @Positive Long applicationId, @NotNull @Positive Long batchId,
        @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
    public record Approval(@NotBlank @Pattern(regexp="APPROVED|RETURNED|REJECTED") String decision,
        @NotBlank @Size(max=480) String comment) {}
    public record Profile(@NotBlank @Size(max=100) String major, @Size(max=1000) String skills,
        @Size(max=10000) String projectExperience, @Size(max=500) String resumeRef,
        @Size(max=100) String preferredCity, LocalDate availableFrom, LocalDate availableTo,
        @Min(1) @Max(7) Integer daysPerWeek) {}
}
