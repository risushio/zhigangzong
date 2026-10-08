package com.zhigangzong.dto;
import jakarta.validation.constraints.*;
public final class MatchRequests {
 private MatchRequests(){}
 public record Feedback(@Positive long jobId,@NotBlank @Pattern(regexp="NOT_INTERESTED|NOT_SUITABLE")String feedbackType,@NotBlank @Size(max=480)String reason){}
 public record Handle(@NotBlank @Size(max=480)String note){}
}
