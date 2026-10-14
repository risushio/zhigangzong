package com.zhigangzong.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class AgentCleanup {
    private final AgentProperties properties;
    private final AgentMapper mapper;
    @Scheduled(initialDelay=60000,fixedDelay=3600000)
    public void cleanup() {
        if(!properties.isEnabled()||!properties.isCleanupEnabled()) return;
        mapper.expireProcessing();
        mapper.cleanToolCalls(properties.getRetentionDays());
        mapper.redactMessages(properties.getRetentionDays());
        mapper.redactDrafts(properties.getRetentionDays());
    }
}
