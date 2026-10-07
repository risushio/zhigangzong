package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.EnterpriseService;
import com.zhigangzong.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnterpriseController {
    private final EnterpriseService service;

    @GetMapping("/enterprises")
    public ApiResponse<PageResult<Enterprise>> listEnterprise(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listEnterprise(page, size));
    }

    @GetMapping("/enterprises/{id}")
    public ApiResponse<Enterprise> getEnterprise(@PathVariable long id) {
        return ApiResponse.ok(service.getEnterprise(id));
    }

    @PostMapping("/enterprises")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Enterprise> createEnterprise(@Valid @RequestBody CreateEnterpriseRequest request) {
        return ApiResponse.ok(service.createEnterprise(request));
    }
}
