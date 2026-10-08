package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.service.StudentSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/portal/student-sheet") @RequiredArgsConstructor public class StudentSheetController {
 private final StudentSheetService service;
 @GetMapping("/export")public ResponseEntity<byte[]> export(@RequestParam(defaultValue="false")boolean template)throws java.io.IOException{return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+(template?"student-template.xlsx":"students.xlsx")+"\"").contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(service.export(template));}
 @PostMapping("/import")public ApiResponse<?> upload(@RequestPart MultipartFile file,@RequestParam(defaultValue="false")boolean apply)throws java.io.IOException{return ApiResponse.ok(service.importSheet(file,apply));}
}
