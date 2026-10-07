package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WorkflowController {
    private final WorkflowService service;

    @GetMapping("/notifications")
    public ApiResponse<PageResult<Notification>> listNotification(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listNotification(page, size));
    }

    @GetMapping("/notifications/{id}")
    public ApiResponse<Notification> getNotification(@PathVariable long id) {
        return ApiResponse.ok(service.getNotification(id));
    }

    @GetMapping("/workflow-rules")
    public ApiResponse<PageResult<WorkflowRule>> listWorkflowRule(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listWorkflowRule(page, size));
    }

    @GetMapping("/workflow-rules/{id}")
    public ApiResponse<WorkflowRule> getWorkflowRule(@PathVariable long id) {
        return ApiResponse.ok(service.getWorkflowRule(id));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<AuditLog>> listAuditLog(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listAuditLog(page, size));
    }

    @GetMapping("/audit-logs/{id}")
    public ApiResponse<AuditLog> getAuditLog(@PathVariable long id) {
        return ApiResponse.ok(service.getAuditLog(id));
    }
}
