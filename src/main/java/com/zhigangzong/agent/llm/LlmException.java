package com.zhigangzong.agent.llm;

/** Only safe, stable messages are exposed; provider bodies/headers/prompts are never attached. */
public final class LlmException extends RuntimeException {
    public enum Code { DISABLED, CONFIGURATION, INVALID_REQUEST, UNSUPPORTED_CAPABILITY, CONNECTION,
        AUTHENTICATION, RATE_LIMITED, TIMEOUT, MODEL_NOT_FOUND, PROVIDER_ERROR, EMPTY_RESPONSE,
        INVALID_RESPONSE, RESPONSE_TOO_LARGE, INTERRUPTED }
    private final Code code;
    private final Integer httpStatus;
    private final boolean retryable;
    public LlmException(Code code,String message) {this(code,message,null,false);}
    public LlmException(Code code,String message,Integer httpStatus,boolean retryable) {
        super(message);this.code=code;this.httpStatus=httpStatus;this.retryable=retryable;
    }
    public Code getCode() {return code;}
    public Integer getHttpStatus() {return httpStatus;}
    public boolean isRetryable() {return retryable;}
}
