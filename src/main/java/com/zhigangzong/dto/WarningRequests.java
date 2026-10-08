package com.zhigangzong.dto;
import jakarta.validation.constraints.*;
import java.util.List;
public final class WarningRequests {
 private WarningRequests() {}
 public record Policy(@NotNull Boolean unplaced,@Min(0) @Max(365) int unplacedGraceDays,
  @NotNull Boolean materials,@Min(0) @Max(365) int materialDays,
  @NotNull @Size(max=5) List<@Pattern(regexp="AGREEMENT|INSURANCE|RESULT|OTHER") String> requiredKinds,
  @NotNull Boolean reports,@Min(1) @Max(365) int frequencyDays,@Min(0) @Max(365) int reportGraceDays,
  @NotNull Boolean contact,@Min(1) @Max(365) int contactDays) {}
}
