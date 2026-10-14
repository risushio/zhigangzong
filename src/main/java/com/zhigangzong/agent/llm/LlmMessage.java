package com.zhigangzong.agent.llm;

import java.util.List;

public record LlmMessage(Role role, String content, String toolCallId, List<LlmToolCall> toolCalls) {
    public enum Role { SYSTEM, USER, ASSISTANT, TOOL }
    public LlmMessage { toolCalls=toolCalls==null?List.of():List.copyOf(toolCalls); }
    public static LlmMessage system(String text) {return new LlmMessage(Role.SYSTEM,text,null,List.of());}
    public static LlmMessage user(String text) {return new LlmMessage(Role.USER,text,null,List.of());}
    public static LlmMessage tool(String id,String result) {return new LlmMessage(Role.TOOL,result,id,List.of());}
    @Override public String toString() {return "LlmMessage[role="+role+", content=<redacted>, toolCalls="+toolCalls.size()+"]";}
}
