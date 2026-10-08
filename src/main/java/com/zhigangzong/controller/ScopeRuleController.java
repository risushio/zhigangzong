package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.ScopeRuleRequests.Save;
import com.zhigangzong.service.ScopeRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor public class ScopeRuleController {
 private final ScopeRuleService service;
 @GetMapping("/workflow-policies")public ApiResponse<?> list(){return ApiResponse.ok(service.list());}
 @PostMapping("/workflow-policies")public ApiResponse<?> save(@Valid @RequestBody Save r)throws java.io.IOException{return ApiResponse.ok(service.save(r));}
 @GetMapping("/placements/{id}/workflow-policy")public ApiResponse<?> effective(@PathVariable long id){return ApiResponse.ok(service.effective(id));}
}
