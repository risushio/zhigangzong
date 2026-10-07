package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.PortalRequests.*;
import com.zhigangzong.service.PortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor
public class PortalController {
    private final PortalService service;
    @PostMapping("/accounts") public ApiResponse<?> account(@Valid @RequestBody Account r){return ApiResponse.ok(service.account(r));}
    @GetMapping("/profile") public ApiResponse<?> profile(){return ApiResponse.ok(service.profile());}
    @PutMapping("/profile") public ApiResponse<?> profile(@Valid @RequestBody Profile r){return ApiResponse.ok(service.profile(r));}
    @GetMapping("/jobs") public ApiResponse<?> jobs(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(service.jobs(q,page,size));}
    @GetMapping("/applications") public ApiResponse<?> applications(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(service.applications(page,size));}
    @PostMapping("/applications") public ApiResponse<?> apply(@Valid @RequestBody Apply r){return ApiResponse.ok(service.apply(r));}
    @GetMapping("/applications/{id}") public ApiResponse<?> application(@PathVariable long id){return ApiResponse.ok(service.application(id));}
    @PostMapping("/applications/{id}/transition") public ApiResponse<?> transition(@PathVariable long id,@Valid @RequestBody Transition r){return ApiResponse.ok(service.transition(id,r));}
    @GetMapping("/batches") public ApiResponse<?> batches(){return ApiResponse.ok(service.batches());}
    @GetMapping("/placements") public ApiResponse<?> placements(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(service.placements(page,size));}
    @PostMapping("/placements") public ApiResponse<?> placement(@Valid @RequestBody Placement r){return ApiResponse.ok(service.submitPlacement(r));}
    @GetMapping("/placements/{id}") public ApiResponse<?> placement(@PathVariable long id){return ApiResponse.ok(service.placement(id));}
    @PostMapping("/placements/{id}/approval") public ApiResponse<?> approve(@PathVariable long id,@Valid @RequestBody Approval r){return ApiResponse.ok(service.approve(id,r));}
    @PostMapping("/placements/{id}/resubmit") public ApiResponse<?> resubmit(@PathVariable long id,@Valid @RequestBody Placement r){return ApiResponse.ok(service.resubmit(id,r));}
    @GetMapping("/notifications") public ApiResponse<?> notifications(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(service.notifications(page,size));}
    @PostMapping("/notifications/{id}/read") public ApiResponse<?> read(@PathVariable long id){service.read(id);return ApiResponse.ok(null);}
}
