package com.zhigangzong.agent;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

/** Student draft business entry point; reuses existing persistence and confirmation transactions. */
@Service
@RequiredArgsConstructor
public class StudentDraftService {
    private final AgentStore store;

    public Map<String, Object> create(String sessionId, JsonNode fields) {
        return store.patch(sessionId, null, null, fields);
    }

    public Map<String, Object> update(String draftId, int expectedVersion, JsonNode fields) {
        String sessionId = (String) store.getDraft(draftId).get("sessionId");
        return store.patch(sessionId, draftId, expectedVersion, fields);
    }

    public Map<String, Object> get(String draftId) { return store.getDraft(draftId); }
    /** Applies validated fields under the existing message, scope and version locks. No model dependency. */
    public Map<String, Object> applyMessage(AgentRecords.Message message,String draftId,Integer expectedVersion,
                                          JsonNode fields,String departmentName) {
        return store.applyMessage(message,draftId,expectedVersion,fields,departmentName);
    }
    public Map<String, Object> cancel(String draftId, int expectedVersion) { return store.cancel(draftId, expectedVersion); }
    public Map<String, Object> confirm(String draftId, int expectedVersion, String idempotencyKey) {
        return store.confirm(draftId, expectedVersion, idempotencyKey);
    }
}
