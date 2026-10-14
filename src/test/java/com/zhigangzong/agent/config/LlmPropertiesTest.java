package com.zhigangzong.agent.config;

import com.zhigangzong.agent.AgentProperties;
import com.zhigangzong.agent.llm.*;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class LlmPropertiesTest {
    @Test void ollamaProfileBindsDurationsAndCapabilitiesWithoutDatabase() {
        LlmProfileTestSupport.profile("ollama").run(context->{assertNull(context.getStartupFailure());var p=context.getBean(LlmProperties.class);assertEquals("qwen2.5:3b",p.getModel());assertEquals(Duration.ofSeconds(5),p.getConnectTimeout());assertEquals(Duration.ofSeconds(120),p.getReadTimeout());assertTrue(p.isSupportsJsonMode());assertFalse(p.isSupportsToolCalling());assertFalse(p.isEnabled());assertEquals(LlmProperties.TokenParameter.MAX_TOKENS,p.getTokenParameter());});
    }
    @Test void deepseekProfileAndOverridesBindWithoutRealKey() {
        LlmProfileTestSupport.profile("deepseek").withPropertyValues("app.llm.model=override-cloud-model","app.llm.api-key=test-only").run(context->{assertNull(context.getStartupFailure());var p=context.getBean(LlmProperties.class);assertEquals("override-cloud-model",p.getModel());assertEquals(Duration.ofSeconds(10),p.getConnectTimeout());assertTrue(p.isSupportsToolCalling());assertFalse(p.isSupportsParallelToolCalls());assertEquals("none",p.getReasoningEffort());assertFalse(p.toString().contains("test-only"));});
    }
    @Test void defaultsRetainDisabledLegacyClientAndLegacyKeysStayRedacted() {
        LlmProfileTestSupport.runner().withPropertyValues("app.agent.api-key=legacy-secret").run(context->{assertNull(context.getStartupFailure());assertFalse(context.getBean(AgentProperties.class).toString().contains("legacy-secret"));assertEquals(LlmException.Code.DISABLED,assertThrows(LlmException.class,()->context.getBean(LlmClient.class).chat(LlmRequest.text("fixture"))).getCode());});
    }
    @Test void explicitNewConfigurationNeverBorrowsLegacyCredentials() {
        LlmProfileTestSupport.runner().withPropertyValues("app.agent.model-enabled=true","app.agent.model=legacy","app.agent.api-key=legacy-secret","app.llm.enabled=true","app.llm.endpoint=https://example.com/chat","app.llm.model=new-model").run(context->assertNotNull(context.getStartupFailure()));
        LlmProfileTestSupport.runner().withPropertyValues("app.agent.model-enabled=true","app.agent.model=legacy","app.agent.api-key=legacy-secret","app.llm.enabled=false").run(context->{assertNull(context.getStartupFailure());assertEquals(LlmException.Code.DISABLED,assertThrows(LlmException.class,()->context.getBean(LlmClient.class).chat(LlmRequest.text("fixture"))).getCode());});
    }
    @Test void unknownAdapterAndInvalidTimeoutFailClearly() {
        for(String property:new String[]{"app.llm.provider=unknown","app.llm.read-timeout=0s","app.llm.max-retries=10"})LlmProfileTestSupport.runner().withPropertyValues(property).run(context->assertNotNull(context.getStartupFailure()));
    }
}
