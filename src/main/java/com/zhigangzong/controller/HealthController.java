package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.service.HealthService;
import com.zhigangzong.vo.HealthStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {
    private final HealthService service;

    @GetMapping
    public ApiResponse<HealthStatus> health() {
        return ApiResponse.ok(service.checkDatabase());
    }

    @GetMapping("/live")
    public ApiResponse<Map<String, String>> live() {
        return ApiResponse.ok(Map.of("status", "UP", "application", "zhigangzong"));
    }
}
