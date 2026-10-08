package com.zhigangzong.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class GradeRequests {
 private GradeRequests() {}
 public record Policy(@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal studentWeight,
  @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal enterpriseWeight,
  @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal teacherWeight,
  @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal passScore,
  @NotBlank @Size(max=480) String note) {}
 public record Evaluation(@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal score,@NotBlank @Size(max=4000) String comment) {}
}
