package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.AttendanceRequests.*;
import com.zhigangzong.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor
public class AttendanceWorkflowController {
    private final AttendanceService service;
    @GetMapping("/placements/{id}/attendance") public ApiResponse<?> list(@PathVariable long id){return ApiResponse.ok(service.list(id));}
    @PutMapping("/placements/{id}/attendance-policy") public ApiResponse<?> policy(@PathVariable long id,@Valid @RequestBody Policy r){return ApiResponse.ok(service.policy(id,r));}
    @PostMapping("/placements/{id}/attendance") public ApiResponse<?> apply(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.apply(id,r));}
    @GetMapping("/attendance/{id}") public ApiResponse<?> detail(@PathVariable long id){return ApiResponse.ok(service.detail(id));}
    @PostMapping("/attendance/{id}/resubmit") public ApiResponse<?> resubmit(@PathVariable long id,@Valid @RequestBody Resubmit r){return ApiResponse.ok(service.resubmit(id,r));}
    @PostMapping("/attendance/{id}/review") public ApiResponse<?> review(@PathVariable long id,@Valid @RequestBody Review r){return ApiResponse.ok(service.review(id,r));}
}
