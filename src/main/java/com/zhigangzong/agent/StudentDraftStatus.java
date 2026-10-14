package com.zhigangzong.agent;

public enum StudentDraftStatus {
    COLLECTING, READY, CREATED, CANCELLED, EXPIRED;

    static StudentDraftStatus persisted(String value) {
        return switch (value) {
            case "EDITING" -> COLLECTING;
            case "COMMITTED" -> CREATED;
            default -> valueOf(value);
        };
    }

    boolean editable() { return this == COLLECTING || this == READY; }
}
