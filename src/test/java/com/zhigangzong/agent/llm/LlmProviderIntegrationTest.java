package com.zhigangzong.agent.llm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in only; fixed fictional input, no database, no prompt/key output. */
@EnabledIfEnvironmentVariable(named="RUN_LLM_LIVE_TESTS",matches="true")
class LlmProviderIntegrationTest {
    @Test void selectedProviderReturnsCompatibleJsonThroughTheSameJavaInterface() {
        String profile=System.getProperty("live.llm.profile","ollama");var runner=LlmProfileTestSupport.profile(profile).withPropertyValues("app.llm.enabled=true");
        String endpoint=System.getProperty("live.llm.endpoint");if(endpoint!=null)runner=runner.withPropertyValues("app.llm.endpoint="+endpoint);
        runner.run(context->{assertNull(context.getStartupFailure());
            var request=LlmRequest.builder().messages(List.of(LlmMessage.system("Output only a JSON object. Preserve string leading zeros. Do not invent missing values."),LlmMessage.user("虚构样例：编号000123，年份2026。只返回JSON对象，字段code为字符串，year为整数。"))).temperature(0.0).responseFormat(LlmRequest.ResponseFormat.JSON_OBJECT).build();
            var result=context.getBean(LlmClient.class).chat(request);var fields=LlmJson.object(result.content());assertTrue(fields.path("code").isTextual());assertEquals("000123",fields.path("code").asText());assertTrue(fields.path("year").isIntegralNumber());assertEquals(2026,fields.path("year").asInt());assertFalse(result.model().isBlank());
            System.out.println("PASS live LLM profile="+profile+", model="+result.model()+", JSON and leading-zero string verified; no database writes");
        });
    }
}
