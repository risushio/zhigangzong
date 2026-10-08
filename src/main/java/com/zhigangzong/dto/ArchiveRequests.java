package com.zhigangzong.dto;
import jakarta.validation.constraints.*;
public final class ArchiveRequests {
 private ArchiveRequests() {}
 public record Apply(@NotBlank @Size(max=4000) String summary,@NotNull @Positive Long resultFileId) {}
 public record Review(@NotBlank @Pattern(regexp="APPROVED|RETURNED") String decision,@NotBlank @Size(max=480) String comment) {}
}
