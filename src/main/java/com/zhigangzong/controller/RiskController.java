package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RiskController {
    private final RiskService service;

    @GetMapping("/alerts")
    public ApiResponse<PageResult<RiskAlert>> listRiskAlert(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listRiskAlert(page, size));
    }

    @GetMapping("/alerts/{id}")
    public ApiResponse<RiskAlert> getRiskAlert(@PathVariable long id) {
        return ApiResponse.ok(service.getRiskAlert(id));
    }

    @GetMapping("/alert-follow-ups")
    public ApiResponse<PageResult<RiskFollowUp>> listRiskFollowUp(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listRiskFollowUp(page, size));
    }

    @GetMapping("/alert-follow-ups/{id}")
    public ApiResponse<RiskFollowUp> getRiskFollowUp(@PathVariable long id) {
        return ApiResponse.ok(service.getRiskFollowUp(id));
    }
}
