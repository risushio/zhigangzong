package com.zhigangzong.agent.llm;

import lombok.Builder;
import java.time.Duration;
import java.util.List;

@Builder(toBuilder=true)
public record LlmRequest(List<LlmMessage> messages,String model,Double temperature,ResponseFormat responseFormat,
                         List<LlmToolDefinition> tools,ToolChoice toolChoice,Duration timeout) {
    public enum ResponseFormat { TEXT, JSON_OBJECT }
    public record ToolChoice(Mode mode,String name) {
        public enum Mode { AUTO, NONE, REQUIRED, NAMED }
        public static ToolChoice auto() {return new ToolChoice(Mode.AUTO,null);}
        public static ToolChoice named(String name) {return new ToolChoice(Mode.NAMED,name);}
    }
    public LlmRequest {
        messages=messages==null?List.of():List.copyOf(messages);
        tools=tools==null?List.of():List.copyOf(tools);
        responseFormat=responseFormat==null?ResponseFormat.TEXT:responseFormat;
    }
    public static LlmRequest text(String text) {return builder().messages(List.of(LlmMessage.user(text))).build();}
    @Override public String toString() {return "LlmRequest[messages="+messages.size()+", tools="+tools.size()+", format="+responseFormat+"]";}
}
