package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/portal") @RequiredArgsConstructor public class ReminderController {
 private final ReminderService service;
 @PostMapping("/reminders/scan")public ApiResponse<?> scan(){return ApiResponse.ok(service.scan());}
 @PostMapping("/notifications/{id}/complete")public ApiResponse<?> complete(@PathVariable long id){service.complete(id);return ApiResponse.ok(null);}
}
