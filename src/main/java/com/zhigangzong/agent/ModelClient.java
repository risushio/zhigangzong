package com.zhigangzong.agent;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface ModelClient {
    JsonNode complete(List<Map<String,Object>> messages,List<Map<String,Object>> tools,Duration remaining);
}
