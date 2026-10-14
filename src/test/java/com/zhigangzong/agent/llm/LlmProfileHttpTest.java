package com.zhigangzong.agent.llm;

import com.sun.net.httpserver.HttpServer;
import com.zhigangzong.agent.llm.adapter.OpenAiCompatibleLlmClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises profile binding and real local HTTP together; no database or cloud credentials. */
class LlmProfileHttpTest {
    HttpServer server;
    final AtomicReference<String> payload=new AtomicReference<>(),authorization=new AtomicReference<>(),response=new AtomicReference<>();
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        response.set(OpenAiCompatibleLlmClientTest.response("{\"code\":\"000123\"}"));
        server.createContext("/chat",exchange->{
            payload.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes=response.get().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
    }
    @AfterEach void stop(){server.stop(0);}
    String endpoint(){return "http://127.0.0.1:"+server.getAddress().getPort()+"/chat";}
    @ParameterizedTest @ValueSource(strings={"ollama","deepseek"})
    void configuredProviderSendsSupportedJsonParameters(String profile) {
        LlmProfileTestSupport.profile(profile).withPropertyValues("app.llm.enabled=true","app.llm.endpoint="+endpoint(),"app.llm.api-key=profile-fixture").run(context->{
            assertNull(context.getStartupFailure());var result=context.getBean(LlmClient.class).chat(LlmRequest.text("Return JSON for a fictional code").toBuilder().responseFormat(LlmRequest.ResponseFormat.JSON_OBJECT).build());
            assertEquals("000123",LlmJson.object(result.content()).path("code").asText());assertEquals("Bearer profile-fixture",authorization.get());
            var wire=LlmJson.object(payload.get());assertTrue(wire.has("max_tokens"));assertFalse(wire.has("max_completion_tokens"));assertFalse(wire.has("tools"));assertFalse(wire.has("parallel_tool_calls"));
            assertEquals("json_object",wire.path("response_format").path("type").asText());
            assertEquals(profile.equals("deepseek")?"deepseek-flash":"qwen2.5:3b",wire.path("model").asText());
            if(profile.equals("deepseek"))assertEquals("none",wire.path("reasoning_effort").asText());else assertFalse(wire.has("reasoning_effort"));
        });
    }
    @Test void deepseekProfileSupportsTypedToolCallsWithoutStrictOrParallelParameters() {
        var call=new LlmToolCall("call_mock","fixture_lookup",LlmJson.object("{\"code\":\"000123\"}"));
        response.set(LlmJson.write(Map.of("choices",List.of(Map.of("finish_reason","tool_calls","message",Map.of("role","assistant","tool_calls",List.of(OpenAiCompatibleLlmClient.toolCallWire(call))))))));
        LlmProfileTestSupport.profile("deepseek").withPropertyValues("app.llm.enabled=true","app.llm.endpoint="+endpoint(),"app.llm.api-key=profile-fixture").run(context->{
            assertNull(context.getStartupFailure());var result=context.getBean(LlmClient.class).chat(LlmRequest.text("fixture").toBuilder().tools(List.of(new LlmToolDefinition("fixture_lookup","fixture",LlmJson.object("{\"type\":\"object\",\"properties\":{}}"),false))).toolChoice(LlmRequest.ToolChoice.named("fixture_lookup")).build());
            assertEquals("000123",result.toolCalls().get(0).arguments().path("code").asText());var wire=LlmJson.object(payload.get());
            assertFalse(wire.has("parallel_tool_calls"));assertFalse(wire.path("tools").get(0).path("function").has("strict"));assertEquals("fixture_lookup",wire.path("tool_choice").path("function").path("name").asText());
        });
    }
    @Test void absentNewConfigurationUsesWholeLegacyConfiguration() {
        LlmProfileTestSupport.runner().withPropertyValues("app.agent.model-enabled=true","app.agent.endpoint="+endpoint(),"app.agent.model=legacy-model","app.agent.api-key=legacy-fixture").run(context->{
            assertNull(context.getStartupFailure());assertNotNull(context.getBean(LlmClient.class).chat(LlmRequest.text("fixture")));
            var wire=LlmJson.object(payload.get());assertEquals("legacy-model",wire.path("model").asText());assertTrue(wire.has("max_completion_tokens"));assertFalse(wire.has("max_tokens"));assertEquals("Bearer legacy-fixture",authorization.get());
        });
    }
}
