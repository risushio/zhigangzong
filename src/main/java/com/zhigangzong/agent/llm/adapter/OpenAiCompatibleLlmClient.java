package com.zhigangzong.agent.llm.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhigangzong.agent.config.LlmProperties;
import com.zhigangzong.agent.llm.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import static com.zhigangzong.agent.llm.LlmException.Code.*;

/** Bounded Chat Completions transport. No prompt, credential, response body or tool execution logging. */
public final class OpenAiCompatibleLlmClient implements LlmClient {
    private final LlmProperties properties;
    private final HttpClient http;
    public OpenAiCompatibleLlmClient(LlmProperties properties) {
        this.properties=Objects.requireNonNull(properties);validateConfiguration();
        http=HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    private static LlmException error(LlmException.Code code,String message) {return new LlmException(code,message);}
    private static boolean validDuration(Duration value) {return value!=null&&!value.isZero()&&!value.isNegative()&&value.compareTo(Duration.ofMinutes(5))<=0;}
    private void validateConfiguration() {
        if(!"openai-compatible".equals(properties.getProvider())||!validDuration(properties.getConnectTimeout())
            ||!validDuration(properties.getReadTimeout())||!validDuration(properties.getTotalTimeout())
            ||properties.getMaxRetries()<0||properties.getMaxRetries()>2||properties.getRetryDelay()==null
            ||properties.getRetryDelay().isNegative()||properties.getMaxRetryDelay()==null
            ||properties.getMaxRetryDelay().isNegative()||properties.getMaxRetryDelay().compareTo(Duration.ofSeconds(5))>0
            ||properties.getRetryDelay().compareTo(properties.getMaxRetryDelay())>0
            ||properties.getMaxResponseBytes()<1024||properties.getMaxResponseBytes()>1048576
            ||properties.getMaxRequestBytes()<1024||properties.getMaxRequestBytes()>1048576
            ||properties.getTokenParameter()==null||properties.getMaxTokens()==null||properties.getMaxTokens()<1
            ||properties.getMaxTokens()>100000||!validTemperature(properties.getTemperature()))
            throw error(CONFIGURATION,"LLM 参数、期限或重试配置无效");
        if(properties.isEnabled()) {
            endpoint();
            if(properties.getModel()==null||properties.getModel().isBlank()||properties.getModel().length()>200)
                throw error(CONFIGURATION,"LLM 模型名称尚未配置或无效");
            String key=properties.getApiKey();
            if((properties.isRequiresApiKey()&&(key==null||key.isBlank()))||(key!=null&&key.chars().anyMatch(c->c<32||c==127)))
                throw error(CONFIGURATION,"LLM 服务端密钥尚未配置或格式无效");
        }
    }
    private URI endpoint() {
        try {
            URI uri=URI.create(properties.getEndpoint());String host=uri.getHost();
            if(host!=null&&uri.getUserInfo()==null&&uri.getFragment()==null
                &&("https".equalsIgnoreCase(uri.getScheme())||("http".equalsIgnoreCase(uri.getScheme())
                &&Set.of("127.0.0.1","localhost","::1","[::1]").contains(host.toLowerCase(Locale.ROOT)))))return uri;
        }catch(Exception ignored){}
        throw error(CONFIGURATION,"模型地址须为 HTTPS 或本机 HTTP 地址，不允许凭据或片段");
    }
    private static boolean validTemperature(Double value) {return value==null||(Double.isFinite(value)&&value>=0&&value<=2);}
    private void validateRequest(LlmRequest request) {
        if(request==null||request.messages().isEmpty()||request.messages().size()>100||request.tools().size()>64
            ||!validTemperature(request.temperature())||(request.timeout()!=null&&!validDuration(request.timeout()))
            ||(request.model()!=null&&(request.model().isBlank()||request.model().length()>200)))
            throw error(INVALID_REQUEST,"聊天消息、参数或请求期限无效");
        if(request.jsonSchemaHint()!=null&&(!request.jsonSchemaHint().isObject()||request.responseFormat()!=LlmRequest.ResponseFormat.JSON_OBJECT))
            throw error(INVALID_REQUEST,"JSON Schema 提示必须为 JSON 输出模式的对象");
        boolean toolMessages=request.messages().stream().anyMatch(m->m.role()==LlmMessage.Role.TOOL||!m.toolCalls().isEmpty());
        if(request.responseFormat()==LlmRequest.ResponseFormat.JSON_OBJECT&&!properties.isSupportsJsonMode())
            throw error(UNSUPPORTED_CAPABILITY,"当前模型配置不支持必要的 JSON 输出能力");
        if((!request.tools().isEmpty()||request.toolChoice()!=null||toolMessages)&&!properties.isSupportsToolCalling())
            throw error(UNSUPPORTED_CAPABILITY,"当前模型配置不支持必要的工具调用能力");
        var names=new HashSet<String>();
        for(var tool:request.tools()) {
            if(!identifier(tool.name())||!names.add(tool.name())||tool.parameters()==null||!tool.parameters().isObject())
                throw error(INVALID_REQUEST,"工具名称或参数 Schema 无效");
            if(tool.strict()&&!properties.isSupportsStrictTools())throw error(UNSUPPORTED_CAPABILITY,"当前模型配置不支持严格工具 Schema");
        }
        for(var message:request.messages()) {
            if(message.role()==null||(message.role()!=LlmMessage.Role.ASSISTANT&&message.content()==null)
                ||(message.role()==LlmMessage.Role.ASSISTANT&&message.content()==null&&message.toolCalls().isEmpty())
                ||(message.role()==LlmMessage.Role.TOOL&&!identifier(message.toolCallId()))
                ||(message.role()!=LlmMessage.Role.TOOL&&message.toolCallId()!=null)
                ||(message.role()!=LlmMessage.Role.ASSISTANT&&!message.toolCalls().isEmpty()))
                throw error(INVALID_REQUEST,"消息角色、内容或工具结果 ID 无效");
            for(var call:message.toolCalls())if(!identifier(call.id())||!identifier(call.name())||call.arguments()==null||!call.arguments().isObject())
                throw error(INVALID_REQUEST,"历史工具调用格式无效");
        }
        var choice=request.toolChoice();
        if(choice!=null&&(choice.mode()==null||(choice.mode()!=LlmRequest.ToolChoice.Mode.NONE&&request.tools().isEmpty())
            ||(choice.mode()==LlmRequest.ToolChoice.Mode.NAMED&&!names.contains(choice.name()))
            ||(choice.mode()!=LlmRequest.ToolChoice.Mode.NAMED&&choice.name()!=null)))
            throw error(INVALID_REQUEST,"工具调用策略无效");
    }
    private static boolean identifier(String value) {return value!=null&&value.matches("[A-Za-z0-9_-]{1,128}");}
    public static Map<String,Object> toolCallWire(LlmToolCall call) {
        return Map.of("id",call.id(),"type","function","function",Map.of("name",call.name(),"arguments",LlmJson.write(call.arguments())));
    }
    public static Map<String,Object> messageWire(LlmMessage message) {
        var result=new LinkedHashMap<String,Object>();result.put("role",message.role().name().toLowerCase(Locale.ROOT));result.put("content",message.content());
        if(message.toolCallId()!=null)result.put("tool_call_id",message.toolCallId());
        if(!message.toolCalls().isEmpty())result.put("tool_calls",message.toolCalls().stream().map(OpenAiCompatibleLlmClient::toolCallWire).toList());return result;
    }
    private String payload(LlmRequest request,String model) {
        var body=new LinkedHashMap<String,Object>();body.put("model",model);body.put("messages",request.messages().stream().map(OpenAiCompatibleLlmClient::messageWire).toList());body.put("stream",false);
        Double temperature=request.temperature()==null?properties.getTemperature():request.temperature();if(temperature!=null)body.put("temperature",temperature);
        switch(properties.getTokenParameter()) {
            case MAX_TOKENS -> body.put("max_tokens",properties.getMaxTokens());
            case MAX_COMPLETION_TOKENS -> body.put("max_completion_tokens",properties.getMaxTokens());
            case NONE -> {}
        }
        if(properties.getReasoningEffort()!=null&&!properties.getReasoningEffort().isBlank())body.put("reasoning_effort",properties.getReasoningEffort());
        if(request.responseFormat()==LlmRequest.ResponseFormat.JSON_OBJECT) {
            if(request.jsonSchemaHint()!=null&&properties.isSupportsJsonSchema())body.put("response_format",Map.of("type","json_schema","json_schema",Map.of("name","structured_response","schema",request.jsonSchemaHint(),"strict",false)));
            else body.put("response_format",Map.of("type","json_object"));
        }
        if(!request.tools().isEmpty()) {
            body.put("tools",request.tools().stream().map(tool->{var fn=new LinkedHashMap<String,Object>();fn.put("name",tool.name());if(tool.description()!=null)fn.put("description",tool.description());fn.put("parameters",tool.parameters());if(properties.isSupportsStrictTools())fn.put("strict",tool.strict());return Map.of("type","function","function",fn);}).toList());
            if(properties.isSupportsParallelToolCalls())body.put("parallel_tool_calls",false);
        }
        var choice=request.toolChoice();if(choice!=null)body.put("tool_choice",choice.mode()==LlmRequest.ToolChoice.Mode.NAMED?Map.of("type","function","function",Map.of("name",choice.name())):choice.mode().name().toLowerCase(Locale.ROOT));
        String value=LlmJson.write(body);if(value.getBytes(StandardCharsets.UTF_8).length>properties.getMaxRequestBytes())throw error(INVALID_REQUEST,"模型请求超过大小限制");return value;
    }
    @Override public LlmResponse chat(LlmRequest request) {
        if(!properties.isEnabled())throw error(DISABLED,"模型调用尚未启用");validateConfiguration();validateRequest(request);URI uri=endpoint();
        String model=request.model()==null?properties.getModel():request.model();String body=payload(request,model);
        long budget=properties.getTotalTimeout().toNanos();
        if(request.timeout()!=null)budget=Math.min(budget,request.timeout().toNanos());
        long deadline=System.nanoTime()+budget;
        for(int attempt=0;attempt<=properties.getMaxRetries();attempt++) {
            long remaining=deadline-System.nanoTime();if(remaining<=0)throw new LlmException(TIMEOUT,"模型请求总期限已到",null,true);
            long attemptBudget=Math.min(remaining,properties.getReadTimeout().toNanos());
            var builder=HttpRequest.newBuilder(uri).timeout(Duration.ofNanos(attemptBudget)).header("Content-Type","application/json");
            if(properties.getApiKey()!=null&&!properties.getApiKey().isBlank())builder.header("Authorization","Bearer "+properties.getApiKey());
            var future=http.sendAsync(builder.POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),ignored->boundedBody(properties.getMaxResponseBytes()));
            LlmException failure;Duration retryDelay=properties.getRetryDelay().multipliedBy(1L<<attempt);
            try {
                var response=future.get(attemptBudget,TimeUnit.NANOSECONDS);if(response.statusCode()==200)return parse(response.body(),request,model);
                failure=httpFailure(response.statusCode());var retryAfter=response.headers().firstValue("Retry-After");if(retryAfter.isPresent())retryDelay=retryAfter(retryAfter.get(),retryDelay);
            }catch(TimeoutException error){future.cancel(true);failure=new LlmException(TIMEOUT,"模型请求超时",null,true);}
            catch(InterruptedException error){future.cancel(true);Thread.currentThread().interrupt();throw error(INTERRUPTED,"模型请求已中断");}
            catch(ExecutionException error){future.cancel(true);Throwable cause=error.getCause();while(cause!=null&&cause.getCause()!=null&&!(cause instanceof LlmException)&&!(cause instanceof HttpTimeoutException))cause=cause.getCause();
                if(cause instanceof LlmException typed)failure=typed;else if(cause instanceof HttpTimeoutException)failure=new LlmException(TIMEOUT,"模型请求超时",null,true);else failure=new LlmException(CONNECTION,"模型服务连接失败",null,true);
            }
            if(!failure.isRetryable()||attempt==properties.getMaxRetries())throw failure;
            if(retryDelay.compareTo(properties.getMaxRetryDelay())>0||retryDelay.toNanos()>=deadline-System.nanoTime())throw failure;
            try {TimeUnit.NANOSECONDS.sleep(retryDelay.toNanos());}catch(InterruptedException error){Thread.currentThread().interrupt();throw error(INTERRUPTED,"模型重试已中断");}
        }
        throw error(PROVIDER_ERROR,"模型服务暂不可用");
    }
    private static Duration retryAfter(String value,Duration fallback) {
        try {long seconds=Long.parseLong(value.trim());return seconds>=0&&seconds<=86400?Duration.ofSeconds(seconds):Duration.ofDays(1);}
        catch(Exception ignored){try{return Duration.ofMillis(Math.max(0,ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-System.currentTimeMillis()));}catch(Exception invalid){return fallback;}}
    }
    private static LlmException httpFailure(int status) {
        if(status==401||status==403)return new LlmException(AUTHENTICATION,"模型服务认证失败",status,false);
        if(status==404)return new LlmException(MODEL_NOT_FOUND,"模型或模型接口不存在，请检查配置",status,false);
        if(status==429)return new LlmException(RATE_LIMITED,"模型服务限流，请稍后重试",status,true);
        return new LlmException(PROVIDER_ERROR,status>=500?"模型服务暂不可用":"模型服务拒绝了请求参数",status,status>=500&&status<=599);
    }
    private static String optionalText(JsonNode node) {
        if(node.isMissingNode()||node.isNull())return null;if(!node.isTextual())throw error(INVALID_RESPONSE,"模型响应字段类型无效");return node.asText();
    }
    private static Long token(JsonNode node) {
        if(node.isMissingNode()||node.isNull())return null;if(!node.isIntegralNumber()||!node.canConvertToLong()||node.asLong()<0)throw error(INVALID_RESPONSE,"模型使用量统计格式无效");return node.asLong();
    }
    private static LlmResponse parse(String body,LlmRequest request,String requestedModel) {
        if(body==null||body.isBlank())throw error(EMPTY_RESPONSE,"模型返回了空响应");var root=LlmJson.object(body);var choices=root.path("choices");
        if(!choices.isArray()||choices.isEmpty()||!choices.get(0).isObject())throw error(INVALID_RESPONSE,"模型响应 choices 格式无效");
        var choice=choices.get(0);var message=choice.path("message");if(!message.isObject()||!"assistant".equals(message.path("role").asText()))throw error(INVALID_RESPONSE,"模型响应消息角色无效");
        String content=optionalText(message.path("content")),reason=optionalText(choice.path("finish_reason"));var calls=new ArrayList<LlmToolCall>();var raw=message.path("tool_calls");
        if(!raw.isMissingNode()&&!raw.isNull()) {
            if(!raw.isArray()||raw.size()>64)throw error(INVALID_RESPONSE,"模型工具调用格式无效");var ids=new HashSet<String>();var allowed=new HashSet<String>();request.tools().forEach(tool->allowed.add(tool.name()));
            for(var call:raw) {
                var fn=call.path("function");String id=optionalText(call.path("id")),name=optionalText(fn.path("name")),arguments=optionalText(fn.path("arguments"));
                if(!"function".equals(call.path("type").asText())||!identifier(id)||!ids.add(id)||!identifier(name)||!allowed.contains(name)||arguments==null)throw error(INVALID_RESPONSE,"模型返回了未允许或无效的工具调用");
                calls.add(new LlmToolCall(id,name,LlmJson.object(arguments)));
            }
        }
        if((content==null||content.isBlank())&&calls.isEmpty())throw error(EMPTY_RESPONSE,"模型回复内容为空");
        if(request.responseFormat()==LlmRequest.ResponseFormat.JSON_OBJECT&&calls.isEmpty()) {
            if(reason!=null&&!"stop".equals(reason))throw error(INVALID_RESPONSE,"模型 JSON 输出未完整结束");LlmJson.object(content);
        }
        LlmResponse.Usage usage=null;var stats=root.path("usage");if(!stats.isMissingNode()&&!stats.isNull()) {
            if(!stats.isObject())throw error(INVALID_RESPONSE,"模型使用量统计格式无效");usage=new LlmResponse.Usage(token(stats.path("prompt_tokens")),token(stats.path("completion_tokens")),token(stats.path("total_tokens")));
        }
        String model=optionalText(root.path("model"));return new LlmResponse(content,calls,reason,usage,model==null?requestedModel:model);
    }
    private static HttpResponse.BodySubscriber<String> boundedBody(int limit) {
        return new HttpResponse.BodySubscriber<>() {
            final CompletableFuture<String> result=new CompletableFuture<>();final ByteArrayOutputStream bytes=new ByteArrayOutputStream();Flow.Subscription subscription;
            @Override public CompletionStage<String> getBody(){return result;}
            @Override public void onSubscribe(Flow.Subscription value){subscription=value;value.request(1);}
            @Override public void onNext(List<ByteBuffer> chunks){for(var chunk:chunks){if(chunk.remaining()>limit-bytes.size()){subscription.cancel();result.completeExceptionally(error(RESPONSE_TOO_LARGE,"模型响应超过大小限制"));return;}byte[] value=new byte[chunk.remaining()];chunk.get(value);bytes.writeBytes(value);}subscription.request(1);}
            @Override public void onError(Throwable error){result.completeExceptionally(error);}
            @Override public void onComplete(){result.complete(bytes.toString(StandardCharsets.UTF_8));}
        };
    }
}
