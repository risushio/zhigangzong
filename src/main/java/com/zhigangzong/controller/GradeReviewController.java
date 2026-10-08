package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.GradeReviewRequests.*;
import com.zhigangzong.service.GradeReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor public class GradeReviewController {
 private final GradeReviewService service;
 @GetMapping("/placements/{id}/grade-reviews") public ApiResponse<?> list(@PathVariable long id){return ApiResponse.ok(service.list(id));}
 @PostMapping("/placements/{id}/grade-reviews") public ApiResponse<?> apply(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.apply(id,r));}
 @GetMapping("/grade-reviews/{id}") public ApiResponse<?> detail(@PathVariable long id){return ApiResponse.ok(service.detail(id));}
 @PostMapping("/grade-reviews/{id}/resubmit") public ApiResponse<?> resubmit(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.resubmit(id,r));}
 @PostMapping("/grade-reviews/{id}/review") public ApiResponse<?> review(@PathVariable long id,@Valid @RequestBody Review r){return ApiResponse.ok(service.review(id,r));}
}
