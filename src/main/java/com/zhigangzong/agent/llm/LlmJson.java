package com.zhigangzong.agent.llm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

/** Strict protocol JSON, independent of the application's NON_NULL response serialization. */
public final class LlmJson {
    private static final ObjectMapper JSON=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private LlmJson() {}
    public static String write(Object value) {
        try{return JSON.writeValueAsString(value);}catch(Exception error){throw new LlmException(LlmException.Code.INVALID_REQUEST,"模型请求无法序列化");}
    }
    public static JsonNode read(String value) {
        try{var node=JSON.readTree(value);if(node!=null)return node;}catch(Exception ignored){}
        throw new LlmException(LlmException.Code.INVALID_RESPONSE,"模型返回了非法 JSON");
    }
    public static JsonNode object(String value) {
        var node=read(value);if(node.isObject())return node;
        throw new LlmException(LlmException.Code.INVALID_RESPONSE,"模型输出须为 JSON 对象");
    }
    public static JsonNode tree(Object value) {return JSON.valueToTree(value);}
}
