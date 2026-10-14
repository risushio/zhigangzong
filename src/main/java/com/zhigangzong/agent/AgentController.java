package com.zhigangzong.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhigangzong.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/agent") @RequiredArgsConstructor
public class AgentController {
    private final AgentStore store;
    private final StudentDraftService drafts;
    private final AgentOrchestrator orchestrator;
    @GetMapping("/settings") public ApiResponse<?> settings() {return ApiResponse.ok(store.settings());}
    @PostMapping("/sessions") public ApiResponse<?> create() {return ApiResponse.ok(store.createSession());}
    @GetMapping("/sessions/{id}") public ApiResponse<?> session(@PathVariable String id) {return ApiResponse.ok(store.viewSession(id));}
    @GetMapping("/sessions/{id}/messages") public ApiResponse<?> history(@PathVariable String id,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return ApiResponse.ok(store.history(id,page,size));}
    @PostMapping("/sessions/{id}/messages") public ApiResponse<?> message(@PathVariable String id,@RequestBody JsonNode request) {
        AgentJson.keys(request,Set.of("clientMessageId","content"));return ApiResponse.ok(orchestrator.message(id,AgentJson.text(request.get("clientMessageId"),80),AgentJson.text(request.get("content"),10000)));
    }
    @PostMapping("/sessions/{id}/drafts") public ApiResponse<?> newDraft(@PathVariable String id,@RequestBody JsonNode request) {AgentJson.keys(request,Set.of("fields"));return ApiResponse.ok(drafts.create(id,request.get("fields")));}
    @GetMapping("/drafts/{id}") public ApiResponse<?> draft(@PathVariable String id) {return ApiResponse.ok(drafts.get(id));}
    @PatchMapping("/drafts/{id}") public ApiResponse<?> patch(@PathVariable String id,@RequestBody JsonNode request) {
        AgentJson.keys(request,Set.of("expectedVersion","fields"));return ApiResponse.ok(drafts.update(id,AgentJson.version(request.get("expectedVersion")),request.get("fields")));
    }
    @PostMapping("/drafts/{id}/cancel") public ApiResponse<?> cancel(@PathVariable String id,@RequestBody JsonNode request) {AgentJson.keys(request,Set.of("expectedVersion"));return ApiResponse.ok(drafts.cancel(id,AgentJson.version(request.get("expectedVersion"))));}
    @PostMapping("/drafts/{id}/confirm") public ApiResponse<?> confirm(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,@RequestBody JsonNode request) {AgentJson.keys(request,Set.of("expectedVersion"));return ApiResponse.ok(drafts.confirm(id,AgentJson.version(request.get("expectedVersion")),key));}
}
