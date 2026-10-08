package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.ArchiveRequests.*;
import com.zhigangzong.service.ArchiveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
@RestController @RequestMapping("/api/portal/placements/{id}/archive") @RequiredArgsConstructor public class ArchiveController {
 private final ArchiveService service;
 @GetMapping public ApiResponse<?> view(@PathVariable long id){return ApiResponse.ok(service.view(id));}
 @PostMapping public ApiResponse<?> apply(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.apply(id,r));}
 @PostMapping("/resubmit") public ApiResponse<?> resubmit(@PathVariable long id,@Valid @RequestBody Apply r){return ApiResponse.ok(service.resubmit(id,r));}
 @PostMapping("/review") public ApiResponse<?> review(@PathVariable long id,@Valid @RequestBody Review r){return ApiResponse.ok(service.review(id,r));}
 @GetMapping("/export") public ResponseEntity<byte[]> export(@PathVariable long id)throws IOException{return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"placement-"+id+"-archive.zip\"").header(HttpHeaders.CACHE_CONTROL,"no-store, private").contentType(MediaType.parseMediaType("application/zip")).body(service.export(id));}
}
