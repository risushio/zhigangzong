package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.TerminationRequests.*;
import com.zhigangzong.service.TerminationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor
public class TerminationController {
    private final TerminationService service;
    @GetMapping("/placements/{id}/terminations") public ApiResponse<?> list(@PathVariable long id){return ApiResponse.ok(service.list(id));}
    @PostMapping("/placements/{id}/terminations") public ApiResponse<?> apply(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.apply(id,r));}
    @GetMapping("/terminations/{id}") public ApiResponse<?> detail(@PathVariable long id){return ApiResponse.ok(service.detail(id));}
    @PostMapping("/terminations/{id}/resubmit") public ApiResponse<?> resubmit(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.resubmit(id,r));}
    @PostMapping("/terminations/{id}/review") public ApiResponse<?> review(@PathVariable long id,@Valid @RequestBody Review r){return ApiResponse.ok(service.review(id,r));}
}
