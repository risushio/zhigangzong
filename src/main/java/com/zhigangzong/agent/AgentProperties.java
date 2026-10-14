package com.zhigangzong.agent;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data @Component @Validated @ConfigurationProperties(prefix = "app.agent")
public class AgentProperties {
    private boolean enabled = false;
    private boolean modelEnabled = false;
    public enum ProcessingMode { EXTRACTION, TOOLS }
    @NotNull private ProcessingMode processingMode = ProcessingMode.EXTRACTION;
    private boolean cleanupEnabled = true;
    private String endpoint = "https://api.openai.com/v1/chat/completions";
    @ToString.Exclude private String apiKey = "";
    private String model = "";
    @Min(1) @Max(10) private int maxToolCalls = 5;
    @Min(100) @Max(10000) private int maxInputLength = 4000;
    @Min(1000) @Max(100000) private int maxContextLength = 16000;
    @Min(1) @Max(120) private int requestTimeoutSeconds = 45;
    @Min(1) @Max(60) private int modelTimeoutSeconds = 12;
    @Min(1) @Max(3600) private int toolTimeoutSeconds = 5;
    @Min(1) @Max(100) private int requestsPerMinute = 12;
    @Min(1) @Max(168) private int draftTtlHours = 24;
    @Min(1) @Max(365) private int retentionDays = 7;
}
