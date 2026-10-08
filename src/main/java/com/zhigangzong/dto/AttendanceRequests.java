package com.zhigangzong.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AttendanceRequests {
    private AttendanceRequests() {}
    public record Policy(@NotNull Boolean enabled, @NotBlank @Size(max=480) String note) {}
    public record Apply(@NotNull LocalDate attendanceDate,
        @NotBlank @Pattern(regexp="CHECK_IN|LEAVE|MAKE_UP") String recordType,
        @NotBlank @Size(max=480) String note) {}
    public record Resubmit(@NotBlank @Size(max=480) String note) {}
    public record Review(@NotBlank @Pattern(regexp="APPROVED|RETURNED|REJECTED") String decision,
        @NotBlank @Size(max=480) String feedback) {}
}
