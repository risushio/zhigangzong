package com.zhigangzong.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", message);
    }

    public static BusinessException notFound(String resource) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", resource + "不存在");
    }

    public static BusinessException notImplemented(String message) {
        return new BusinessException(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", message);
    }
}
