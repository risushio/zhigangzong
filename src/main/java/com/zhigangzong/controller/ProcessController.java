package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.PortalRequests.*;
import com.zhigangzong.service.ProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor
public class ProcessController {
    private final ProcessService service;
    @GetMapping("/placements/{id}/mentors") public ApiResponse<?> mentors(@PathVariable long id){return ApiResponse.ok(service.mentors(id));}
    @PostMapping("/placements/{id}/mentors") public ApiResponse<?> assign(@PathVariable long id,@Valid @RequestBody Mentors r){return ApiResponse.ok(service.assign(id,r));}
    @PostMapping("/placements/{id}/arrival") public ApiResponse<?> arrive(@PathVariable long id,@Valid @RequestBody Arrival r){return ApiResponse.ok(service.arrive(id,r));}
    @GetMapping("/placements/{id}/process") public ApiResponse<?> process(@PathVariable long id){return ApiResponse.ok(service.process(id));}
    @PostMapping("/placements/{id}/reports") public ApiResponse<?> create(@PathVariable long id,@Valid @RequestBody Report r){return ApiResponse.ok(service.create(id,r));}
    @GetMapping("/reports/{id}") public ApiResponse<?> report(@PathVariable long id){return ApiResponse.ok(service.report(id));}
    @PutMapping("/reports/{id}") public ApiResponse<?> save(@PathVariable long id,@Valid @RequestBody Report r){return ApiResponse.ok(service.save(id,r));}
    @PostMapping("/reports/{id}/submit") public ApiResponse<?> submit(@PathVariable long id){return ApiResponse.ok(service.submit(id));}
    @PostMapping("/reports/{id}/review") public ApiResponse<?> review(@PathVariable long id,@Valid @RequestBody ReportReview r){return ApiResponse.ok(service.review(id,r));}
}
