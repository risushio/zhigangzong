package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.InternshipService;
import com.zhigangzong.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InternshipController {
    private final InternshipService service;

    @GetMapping("/batches")
    public ApiResponse<PageResult<InternshipBatch>> listInternshipBatch(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listInternshipBatch(page, size));
    }

    @GetMapping("/batches/{id}")
    public ApiResponse<InternshipBatch> getInternshipBatch(@PathVariable long id) {
        return ApiResponse.ok(service.getInternshipBatch(id));
    }

    @PostMapping("/batches")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InternshipBatch> createInternshipBatch(@Valid @RequestBody CreateInternshipBatchRequest request) {
        return ApiResponse.ok(service.createInternshipBatch(request));
    }

    @GetMapping("/placements")
    public ApiResponse<PageResult<InternshipPlacement>> listInternshipPlacement(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listInternshipPlacement(page, size));
    }

    @GetMapping("/placements/{id}")
    public ApiResponse<InternshipPlacement> getInternshipPlacement(@PathVariable long id) {
        return ApiResponse.ok(service.getInternshipPlacement(id));
    }

    @GetMapping("/approvals")
    public ApiResponse<PageResult<ApprovalRecord>> listApprovalRecord(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listApprovalRecord(page, size));
    }

    @GetMapping("/approvals/{id}")
    public ApiResponse<ApprovalRecord> getApprovalRecord(@PathVariable long id) {
        return ApiResponse.ok(service.getApprovalRecord(id));
    }

    @GetMapping("/materials")
    public ApiResponse<PageResult<InternshipMaterial>> listInternshipMaterial(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listInternshipMaterial(page, size));
    }

    @GetMapping("/materials/{id}")
    public ApiResponse<InternshipMaterial> getInternshipMaterial(@PathVariable long id) {
        return ApiResponse.ok(service.getInternshipMaterial(id));
    }
}
