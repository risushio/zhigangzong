package com.zhigangzong.agent;

import lombok.Data;
import java.time.LocalDateTime;

public final class AgentRecords {
    private AgentRecords() {}
    @Data public static class Session {
        private String id, activeDraftId, processingMessageId;
        private Long schoolId, ownerId;
        private LocalDateTime processingUntil, createdAt, updatedAt, expiresAt;
    }
    @Data public static class Message {
        private String id, sessionId, clientMessageId, requestHash, content, processingStatus, responsePayload, traceId;
        private LocalDateTime createdAt, updatedAt;
    }
    @Data public static class Draft {
        private String id, sessionId, status, fieldsJson, sourcesJson, confirmedPayloadHash;
        private Long schoolId, ownerId, studentId, confirmedBy;
        private Integer version, confirmedVersion;
        private LocalDateTime confirmedAt, createdAt, updatedAt, expiresAt;
    }
    @Data public static class Action {
        private String id, actionType, idempotencyKey, draftId, requestHash, resultJson;
        private Long schoolId, actorId, studentId;
        private Integer draftVersion;
        private LocalDateTime createdAt;
    }
    @Data public static class ToolCall {
        private String messageId, callId, requestHash, resultJson;
        private LocalDateTime createdAt;
    }
}
