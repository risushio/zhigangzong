package com.zhigangzong.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class ExtensionRequests {
    private ExtensionRequests() {}
    public record Apply(@NotNull LocalDate endDate,@NotBlank @Size(max=480) String reason) {}
    public record Review(@NotBlank @Pattern(regexp="APPROVED|RETURNED|REJECTED") String decision,
        @NotBlank @Size(max=480) String comment) {}
    public record Dates(LocalDate startDate,LocalDate endDate) {}
}
