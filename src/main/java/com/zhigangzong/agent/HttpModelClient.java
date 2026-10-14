package com.zhigangzong.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.zhigangzong.agent.config.LlmConfiguration;
import com.zhigangzong.agent.llm.*;
import com.zhigangzong.agent.llm.adapter.OpenAiCompatibleLlmClient;
import com.zhigangzong.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.*;

/** Compatibility bridge for the existing Agent; only LlmClient performs HTTP. */
@Component
public class HttpModelClient implements ModelClient {
    private final AgentProperties properties;
    private final LlmClient client;
    @Autowired public HttpModelClient(AgentProperties properties,LlmClient client){this.properties=properties;this.client=client;}
    /** Keeps the old standalone construction contract for protocol tests. */
    public HttpModelClient(AgentProperties properties){this(properties,request->new OpenAiCompatibleLlmClient(LlmConfiguration.legacyProperties(properties)).chat(request));}
    @Override public JsonNode complete(List<Map<String,Object>> messages,List<Map<String,Object>> tools,Duration remaining) {
        if(!properties.isModelEnabled())throw unavailable("MODEL_UNAVAILABLE","模型尚未配置，可在草稿表单中手动填写");
        try {
            var request=LlmRequest.builder().messages(messages.stream().map(HttpModelClient::message).toList())
                .tools(tools.stream().map(HttpModelClient::tool).toList()).timeout(remaining).build();
            var response=client.chat(request);var result=new LinkedHashMap<String,Object>();result.put("role","assistant");result.put("content",response.content());
            if(!response.toolCalls().isEmpty())result.put("tool_calls",response.toolCalls().stream().map(OpenAiCompatibleLlmClient::toolCallWire).toList());return LlmJson.tree(result);
        }catch(LlmException error){throw unavailable(switch(error.getCode()) {
            case TIMEOUT -> "MODEL_TIMEOUT";case CONFIGURATION -> "MODEL_CONFIG";case AUTHENTICATION -> "MODEL_AUTHENTICATION";
            case RATE_LIMITED -> "MODEL_RATE_LIMITED";case MODEL_NOT_FOUND -> "MODEL_NOT_FOUND";
            case UNSUPPORTED_CAPABILITY -> "MODEL_CAPABILITY";case EMPTY_RESPONSE,INVALID_RESPONSE,RESPONSE_TOO_LARGE,INVALID_REQUEST -> "MODEL_INVALID";
            default -> "MODEL_UNAVAILABLE";
        },error.getMessage());}
        catch(IllegalArgumentException error){throw unavailable("MODEL_INVALID","模型请求格式无效");}
    }
    private static String text(JsonNode node) {if(node.isMissingNode()||node.isNull())return null;if(!node.isTextual())throw new IllegalArgumentException();return node.asText();}
    private static LlmMessage message(Map<String,Object> source) {
        var node=LlmJson.tree(source);var role=LlmMessage.Role.valueOf(node.path("role").asText().toUpperCase(Locale.ROOT));var calls=new ArrayList<LlmToolCall>();
        if(node.hasNonNull("tool_calls"))for(var call:node.get("tool_calls")){var fn=call.path("function");calls.add(new LlmToolCall(text(call.path("id")),text(fn.path("name")),LlmJson.object(text(fn.path("arguments")))));}
        return new LlmMessage(role,text(node.path("content")),text(node.path("tool_call_id")),calls);
    }
    private static LlmToolDefinition tool(Map<String,Object> source) {
        var fn=LlmJson.tree(source).path("function");return new LlmToolDefinition(text(fn.path("name")),text(fn.path("description")),fn.get("parameters"),fn.path("strict").asBoolean(false));
    }
    static BusinessException unavailable(String code,String message){return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,code,message);}
}
