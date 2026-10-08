package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.WarningRequests.Policy;
import com.zhigangzong.dto.CaseRequests.Note;
import com.zhigangzong.service.WarningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor public class WarningController {
 private final WarningService service;
 @GetMapping("/rule-batches") public ApiResponse<?> batches(){return ApiResponse.ok(service.batches());}
 @GetMapping("/batches/{id}/warnings") public ApiResponse<?> settings(@PathVariable long id){return ApiResponse.ok(service.settings(id));}
 @PutMapping("/batches/{id}/warnings") public ApiResponse<?> save(@PathVariable long id,@Valid @RequestBody Policy r){return ApiResponse.ok(service.save(id,r));}
 @PostMapping("/batches/{id}/warnings/scan") public ApiResponse<?> scan(@PathVariable long id){return ApiResponse.ok(service.scan(id));}
 @GetMapping("/placements/{id}/contacts") public ApiResponse<?> contacts(@PathVariable long id){return ApiResponse.ok(service.contacts(id));}
 @PostMapping("/placements/{id}/contacts") public ApiResponse<?> contact(@PathVariable long id,@Valid @RequestBody Note r){return ApiResponse.ok(service.contact(id,r.note()));}
}
