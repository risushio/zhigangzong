package com.zhigangzong.agent.config;

import com.zhigangzong.agent.AgentProperties;
import com.zhigangzong.agent.llm.*;
import com.zhigangzong.agent.llm.adapter.OpenAiCompatibleLlmClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import java.time.Duration;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {
    @Bean
    LlmClient llmClient(LlmProperties properties,AgentProperties legacy,Environment environment) {
        // Any bound new configuration selects it as a whole; never fill a missing new key from old credentials.
        var effective=Binder.get(environment).bind("app.llm",LlmProperties.class).isBound()?properties:legacyProperties(legacy);
        if(!"openai-compatible".equals(effective.getProvider()))
            throw new LlmException(LlmException.Code.CONFIGURATION,"LLM 协议适配器配置不支持");
        return new OpenAiCompatibleLlmClient(effective);
    }
    public static LlmProperties legacyProperties(AgentProperties legacy) {
        var result=new LlmProperties();result.setEnabled(legacy.isModelEnabled());result.setEndpoint(legacy.getEndpoint());
        result.setModel(legacy.getModel());result.setApiKey(legacy.getApiKey());result.setTemperature(null);
        result.setReadTimeout(Duration.ofSeconds(legacy.getModelTimeoutSeconds()));
        result.setTotalTimeout(Duration.ofSeconds(legacy.getRequestTimeoutSeconds()));
        result.setSupportsToolCalling(true);result.setSupportsStrictTools(true);result.setSupportsParallelToolCalls(true);
        result.setTokenParameter(LlmProperties.TokenParameter.MAX_COMPLETION_TOKENS);
        result.setMaxRetries(0);return result;
    }
}
