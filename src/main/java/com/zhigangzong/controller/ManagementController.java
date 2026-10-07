package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.*;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.ManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ManagementController {
    private final ManagementService service;
    @PutMapping("/enterprises/{id}") public ApiResponse<Enterprise> updateEnterprise(@PathVariable long id,@Valid @RequestBody CreateEnterpriseRequest r) {return ApiResponse.ok(service.updateEnterprise(id,r));}
    @PutMapping("/jobs/{id}") public ApiResponse<JobPosition> updateJob(@PathVariable long id,@Valid @RequestBody CreateJobPositionRequest r) {return ApiResponse.ok(service.updateJob(id,r));}
    @PostMapping("/enterprises/{id}/review") public ApiResponse<Enterprise> reviewEnterprise(@PathVariable long id,@Valid @RequestBody ReviewRequest r) {return ApiResponse.ok(service.reviewEnterprise(id,r));}
    @PostMapping("/jobs/{id}/review") public ApiResponse<JobPosition> reviewJob(@PathVariable long id,@Valid @RequestBody ReviewRequest r) {return ApiResponse.ok(service.reviewJob(id,r));}
    @PostMapping("/jobs/{id}/publication") public ApiResponse<JobPosition> publication(@PathVariable long id,@Valid @RequestBody PublicationRequest r) {return ApiResponse.ok(service.publication(id,r));}
}
