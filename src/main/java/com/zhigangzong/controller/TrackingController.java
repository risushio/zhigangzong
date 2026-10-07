package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrackingController {
    private final TrackingService service;

    @GetMapping("/reports")
    public ApiResponse<PageResult<ProgressReport>> listProgressReport(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listProgressReport(page, size));
    }

    @GetMapping("/reports/{id}")
    public ApiResponse<ProgressReport> getProgressReport(@PathVariable long id) {
        return ApiResponse.ok(service.getProgressReport(id));
    }

    @GetMapping("/attendance")
    public ApiResponse<PageResult<AttendanceRecord>> listAttendanceRecord(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listAttendanceRecord(page, size));
    }

    @GetMapping("/attendance/{id}")
    public ApiResponse<AttendanceRecord> getAttendanceRecord(@PathVariable long id) {
        return ApiResponse.ok(service.getAttendanceRecord(id));
    }

    @GetMapping("/changes")
    public ApiResponse<PageResult<ChangeRequest>> listChangeRequest(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listChangeRequest(page, size));
    }

    @GetMapping("/changes/{id}")
    public ApiResponse<ChangeRequest> getChangeRequest(@PathVariable long id) {
        return ApiResponse.ok(service.getChangeRequest(id));
    }

    @GetMapping("/guidance")
    public ApiResponse<PageResult<GuidanceRecord>> listGuidanceRecord(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listGuidanceRecord(page, size));
    }

    @GetMapping("/guidance/{id}")
    public ApiResponse<GuidanceRecord> getGuidanceRecord(@PathVariable long id) {
        return ApiResponse.ok(service.getGuidanceRecord(id));
    }
}
