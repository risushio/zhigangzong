package com.zhigangzong.dto;
import jakarta.validation.constraints.*;
public record PublicationRequest(@NotBlank @Pattern(regexp="PUBLISHED|OFFLINE") String status) {}
