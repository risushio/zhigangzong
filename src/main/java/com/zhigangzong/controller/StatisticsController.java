package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.service.StatisticsService;
import com.zhigangzong.vo.StatisticsOverview;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService service;

    @GetMapping("/overview")
    public ApiResponse<StatisticsOverview> overview() {
        return ApiResponse.ok(service.overview());
    }
}
