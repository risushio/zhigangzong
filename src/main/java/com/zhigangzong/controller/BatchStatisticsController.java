package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.service.BatchStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor public class BatchStatisticsController {
 private final BatchStatisticsService service;
 @GetMapping("/batch-statistics")public ApiResponse<?> statistics(@RequestParam long batchId){return ApiResponse.ok(service.statistics(batchId));}
}
