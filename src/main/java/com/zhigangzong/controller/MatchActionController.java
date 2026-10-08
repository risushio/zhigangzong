package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.MatchRequests.*;
import com.zhigangzong.service.MatchActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/portal") public class MatchActionController {
 private final MatchActionService service;
 @GetMapping("/favorites")public ApiResponse<?> favorites(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(service.favorites(page,size));}
 @PutMapping("/favorites/{id}")public ApiResponse<?> favorite(@PathVariable long id){service.favorite(id,true);return ApiResponse.ok(null);}
 @DeleteMapping("/favorites/{id}")public ApiResponse<?> unfavorite(@PathVariable long id){service.favorite(id,false);return ApiResponse.ok(null);}
 @GetMapping("/feedbacks")public ApiResponse<?> feedbacks(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(service.feedbacks(page,size));}
 @PostMapping("/feedbacks")public ApiResponse<?> feedback(@Valid @RequestBody Feedback r){return ApiResponse.ok(service.feedback(r));}
 @PostMapping("/feedbacks/{id}/handle")public ApiResponse<?> handle(@PathVariable long id,@Valid @RequestBody Handle r){service.handle(id,r);return ApiResponse.ok(null);}
}
