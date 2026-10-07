package com.zhigangzong.dto;
import jakarta.validation.constraints.*;
public record ReviewRequest(
        @NotBlank @Pattern(regexp="APPROVED|REJECTED|SUSPENDED") String decision,
        @NotBlank @Size(max=480) String note) {}
