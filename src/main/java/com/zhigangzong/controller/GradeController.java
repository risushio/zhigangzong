package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.GradeRequests.*;
import com.zhigangzong.service.GradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor public class GradeController {
 private final GradeService service;
 @GetMapping("/batches/{id}/grading") public ApiResponse<?> policy(@PathVariable long id){return ApiResponse.ok(service.settings(id));}
 @PutMapping("/batches/{id}/grading") public ApiResponse<?> savePolicy(@PathVariable long id,@Valid @RequestBody Policy r){return ApiResponse.ok(service.savePolicy(id,r));}
 @GetMapping("/placements/{id}/evaluations") public ApiResponse<?> view(@PathVariable long id){return ApiResponse.ok(service.view(id));}
 @GetMapping("/placements/{id}/evaluations/{type}") public ApiResponse<?> history(@PathVariable long id,@PathVariable String type){return ApiResponse.ok(service.history(id,type));}
 @PutMapping("/placements/{id}/evaluations") public ApiResponse<?> save(@PathVariable long id,@Valid @RequestBody Evaluation r){return ApiResponse.ok(service.save(id,r));}
 @PostMapping("/placements/{id}/evaluations/submit") public ApiResponse<?> submit(@PathVariable long id){return ApiResponse.ok(service.submit(id));}
}
