package com.zhigangzong.agent.llm;

import java.util.List;

public record LlmResponse(String content,List<LlmToolCall> toolCalls,String finishReason,Usage usage,String model) {
    public record Usage(Long promptTokens,Long completionTokens,Long totalTokens) {}
    public LlmResponse {toolCalls=toolCalls==null?List.of():List.copyOf(toolCalls);}
    @Override public String toString() {return "LlmResponse[content=<redacted>, toolCalls="+toolCalls.size()+", finishReason="+finishReason+"]";}
}
