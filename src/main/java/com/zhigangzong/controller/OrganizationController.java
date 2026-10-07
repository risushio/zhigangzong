package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.common.PageResult;
import com.zhigangzong.entity.*;
import com.zhigangzong.service.OrganizationService;
import com.zhigangzong.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService service;

    @GetMapping("/schools")
    public ApiResponse<PageResult<School>> listSchool(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listSchool(page, size));
    }

    @GetMapping("/schools/{id}")
    public ApiResponse<School> getSchool(@PathVariable long id) {
        return ApiResponse.ok(service.getSchool(id));
    }

    @PostMapping("/schools")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<School> createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        return ApiResponse.ok(service.createSchool(request));
    }

    @GetMapping("/departments")
    public ApiResponse<PageResult<Department>> listDepartment(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listDepartment(page, size));
    }

    @GetMapping("/departments/{id}")
    public ApiResponse<Department> getDepartment(@PathVariable long id) {
        return ApiResponse.ok(service.getDepartment(id));
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Department> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.ok(service.createDepartment(request));
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<UserAccount>> listUserAccount(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listUserAccount(page, size));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserAccount> getUserAccount(@PathVariable long id) {
        return ApiResponse.ok(service.getUserAccount(id));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserAccount> createUserAccount(@Valid @RequestBody CreateUserAccountRequest request) {
        return ApiResponse.ok(service.createUserAccount(request));
    }
}
