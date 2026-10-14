package com.zhigangzong.agent.llm;

import com.sun.net.httpserver.HttpServer;
import com.zhigangzong.agent.config.LlmProperties;
import com.zhigangzong.agent.llm.adapter.OpenAiCompatibleLlmClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static com.zhigangzong.agent.llm.LlmException.Code.*;

class OpenAiCompatibleLlmClientTest {
    HttpServer server;ExecutorService executor;
    final Queue<Reply> replies=new ConcurrentLinkedQueue<>();final Queue<String> requests=new ConcurrentLinkedQueue<>();
    final AtomicInteger count=new AtomicInteger();LlmProperties properties;
    record Reply(int status,String body,long delay,String retryAfter) {}
    static String response(String content) {return LlmJson.write(Map.of("model","reported-model","choices",List.of(Map.of("finish_reason","stop","message",Map.of("role","assistant","content",content)))));}
    @BeforeEach void setup() throws Exception {
        executor=Executors.newCachedThreadPool();server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(executor);
        server.createContext("/chat",exchange->{
            count.incrementAndGet();requests.add(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            assertEquals("Bearer test-private-key",exchange.getRequestHeaders().getFirst("Authorization"));
            var reply=replies.poll();if(reply==null)reply=new Reply(200,response("ok"),0,null);
            try {if(reply.delay()>0)Thread.sleep(reply.delay());if(reply.retryAfter()!=null)exchange.getResponseHeaders().set("Retry-After",reply.retryAfter());byte[] bytes=reply.body().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(reply.status(),bytes.length);exchange.getResponseBody().write(bytes);}catch(Exception ignored){}finally{exchange.close();}
        });server.start();properties=new LlmProperties();properties.setEnabled(true);properties.setModel("configured-model");properties.setApiKey("test-private-key");properties.setEndpoint("http://127.0.0.1:"+server.getAddress().getPort()+"/chat");properties.setMaxRetries(0);
    }
    @AfterEach void cleanup(){server.stop(0);executor.shutdownNow();}
    LlmClient client(){return new OpenAiCompatibleLlmClient(properties);}
    void reply(int status,String body){replies.add(new Reply(status,body,0,null));}
    LlmToolDefinition tool(){return new LlmToolDefinition("lookup","Only a fixture",LlmJson.object("{\"type\":\"object\",\"properties\":{}}"),false);}

    @Test void mapsContentModelUsageAndRequestOverrides() {
        var envelope=LlmJson.object(response("正常回复"));((com.fasterxml.jackson.databind.node.ObjectNode)envelope).set("usage",LlmJson.tree(Map.of("prompt_tokens",5,"completion_tokens",4,"total_tokens",9)));reply(200,LlmJson.write(envelope));
        var result=client().chat(LlmRequest.text("fictional prompt").toBuilder().model("override-model").temperature(0.2).build());
        assertEquals("正常回复",result.content());assertEquals("reported-model",result.model());assertEquals("stop",result.finishReason());assertEquals(9L,result.usage().totalTokens());
        var request=LlmJson.object(requests.remove());assertEquals("override-model",request.path("model").asText());assertEquals(0.2,request.path("temperature").asDouble());
        assertEquals("user",request.path("messages").get(0).path("role").asText());assertFalse(request.has("tools"));assertFalse(request.has("response_format"));assertTrue(request.has("max_tokens"));assertFalse(request.has("max_completion_tokens"));assertFalse(request.has("parallel_tool_calls"));
    }
    @Test void jsonModeValidatesContentAndPreservesStringZeros() {
        properties.setSupportsJsonMode(true);reply(200,response("{\"code\":\"000123\",\"year\":2026}"));
        var result=client().chat(LlmRequest.text("Output JSON only").toBuilder().responseFormat(LlmRequest.ResponseFormat.JSON_OBJECT).build());
        assertEquals("000123",LlmJson.object(result.content()).path("code").asText());assertNull(result.usage());
        assertEquals("json_object",LlmJson.object(requests.remove()).path("response_format").path("type").asText());
    }
    @Test void necessaryUnsupportedCapabilitiesFailBeforeAnyHttpRequest() {
        var client=client();assertEquals(UNSUPPORTED_CAPABILITY,assertThrows(LlmException.class,()->client.chat(LlmRequest.text("JSON").toBuilder().responseFormat(LlmRequest.ResponseFormat.JSON_OBJECT).build())).getCode());
        assertEquals(UNSUPPORTED_CAPABILITY,assertThrows(LlmException.class,()->client.chat(LlmRequest.text("tool").toBuilder().tools(List.of(tool())).build())).getCode());
        assertEquals(0,count.get());properties.setSupportsToolCalling(true);
        assertEquals(UNSUPPORTED_CAPABILITY,assertThrows(LlmException.class,()->client().chat(LlmRequest.text("tool").toBuilder().tools(List.of(new LlmToolDefinition("lookup","fixture",tool().parameters(),true))).build())).getCode());assertEquals(0,count.get());
    }
    @Test void toolCallsAndToolResultHistoryAreStandardizedWithoutExecutingAnything() {
        properties.setSupportsToolCalling(true);
        var call=new LlmToolCall("call_1","lookup",LlmJson.object("{\"number\":\"000123\"}"));
        reply(200,LlmJson.write(Map.of("choices",List.of(Map.of("finish_reason","tool_calls","message",Map.of("role","assistant","tool_calls",List.of(OpenAiCompatibleLlmClient.toolCallWire(call)),"reasoning_content","private-hidden-reasoning"))))));
        var result=client().chat(LlmRequest.builder().messages(List.of(LlmMessage.user("fixture"),new LlmMessage(LlmMessage.Role.ASSISTANT,null,null,List.of(call)),LlmMessage.tool("call_1","{\"ok\":true}"))).tools(List.of(tool())).toolChoice(LlmRequest.ToolChoice.named("lookup")).build());
        assertNull(result.content());assertEquals("000123",result.toolCalls().get(0).arguments().path("number").asText());
        var request=LlmJson.object(requests.remove());assertTrue(request.path("messages").get(1).path("content").isNull());assertEquals("call_1",request.path("messages").get(2).path("tool_call_id").asText());assertEquals("lookup",request.path("tool_choice").path("function").path("name").asText());assertFalse(request.path("tools").get(0).path("function").has("strict"));assertFalse(request.has("parallel_tool_calls"));assertFalse(result.toString().contains("000123"));assertFalse(result.toString().contains("private-hidden"));
    }
    @ParameterizedTest @CsvSource({"401,AUTHENTICATION","403,AUTHENTICATION","404,MODEL_NOT_FOUND","429,RATE_LIMITED","500,PROVIDER_ERROR","400,PROVIDER_ERROR","302,PROVIDER_ERROR"})
    void mapsHttpErrorsWithoutProviderBodyOrKey(int status,String code) {
        reply(status,"PRIVATE_PROVIDER_BODY test-private-key");var error=assertThrows(LlmException.class,()->client().chat(LlmRequest.text("PRIVATE_PROMPT")));
        assertEquals(LlmException.Code.valueOf(code),error.getCode());assertEquals(status,error.getHttpStatus());assertNull(error.getCause());assertFalse(error.toString().contains("PRIVATE_"));assertFalse(error.toString().contains("test-private-key"));assertEquals(1,count.get());
    }
    @Test void transientFailureRetriesOnceAndReturnsSuccessfulResponse() {
        properties.setMaxRetries(1);replies.add(new Reply(429,"limited",0,"0"));reply(200,response("recovered"));
        assertEquals("recovered",client().chat(LlmRequest.text("fixture")).content());assertEquals(2,count.get());
    }
    @Test void boundedRetriesStopAndRespectLongRetryAfter() {
        properties.setMaxRetries(1);properties.setRetryDelay(Duration.ZERO);reply(500,"failed");reply(500,"failed");reply(200,response("must not reach"));
        assertEquals(PROVIDER_ERROR,assertThrows(LlmException.class,()->client().chat(LlmRequest.text("fixture"))).getCode());assertEquals(2,count.get());
        replies.clear();count.set(0);replies.add(new Reply(429,"limited",0,"120"));assertEquals(RATE_LIMITED,assertThrows(LlmException.class,()->client().chat(LlmRequest.text("fixture"))).getCode());assertEquals(1,count.get());
    }
    @Test void readTimeoutAndRetriesStayWithinTotalDeadline() {
        properties.setReadTimeout(Duration.ofMillis(80));properties.setTotalTimeout(Duration.ofMillis(180));properties.setRetryDelay(Duration.ZERO);properties.setMaxRetries(1);
        replies.add(new Reply(200,response("late"),500,null));replies.add(new Reply(200,response("late"),500,null));long start=System.nanoTime();
        assertEquals(TIMEOUT,assertThrows(LlmException.class,()->client().chat(LlmRequest.text("fixture"))).getCode());assertTrue(Duration.ofNanos(System.nanoTime()-start).compareTo(Duration.ofSeconds(2))<0);assertTrue(count.get()<=2);
    }
    @Test void connectionFailureIsRecoverableAndBounded() throws Exception {
        int port;try(var socket=new java.net.ServerSocket(0)){port=socket.getLocalPort();}properties.setEndpoint("http://127.0.0.1:"+port+"/chat");properties.setMaxRetries(1);properties.setRetryDelay(Duration.ZERO);
        var error=assertThrows(LlmException.class,()->client().chat(LlmRequest.text("fixture").toBuilder().timeout(Duration.ofSeconds(1)).build()));assertEquals(CONNECTION,error.getCode());assertTrue(error.isRetryable());
    }
    @Test void requestCannotExtendConfiguredTotalDeadline() {
        properties.setTotalTimeout(Duration.ofMillis(100));properties.setReadTimeout(Duration.ofSeconds(2));
        replies.add(new Reply(200,response("late"),1000,null));long start=System.nanoTime();
        assertEquals(TIMEOUT,assertThrows(LlmException.class,()->client().chat(LlmRequest.text("fixture").toBuilder().timeout(Duration.ofSeconds(3)).build())).getCode());
        assertTrue(Duration.ofNanos(System.nanoTime()-start).compareTo(Duration.ofMillis(800))<0);
    }
    @Test void interruptionCancelsRequestAndPreservesInterruptFlag() throws Exception {
        replies.add(new Reply(200,response("late"),3000,null));var client=client();
        var result=new CompletableFuture<LlmException>();var flag=new java.util.concurrent.atomic.AtomicBoolean();
        Thread caller=new Thread(()->{try{client.chat(LlmRequest.text("fixture"));result.completeExceptionally(new AssertionError("Expected interruption"));}catch(LlmException error){flag.set(Thread.currentThread().isInterrupted());result.complete(error);}});
        try {
            caller.start();long deadline=System.nanoTime()+Duration.ofSeconds(2).toNanos();
            while(count.get()==0&&System.nanoTime()<deadline)Thread.sleep(5);
            assertEquals(1,count.get());caller.interrupt();assertEquals(INTERRUPTED,result.get(2,TimeUnit.SECONDS).getCode());assertTrue(flag.get());
        }finally{caller.interrupt();caller.join(2000);}
    }
    @Test void responseAndRequestLimitsAreEnforcedWithoutRetry() {
        properties.setMaxResponseBytes(1024);properties.setMaxRetries(1);reply(200,"X".repeat(2048));assertEquals(RESPONSE_TOO_LARGE,assertThrows(LlmException.class,()->client().chat(LlmRequest.text("fixture"))).getCode());assertEquals(1,count.get());
        properties.setMaxRequestBytes(1024);assertEquals(INVALID_REQUEST,assertThrows(LlmException.class,()->client().chat(LlmRequest.text("PRIVATE".repeat(300)))).getCode());assertEquals(1,count.get());
    }
    @ParameterizedTest @ValueSource(strings={"", "not-json", "{}", "{\"choices\":[]}","{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\" \"}}]}","{\"choices\":[{\"message\":{\"role\":\"user\",\"content\":\"x\"}}]}","{\"choices\":[],\"choices\":[]}","{} {}"})
    void rejectsEmptyOrMalformedEnvelope(String body) {
        reply(200,body);assertThrows(LlmException.class,()->client().chat(LlmRequest.text("fixture")));assertEquals(1,count.get());
    }
    @Test void rejectsInvalidTruncatedJsonAndUnknownOrMalformedToolCalls() {
        properties.setSupportsJsonMode(true);reply(200,response("not-json"));var json=LlmRequest.text("JSON").toBuilder().responseFormat(LlmRequest.ResponseFormat.JSON_OBJECT).build();assertEquals(INVALID_RESPONSE,assertThrows(LlmException.class,()->client().chat(json)).getCode());
        var root=LlmJson.object(response("{}"));((com.fasterxml.jackson.databind.node.ObjectNode)root.path("choices").get(0)).put("finish_reason","length");reply(200,LlmJson.write(root));assertThrows(LlmException.class,()->client().chat(json));
        properties.setSupportsToolCalling(true);var toolRequest=LlmRequest.text("fixture").toBuilder().tools(List.of(tool())).build();
        for(String name:List.of("unknown","lookup")){reply(200,LlmJson.write(Map.of("choices",List.of(Map.of("message",Map.of("role","assistant","tool_calls",List.of(Map.of("id","call_1","type","function","function",Map.of("name",name,"arguments","not-json")))))))));assertThrows(LlmException.class,()->client().chat(toolRequest));}
    }
    @Test void invalidConfigurationAndRequestAreSafeAndMakeNoNetworkCalls() {
        for(String endpoint:List.of("http://example.com/chat","file:///private","https://user:password@example.com/chat","https://example.com/chat#private")) {properties.setEndpoint(endpoint);assertEquals(CONFIGURATION,assertThrows(LlmException.class,this::client).getCode());}
        properties.setEndpoint("http://127.0.0.1:"+server.getAddress().getPort()+"/chat");properties.setApiKey("PRIVATE_KEY\nINJECTION");assertThrows(LlmException.class,this::client);assertFalse(properties.toString().contains("PRIVATE_KEY"));
        properties.setApiKey("test-private-key");assertEquals(INVALID_REQUEST,assertThrows(LlmException.class,()->client().chat(LlmRequest.text("fixture").toBuilder().temperature(Double.NaN).build())).getCode());
        properties.setEnabled(false);assertEquals(DISABLED,assertThrows(LlmException.class,()->client().chat(LlmRequest.text("fixture"))).getCode());assertEquals(0,count.get());
    }
}
