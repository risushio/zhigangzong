package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.TransferRequests.*;
import com.zhigangzong.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor
public class TransferController {
    private final TransferService service;
    @GetMapping("/placements/{id}/transfers") public ApiResponse<?> list(@PathVariable long id){return ApiResponse.ok(service.list(id));}
    @PostMapping("/placements/{id}/transfers") public ApiResponse<?> apply(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.apply(id,r));}
    @GetMapping("/transfers/{id}") public ApiResponse<?> detail(@PathVariable long id){return ApiResponse.ok(service.detail(id));}
    @PostMapping("/transfers/{id}/resubmit") public ApiResponse<?> resubmit(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.resubmit(id,r));}
    @PostMapping("/transfers/{id}/review") public ApiResponse<?> review(@PathVariable long id,@Valid @RequestBody Review r){return ApiResponse.ok(service.review(id,r));}
}
