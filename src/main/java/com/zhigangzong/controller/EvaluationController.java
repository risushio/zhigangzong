package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EvaluationController {
    private final EvaluationService service;

    @GetMapping("/evaluations")
    public ApiResponse<PageResult<InternshipEvaluation>> listInternshipEvaluation(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listInternshipEvaluation(page, size));
    }

    @GetMapping("/evaluations/{id}")
    public ApiResponse<InternshipEvaluation> getInternshipEvaluation(@PathVariable long id) {
        return ApiResponse.ok(service.getInternshipEvaluation(id));
    }

    @GetMapping("/archives")
    public ApiResponse<PageResult<ArchiveRecord>> listArchiveRecord(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listArchiveRecord(page, size));
    }

    @GetMapping("/archives/{id}")
    public ApiResponse<ArchiveRecord> getArchiveRecord(@PathVariable long id) {
        return ApiResponse.ok(service.getArchiveRecord(id));
    }
}
