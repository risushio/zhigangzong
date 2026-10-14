CREATE TABLE IF NOT EXISTS agent_session (
    id VARCHAR(36) PRIMARY KEY,
    school_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    active_draft_id VARCHAR(36),
    processing_message_id VARCHAR(36),
    processing_until DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    INDEX idx_agent_owner (school_id, owner_id, updated_at),
    FOREIGN KEY (school_id) REFERENCES school(id),
    FOREIGN KEY (owner_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS agent_message (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    client_message_id VARCHAR(80) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    content TEXT,
    processing_status VARCHAR(16) NOT NULL,
    response_payload MEDIUMTEXT,
    trace_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_agent_message (session_id, client_message_id),
    FOREIGN KEY (session_id) REFERENCES agent_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS agent_student_draft (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    school_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    version INT NOT NULL,
    fields_json TEXT NOT NULL,
    sources_json TEXT NOT NULL,
    student_id BIGINT,
    confirmed_by BIGINT,
    confirmed_at DATETIME(6),
    confirmed_version INT,
    confirmed_payload_hash CHAR(64),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    INDEX idx_agent_draft_owner (school_id, owner_id, status),
    FOREIGN KEY (session_id) REFERENCES agent_session(id),
    FOREIGN KEY (student_id) REFERENCES student_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS agent_action_request (
    id VARCHAR(36) PRIMARY KEY,
    school_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    draft_id VARCHAR(36) NOT NULL,
    draft_version INT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    student_id BIGINT NOT NULL,
    result_json TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_agent_action (school_id, actor_id, action_type, idempotency_key),
    FOREIGN KEY (draft_id) REFERENCES agent_student_draft(id),
    FOREIGN KEY (student_id) REFERENCES student_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS agent_tool_call (
    message_id VARCHAR(36) NOT NULL,
    call_id VARCHAR(80) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    result_json TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (message_id, call_id),
    FOREIGN KEY (message_id) REFERENCES agent_message(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
