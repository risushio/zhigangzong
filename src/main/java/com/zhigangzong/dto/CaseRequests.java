package com.zhigangzong.dto;
import jakarta.validation.constraints.*;
public final class CaseRequests {
    private CaseRequests() {}
    public record Create(@Positive Long placementId,@NotBlank @Size(max=160) String title,@NotBlank @Size(max=4000) String description) {}
    public record Note(@NotBlank @Size(max=4000) String note) {}
    public record Assign(@NotNull @Positive Long ownerId,@NotBlank @Size(max=480) String note) {}
    public record Confirm(@NotBlank @Pattern(regexp="ACCEPT|REOPEN") String decision,@NotBlank @Size(max=480) String note) {}
    public record Review(@NotBlank @Pattern(regexp="CLOSE|RETURN") String decision,@NotBlank @Size(max=480) String note) {}
}
