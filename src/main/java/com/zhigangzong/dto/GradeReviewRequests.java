package com.zhigangzong.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public final class GradeReviewRequests {
 private GradeReviewRequests() {}
 public record Apply(@NotBlank @Size(max=480) String reason) {}
 public record Review(@NotBlank @Pattern(regexp="APPROVED|RETURNED|REJECTED") String decision,@NotBlank @Size(max=480) String comment,
  @NotNull @Size(max=3) List<@Pattern(regexp="STUDENT|ENTERPRISE|TEACHER") String> returnTypes) {}
}
