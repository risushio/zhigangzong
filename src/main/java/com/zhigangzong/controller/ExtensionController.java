package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.ExtensionRequests.*;
import com.zhigangzong.service.ExtensionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor
public class ExtensionController {
    private final ExtensionService service;
    @GetMapping("/placements/{id}/extensions") public ApiResponse<?> list(@PathVariable long id){return ApiResponse.ok(service.list(id));}
    @PostMapping("/placements/{id}/extensions") public ApiResponse<?> apply(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.apply(id,r));}
    @GetMapping("/extensions/{id}") public ApiResponse<?> detail(@PathVariable long id){return ApiResponse.ok(service.detail(id));}
    @PostMapping("/extensions/{id}/resubmit") public ApiResponse<?> resubmit(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.resubmit(id,r));}
    @PostMapping("/extensions/{id}/review") public ApiResponse<?> review(@PathVariable long id,@Valid @RequestBody Review r){return ApiResponse.ok(service.review(id,r));}
}
