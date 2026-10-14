package com.zhigangzong.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@Getter @Setter
@ConfigurationProperties(prefix="app.llm",ignoreUnknownFields=false)
public class LlmProperties {
    public enum TokenParameter { NONE, MAX_TOKENS, MAX_COMPLETION_TOKENS }
    private boolean enabled=false;
    private String provider="openai-compatible";
    private String endpoint="";
    private String model="";
    private String apiKey="";
    private boolean requiresApiKey=true;
    private Duration connectTimeout=Duration.ofSeconds(5);
    private Duration readTimeout=Duration.ofSeconds(120);
    private Duration totalTimeout=Duration.ofSeconds(150);
    private boolean supportsJsonMode=false;
    private boolean supportsToolCalling=false;
    private boolean supportsStrictTools=false;
    private boolean supportsParallelToolCalls=false;
    private Double temperature=0.0;
    private Integer maxTokens=2000;
    private TokenParameter tokenParameter=TokenParameter.MAX_TOKENS;
    private String reasoningEffort;
    private int maxRetries=1;
    private Duration retryDelay=Duration.ofMillis(200);
    private Duration maxRetryDelay=Duration.ofSeconds(2);
    private int maxResponseBytes=131072;
    private int maxRequestBytes=131072;
    @Override public String toString() {return "LlmProperties[enabled="+enabled+", provider="+provider+", apiKey=<redacted>]";}
}
