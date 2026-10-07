package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchingController {
    private final MatchingService service;

    @GetMapping("/recommendations")
    public ApiResponse<java.util.List<com.zhigangzong.vo.JobRecommendation>> recommendations(
            @RequestParam long studentId) {
        return ApiResponse.ok(service.recommendations(studentId));
    }

    @GetMapping("/match-feedback")
    public ApiResponse<PageResult<MatchFeedback>> listMatchFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listMatchFeedback(page, size));
    }

    @GetMapping("/match-feedback/{id}")
    public ApiResponse<MatchFeedback> getMatchFeedback(@PathVariable long id) {
        return ApiResponse.ok(service.getMatchFeedback(id));
    }
}
