package com.zhigangzong.controller;
import com.zhigangzong.common.*;
import com.zhigangzong.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/catalog") @RequiredArgsConstructor
public class CatalogController {
    private final CatalogService service;
    @GetMapping("/{resource}")
    public ApiResponse<PageResult<Map<String,Object>>> search(@PathVariable String resource,
            @RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status,
            @RequestParam(defaultValue="") String city,@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="20") int size) {
        return ApiResponse.ok(service.search(resource,q,status,city,page,size));
    }
}
