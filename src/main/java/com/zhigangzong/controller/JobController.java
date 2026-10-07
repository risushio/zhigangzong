package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.JobService;
import com.zhigangzong.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JobController {
    private final JobService service;

    @GetMapping("/jobs")
    public ApiResponse<PageResult<JobPosition>> listJobPosition(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listJobPosition(page, size));
    }

    @GetMapping("/jobs/{id}")
    public ApiResponse<JobPosition> getJobPosition(@PathVariable long id) {
        return ApiResponse.ok(service.getJobPosition(id));
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JobPosition> createJobPosition(@Valid @RequestBody CreateJobPositionRequest request) {
        return ApiResponse.ok(service.createJobPosition(request));
    }

    @GetMapping("/job-favorites")
    public ApiResponse<PageResult<JobFavorite>> listJobFavorite(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listJobFavorite(page, size));
    }

    @GetMapping("/job-favorites/{id}")
    public ApiResponse<JobFavorite> getJobFavorite(@PathVariable long id) {
        return ApiResponse.ok(service.getJobFavorite(id));
    }
}
