package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.PortalRequests.SelfPlacement;
import com.zhigangzong.service.SelfPlacementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor
public class SelfPlacementController {
    private final SelfPlacementService service;
    @PostMapping("/self-placements") public ApiResponse<?> create(@Valid @RequestBody SelfPlacement r){return ApiResponse.ok(service.create(r));}
    @PutMapping("/self-placements/{id}") public ApiResponse<?> save(@PathVariable long id,@Valid @RequestBody SelfPlacement r){return ApiResponse.ok(service.save(id,r));}
    @PostMapping("/self-placements/{id}/submit") public ApiResponse<?> submit(@PathVariable long id){return ApiResponse.ok(service.submit(id));}
}
