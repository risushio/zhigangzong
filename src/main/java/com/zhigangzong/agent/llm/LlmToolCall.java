package com.zhigangzong.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;

public record LlmToolCall(String id,String name,JsonNode arguments) {
    public LlmToolCall {arguments=arguments==null?null:arguments.deepCopy();}
    @Override public JsonNode arguments() {return arguments==null?null:arguments.deepCopy();}
    @Override public String toString() {return "LlmToolCall[id="+id+", name="+name+", arguments=<redacted>]";}
}
