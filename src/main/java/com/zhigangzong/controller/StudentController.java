package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.StudentService;
import com.zhigangzong.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService service;

    @GetMapping("/students")
    public ApiResponse<PageResult<StudentProfile>> listStudentProfile(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listStudentProfile(page, size));
    }

    @GetMapping("/students/{id}")
    public ApiResponse<StudentProfile> getStudentProfile(@PathVariable long id) {
        return ApiResponse.ok(service.getStudentProfile(id));
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentProfile> createStudentProfile(@Valid @RequestBody CreateStudentProfileRequest request) {
        return ApiResponse.ok(service.createStudentProfile(request));
    }
}
