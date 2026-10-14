package com.zhigangzong.agent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhigangzong.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

final class AgentJson {
    static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Shanghai")); }
    static String id() { return UUID.randomUUID().toString(); }
    static String write(Object value) {
        try { return JSON.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("Agent JSON serialization failed"); }
    }
    static ObjectNode object(String value) {
        try {
            var node=JSON.readTree(value);
            if (node instanceof ObjectNode object) return object;
        } catch (Exception ignored) {}
        throw BusinessException.badRequest("工具参数必须为合法 JSON 对象");
    }
    static JsonNode read(String value) {
        try { return JSON.readTree(value); }
        catch (Exception e) { throw new IllegalStateException("Invalid persisted Agent data"); }
    }
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    static void keys(JsonNode node,Set<String> allowed) {
        if(node==null||!node.isObject()) throw BusinessException.badRequest("参数必须为对象");
        node.fieldNames().forEachRemaining(k->{if(!allowed.contains(k)) throw BusinessException.badRequest("存在未允许的字段");});
    }
    static int version(JsonNode node) {
        if(node==null||!node.isIntegralNumber()||!node.canConvertToInt()||node.asInt()<1)
            throw BusinessException.badRequest("expectedVersion 必须为正整数");
        return node.asInt();
    }
    static String text(JsonNode node,int max) {
        if(node==null||!node.isTextual()||node.asText().isBlank()||node.asText().length()>max)
            throw BusinessException.badRequest("文字参数缺失或超过长度限制");
        return node.asText().trim();
    }
}
