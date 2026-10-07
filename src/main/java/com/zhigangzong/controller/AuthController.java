package com.zhigangzong.controller;

import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.mapper.AuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthMapper mapper;

    @GetMapping("/csrf")
    public ApiResponse<Map<String,String>> csrf(CsrfToken token) {
        return ApiResponse.ok(Map.of("token", token.getToken(), "headerName", token.getHeaderName()));
    }
    @GetMapping("/me")
    public ApiResponse<Map<String,Object>> me(Authentication auth) {
        var account = mapper.find(auth.getName());
        account.remove("passwordHash"); account.remove("enabled");
        return ApiResponse.ok(account);
    }
}
