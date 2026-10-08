package com.zhigangzong.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public final class ScopeRuleRequests {
 private ScopeRuleRequests(){}
 public record Policy(@Min(1)@Max(366)int minimumDays,boolean approvalMaterials,boolean attendanceEnabled,
  boolean reports,@Min(1)@Max(90)int frequencyDays,@Min(0)@Max(90)int reportGraceDays,
  @Min(0)@Max(366)int materialDays,@NotNull @Size(max=4)List<@Pattern(regexp="AGREEMENT|INSURANCE|RESULT|OTHER")String> requiredKinds,
  @Valid @NotNull GradeRequests.Policy grading){}
 public record Save(@Positive long departmentId,@Positive Long batchId,@Size(max=100)String major,@Valid @NotNull Policy policy){}
}
