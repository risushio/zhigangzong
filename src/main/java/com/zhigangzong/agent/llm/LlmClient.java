package com.zhigangzong.agent.llm;

/** Provider-independent chat contract. Clients never execute tools or access business storage. */
public interface LlmClient {
    LlmResponse chat(LlmRequest request);
}
