package com.zhigangzong.controller;
import com.zhigangzong.common.ApiResponse;
import com.zhigangzong.dto.PortalRequests.FileReview;
import com.zhigangzong.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
@RestController @RequestMapping("/api/portal/files") @RequiredArgsConstructor
public class FileController {
    private final FileService service;
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ApiResponse<?> upload(@RequestPart("file")MultipartFile file,@RequestParam String kind,@RequestParam(required=false)Long placementId)throws IOException{return ApiResponse.ok(service.upload(file,kind,placementId));}
    @GetMapping public ApiResponse<?> list(@RequestParam(required=false)Long placementId,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size){return ApiResponse.ok(service.list(placementId,page,size));}
    @GetMapping("/{id}") public ApiResponse<?> detail(@PathVariable long id){return ApiResponse.ok(service.detail(id));}
    @PostMapping("/{id}/review") public ApiResponse<?> review(@PathVariable long id,@Valid @RequestBody FileReview r){return ApiResponse.ok(service.review(id,r));}
    @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@PathVariable long id)throws IOException{return content(id,false);}
    @GetMapping("/{id}/preview") public ResponseEntity<byte[]> preview(@PathVariable long id)throws IOException{return content(id,true);}
    private ResponseEntity<byte[]> content(long id,boolean preview)throws IOException{
        var d=service.download(id,preview);var f=d.file();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.getContentType())).contentLength(d.bytes().length)
            .header(HttpHeaders.CONTENT_DISPOSITION,(preview?ContentDisposition.inline():ContentDisposition.attachment()).filename(f.getOriginalName(),StandardCharsets.UTF_8).build().toString())
            .header(HttpHeaders.CACHE_CONTROL,"no-store, private").header("X-Content-Type-Options","nosniff")
            .header("Content-Security-Policy","sandbox; default-src 'none'; frame-ancestors 'self'")
            .body(d.bytes());
    }
}
