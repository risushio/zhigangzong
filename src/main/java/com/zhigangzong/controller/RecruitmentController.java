package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.RecruitmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecruitmentController {
    private final RecruitmentService service;

    @GetMapping("/applications")
    public ApiResponse<PageResult<JobApplication>> listJobApplication(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listJobApplication(page, size));
    }

    @GetMapping("/applications/{id}")
    public ApiResponse<JobApplication> getJobApplication(@PathVariable long id) {
        return ApiResponse.ok(service.getJobApplication(id));
    }

    @GetMapping("/recruitment-events")
    public ApiResponse<PageResult<RecruitmentEvent>> listRecruitmentEvent(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listRecruitmentEvent(page, size));
    }

    @GetMapping("/recruitment-events/{id}")
    public ApiResponse<RecruitmentEvent> getRecruitmentEvent(@PathVariable long id) {
        return ApiResponse.ok(service.getRecruitmentEvent(id));
    }
}
