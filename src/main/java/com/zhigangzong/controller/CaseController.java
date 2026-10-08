package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.CaseRequests.*;
import com.zhigangzong.service.CaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor public class CaseController {
 private final CaseService service;
 @GetMapping("/cases") public ApiResponse<?> list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(service.list(page,size));}
 @PostMapping("/cases") public ApiResponse<?> create(@Valid @RequestBody Create r){return ApiResponse.ok(service.create(r));}
 @GetMapping("/case-owners") public ApiResponse<?> owners(){return ApiResponse.ok(service.owners());}
 @GetMapping("/cases/{id}") public ApiResponse<?> detail(@PathVariable long id){return ApiResponse.ok(service.detail(id));}
 @PostMapping("/cases/{id}/assign") public ApiResponse<?> assign(@PathVariable long id,@Valid @RequestBody Assign r){return ApiResponse.ok(service.assign(id,r));}
 @PostMapping("/cases/{id}/follow") public ApiResponse<?> follow(@PathVariable long id,@Valid @RequestBody Note r){return ApiResponse.ok(service.follow(id,r));}
 @PostMapping("/cases/{id}/resolve") public ApiResponse<?> resolve(@PathVariable long id,@Valid @RequestBody Note r){return ApiResponse.ok(service.resolve(id,r));}
 @PostMapping("/cases/{id}/confirm") public ApiResponse<?> confirm(@PathVariable long id,@Valid @RequestBody Confirm r){return ApiResponse.ok(service.confirm(id,r));}
 @PostMapping("/cases/{id}/review") public ApiResponse<?> review(@PathVariable long id,@Valid @RequestBody Review r){return ApiResponse.ok(service.review(id,r));}
}
