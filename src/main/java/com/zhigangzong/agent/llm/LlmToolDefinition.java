package com.zhigangzong.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;

public record LlmToolDefinition(String name,String description,JsonNode parameters,boolean strict) {
    public LlmToolDefinition {parameters=parameters==null?null:parameters.deepCopy();}
    @Override public JsonNode parameters() {return parameters==null?null:parameters.deepCopy();}
    @Override public String toString() {return "LlmToolDefinition[name="+name+", schema=<redacted>]";}
}
