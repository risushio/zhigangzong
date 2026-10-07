package com.zhigangzong.dto;

import jakarta.validation.constraints.*;


public record CreateEnterpriseRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 32) String creditCode,
        @Size(max = 80) String contactName,
        @Size(max = 40) String contactPhone,
        @Size(max = 500) String qualificationRef,
        @Size(max = 10000) String cooperationNotes,
        @Size(max = 10000) String inspectionNotes) {
}
