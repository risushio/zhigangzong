-- 第一版草稿：仅创建缺失表，不删除数据、不修改既有表。
-- 所有时间为本地开发约定 Asia/Shanghai；业务关联使用外键，无级联删除。
-- 后续字段变更必须提供单独迁移脚本；IF NOT EXISTS 不会升级已有表。

CREATE TABLE IF NOT EXISTS school (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(40) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_school_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS department (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(40) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_department_code (school_id, code),
    UNIQUE KEY uk_department_school (id, school_id),
    FOREIGN KEY (school_id) REFERENCES school(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    school_id BIGINT NOT NULL,
    department_id BIGINT,
    display_name VARCHAR(80) NOT NULL,
    role VARCHAR(32) NOT NULL,
    email VARCHAR(160),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (school_id) REFERENCES school(id),
    FOREIGN KEY (department_id, school_id) REFERENCES department(id, school_id),
    CHECK (role IN ('STUDENT','TEACHER','RECRUITER','ENTERPRISE_MENTOR','DEPARTMENT_ADMIN','SCHOOL_ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    student_no VARCHAR(40) NOT NULL,
    major VARCHAR(100) NOT NULL,
    skills VARCHAR(1000),
    project_experience TEXT,
    resume_ref VARCHAR(500),
    preferred_city VARCHAR(100),
    available_from DATE,
    available_to DATE,
    days_per_week INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_user (user_id),
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    CHECK (days_per_week BETWEEN 1 AND 7),
    CHECK (available_to >= available_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS enterprise (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    credit_code VARCHAR(32) NOT NULL,
    contact_name VARCHAR(80),
    contact_phone VARCHAR(40),
    qualification_ref VARCHAR(500),
    cooperation_notes TEXT,
    inspection_notes TEXT,
    review_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    suspension_reason VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_enterprise_credit (credit_code),
    CHECK (review_status IN ('PENDING','APPROVED','REJECTED','SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_position (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    required_major VARCHAR(100),
    required_skills VARCHAR(1000),
    city VARCHAR(100) NOT NULL,
    start_date DATE,
    end_date DATE,
    days_per_week INT,
    headcount INT NOT NULL,
    monthly_pay DECIMAL(12,2),
    working_hours VARCHAR(120),
    application_deadline DATE,
    review_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    publish_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id),
    CHECK (end_date >= start_date),
    CHECK (days_per_week BETWEEN 1 AND 7),
    CHECK (headcount > 0),
    CHECK (monthly_pay >= 0),
    CHECK (review_status IN ('PENDING','APPROVED','REJECTED')),
    CHECK (publish_status IN ('DRAFT','PUBLISHED','OFFLINE')),
    INDEX idx_job_search (city, review_status, publish_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_favorite (student_id, job_id),
    FOREIGN KEY (student_id) REFERENCES student_profile(id),
    FOREIGN KEY (job_id) REFERENCES job_position(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS job_application (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    resume_ref VARCHAR(500),
    recruitment_status VARCHAR(32) NOT NULL DEFAULT 'APPLIED',
    interview_at DATETIME,
    offer_details TEXT,
    student_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_application (student_id, job_id),
    FOREIGN KEY (student_id) REFERENCES student_profile(id),
    FOREIGN KEY (job_id) REFERENCES job_position(id),
    CHECK (recruitment_status IN ('APPLIED','INTERVIEW','OFFERED','ACCEPTED','REJECTED','WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recruitment_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    actor_id BIGINT,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    note TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES job_application(id),
    FOREIGN KEY (actor_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS internship_batch (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    learning_objectives TEXT,
    task_requirements TEXT,
    material_requirements TEXT,
    grading_criteria TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES department(id),
    CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS internship_placement (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    source VARCHAR(16) NOT NULL,
    application_id BIGINT,
    enterprise_id BIGINT NOT NULL,
    job_id BIGINT,
    position_title VARCHAR(120) NOT NULL,
    school_approval_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    arrival_status VARCHAR(24) NOT NULL DEFAULT 'NOT_ARRIVED',
    teacher_id BIGINT,
    enterprise_mentor_id BIGINT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student_profile(id),
    FOREIGN KEY (batch_id) REFERENCES internship_batch(id),
    FOREIGN KEY (application_id) REFERENCES job_application(id),
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id),
    FOREIGN KEY (job_id) REFERENCES job_position(id),
    FOREIGN KEY (teacher_id) REFERENCES user_account(id),
    FOREIGN KEY (enterprise_mentor_id) REFERENCES user_account(id),
    CHECK (source IN ('PLATFORM','SELF')),
    CHECK (source = 'SELF' OR (application_id IS NOT NULL AND job_id IS NOT NULL)),
    CHECK (school_approval_status IN ('DRAFT','PENDING','APPROVED','RETURNED','REJECTED')),
    CHECK (arrival_status IN ('NOT_ARRIVED','ARRIVED')),
    CHECK (end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    approver_id BIGINT NOT NULL,
    decision VARCHAR(24) NOT NULL,
    comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (approver_id) REFERENCES user_account(id),
    CHECK (decision IN ('APPROVED','RETURNED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS internship_material (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    material_type VARCHAR(40) NOT NULL,
    file_ref VARCHAR(500) NOT NULL,
    review_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    review_comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS placement_process_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    note VARCHAR(480) NOT NULL,
    teacher_id BIGINT,
    enterprise_mentor_id BIGINT,
    arrival_date DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (actor_id) REFERENCES user_account(id),
    FOREIGN KEY (teacher_id) REFERENCES user_account(id),
    FOREIGN KEY (enterprise_mentor_id) REFERENCES user_account(id),
    CHECK (action IN ('MENTORS_ASSIGNED','ARRIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS progress_report (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    content TEXT,
    attachment_ref VARCHAR(500),
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    reviewer_id BIGINT,
    feedback TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (reviewer_id) REFERENCES user_account(id),
    CHECK (period_end >= period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS progress_report_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    content TEXT,
    feedback TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (report_id) REFERENCES progress_report(id),
    FOREIGN KEY (actor_id) REFERENCES user_account(id),
    CHECK (action IN ('DRAFT_SAVED','SUBMITTED','REVIEWED','RETURNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attendance_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    record_type VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    note TEXT,
    reviewer_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (reviewer_id) REFERENCES user_account(id),
    CHECK (record_type IN ('CHECK_IN','LEAVE','MAKE_UP','APPEAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS placement_attendance_policy (
    placement_id BIGINT NOT NULL PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attendance_policy_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL,
    note VARCHAR(480) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (actor_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attendance_record_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(24) NOT NULL,
    attendance_date DATE NOT NULL,
    record_type VARCHAR(24) NOT NULL,
    note VARCHAR(480) NOT NULL,
    feedback VARCHAR(480),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (record_id) REFERENCES attendance_record(id),
    FOREIGN KEY (actor_id) REFERENCES user_account(id),
    CHECK (action IN ('PENDING','APPROVED','RETURNED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS change_request (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    change_type VARCHAR(24) NOT NULL,
    original_snapshot TEXT NOT NULL,
    requested_snapshot TEXT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT,
    review_comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (reviewer_id) REFERENCES user_account(id),
    CHECK (change_type IN ('EXTEND','CHANGE_JOB','CHANGE_ENTERPRISE','TERMINATE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS change_request_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(24) NOT NULL,
    original_snapshot TEXT NOT NULL,
    requested_snapshot TEXT NOT NULL,
    reason VARCHAR(480) NOT NULL,
    review_comment VARCHAR(480),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES change_request(id),
    FOREIGN KEY (actor_id) REFERENCES user_account(id),
    CHECK (action IN ('PENDING','APPROVED','RETURNED','REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS placement_replacement (
    previous_placement_id BIGINT NOT NULL PRIMARY KEY,
    next_placement_id BIGINT NOT NULL UNIQUE,
    request_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (previous_placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (next_placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (request_id) REFERENCES change_request(id),
    CHECK (previous_placement_id <> next_placement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS placement_termination (
    placement_id BIGINT NOT NULL PRIMARY KEY,
    request_id BIGINT NOT NULL UNIQUE,
    terminated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (request_id) REFERENCES change_request(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS guidance_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    mentor_id BIGINT NOT NULL,
    contact_at DATETIME NOT NULL,
    content TEXT NOT NULL,
    next_contact_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (mentor_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_alert (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    placement_id BIGINT,
    alert_type VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    owner_id BIGINT,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
    resolution TEXT,
    resolved_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student_profile(id),
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (owner_id) REFERENCES user_account(id),
    CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_follow_up (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    alert_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    result TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (alert_id) REFERENCES risk_alert(id),
    FOREIGN KEY (actor_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS internship_evaluation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    evaluator_id BIGINT NOT NULL,
    evaluation_type VARCHAR(24) NOT NULL,
    score DECIMAL(5,2),
    comment TEXT,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (evaluator_id) REFERENCES user_account(id),
    CHECK (evaluation_type IN ('STUDENT','TEACHER','ENTERPRISE')),
    CHECK (score BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS archive_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    placement_id BIGINT NOT NULL,
    summary_ref VARCHAR(500),
    appraisal_ref VARCHAR(500),
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT,
    archived_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_archive_placement (placement_id),
    FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
    FOREIGN KEY (reviewer_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    content TEXT,
    business_type VARCHAR(40),
    business_id BIGINT,
    due_at DATETIME,
    read_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS workflow_rule (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    batch_id BIGINT,
    major VARCHAR(100),
    attendance_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    report_frequency_days INT NOT NULL DEFAULT 7,
    approval_steps TEXT,
    material_template TEXT,
    evaluation_weights TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES department(id),
    FOREIGN KEY (batch_id) REFERENCES internship_batch(id),
    CHECK (report_frequency_days > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT,
    action VARCHAR(40) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id BIGINT,
    description VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_id) REFERENCES user_account(id),
    INDEX idx_audit_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS match_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    feedback_type VARCHAR(32) NOT NULL,
    reason TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student_profile(id),
    FOREIGN KEY (job_id) REFERENCES job_position(id),
    CHECK (feedback_type IN ('NOT_INTERESTED','NOT_SUITABLE','TEACHER_ADJUSTMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 登录凭据与业务用户档案分离；不修改已有业务表。
CREATE TABLE IF NOT EXISTS match_feedback_resolution (
 feedback_id BIGINT PRIMARY KEY, actor_id BIGINT NOT NULL, note VARCHAR(480) NOT NULL,
 completed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(feedback_id) REFERENCES match_feedback(id), FOREIGN KEY(actor_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_account (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 招聘账号所属企业；新增表，不覆盖现有业务数据。
CREATE TABLE IF NOT EXISTS enterprise_member (
    user_id BIGINT NOT NULL PRIMARY KEY,
    enterprise_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_account(id),
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 自主申报快照和私有文件；只创建新表，不改写已有业务表。
CREATE TABLE IF NOT EXISTS self_placement_detail (
 placement_id BIGINT PRIMARY KEY,
 enterprise_name VARCHAR(160) NOT NULL, credit_code VARCHAR(32) NOT NULL,
 contact_name VARCHAR(80) NOT NULL, contact_phone VARCHAR(40) NOT NULL,
 address VARCHAR(300) NOT NULL, duties TEXT NOT NULL,
 FOREIGN KEY (placement_id) REFERENCES internship_placement(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS self_placement_event (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, placement_id BIGINT NOT NULL, actor_id BIGINT NOT NULL,
 action VARCHAR(24) NOT NULL, enterprise_id BIGINT NOT NULL, enterprise_name VARCHAR(160) NOT NULL,
 credit_code VARCHAR(32) NOT NULL, contact_name VARCHAR(80) NOT NULL, contact_phone VARCHAR(40) NOT NULL,
 address VARCHAR(300) NOT NULL, duties TEXT NOT NULL, position_title VARCHAR(120) NOT NULL,
 start_date DATE NOT NULL, end_date DATE NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (placement_id) REFERENCES internship_placement(id), FOREIGN KEY (actor_id) REFERENCES user_account(id),
 FOREIGN KEY (enterprise_id) REFERENCES enterprise(id), CHECK (action IN ('DRAFT_SAVED','SUBMITTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS stored_file (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, owner_user_id BIGINT NOT NULL, school_id BIGINT NOT NULL,
 placement_id BIGINT, kind VARCHAR(24) NOT NULL, original_name VARCHAR(180) NOT NULL,
 content_type VARCHAR(120) NOT NULL, storage_key VARCHAR(40) NOT NULL UNIQUE,
 byte_size BIGINT NOT NULL, sha256 CHAR(64) NOT NULL, review_status VARCHAR(24) NOT NULL,
 review_comment VARCHAR(480), created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (owner_user_id) REFERENCES user_account(id), FOREIGN KEY (school_id) REFERENCES school(id),
 FOREIGN KEY (placement_id) REFERENCES internship_placement(id),
 CHECK (kind IN ('RESUME','AGREEMENT','INSURANCE','RESULT','OTHER')),
 CHECK ((kind='RESUME' AND placement_id IS NULL) OR (kind<>'RESUME' AND placement_id IS NOT NULL)),
 CHECK (byte_size BETWEEN 1 AND 10485760), CHECK (review_status IN ('UPLOADED','PENDING','APPROVED','RETURNED')),
 INDEX idx_file_placement (placement_id,id), INDEX idx_file_owner (owner_user_id,kind,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS application_resume (
 application_id BIGINT PRIMARY KEY, file_id BIGINT NOT NULL,
 FOREIGN KEY (application_id) REFERENCES job_application(id), FOREIGN KEY (file_id) REFERENCES stored_file(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS file_review_event (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, file_id BIGINT NOT NULL, actor_id BIGINT NOT NULL,
 decision VARCHAR(24) NOT NULL, comment VARCHAR(480) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (file_id) REFERENCES stored_file(id), FOREIGN KEY (actor_id) REFERENCES user_account(id),
 CHECK (decision IN ('APPROVED','RETURNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_case (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 school_id BIGINT NOT NULL, student_id BIGINT NOT NULL, placement_id BIGINT,
 kind VARCHAR(24) NOT NULL, title VARCHAR(160) NOT NULL, description TEXT NOT NULL,
 owner_id BIGINT, status VARCHAR(24) NOT NULL DEFAULT 'OPEN', resolution TEXT,
 student_confirmed BOOLEAN NOT NULL DEFAULT FALSE, rule_key VARCHAR(160), evidence TEXT,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (school_id) REFERENCES school(id), FOREIGN KEY (student_id) REFERENCES student_profile(id),
 FOREIGN KEY (placement_id) REFERENCES internship_placement(id), FOREIGN KEY (owner_id) REFERENCES user_account(id),
 CHECK (kind IN ('HELP','WARNING')), CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
 INDEX idx_case_scope (school_id,student_id,owner_id), INDEX idx_case_rule (student_id,rule_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS student_case_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, case_id BIGINT NOT NULL, actor_id BIGINT NOT NULL,
 action VARCHAR(32) NOT NULL, note TEXT NOT NULL, snapshot TEXT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (case_id) REFERENCES student_case(id), FOREIGN KEY (actor_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS batch_warning_policy (batch_id BIGINT PRIMARY KEY, settings TEXT NOT NULL, FOREIGN KEY (batch_id) REFERENCES internship_batch(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS batch_warning_policy_event (id BIGINT AUTO_INCREMENT PRIMARY KEY,batch_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,settings TEXT NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(batch_id) REFERENCES internship_batch(id),FOREIGN KEY(actor_id) REFERENCES user_account(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS warning_detection (detection_key VARCHAR(160) PRIMARY KEY,batch_id BIGINT NOT NULL,active BOOLEAN NOT NULL,episode INT NOT NULL,case_id BIGINT NOT NULL,FOREIGN KEY(batch_id) REFERENCES internship_batch(id),FOREIGN KEY(case_id) REFERENCES student_case(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS warning_scan (id BIGINT AUTO_INCREMENT PRIMARY KEY,batch_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,checked_students INT NOT NULL,created_cases INT NOT NULL,settings TEXT NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(batch_id) REFERENCES internship_batch(id),FOREIGN KEY(actor_id) REFERENCES user_account(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS batch_grade_policy(batch_id BIGINT PRIMARY KEY,version INT NOT NULL,settings TEXT NOT NULL,FOREIGN KEY(batch_id) REFERENCES internship_batch(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS batch_grade_policy_event(id BIGINT AUTO_INCREMENT PRIMARY KEY,batch_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,version INT NOT NULL,settings TEXT NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(batch_id) REFERENCES internship_batch(id),FOREIGN KEY(actor_id) REFERENCES user_account(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS evaluation_entry(id BIGINT AUTO_INCREMENT PRIMARY KEY,placement_id BIGINT NOT NULL,evaluator_id BIGINT NOT NULL,type VARCHAR(24) NOT NULL,status VARCHAR(24) NOT NULL,score DECIMAL(5,2) NOT NULL,comment TEXT NOT NULL,revision INT NOT NULL,UNIQUE KEY uk_evaluation_entry(placement_id,type),FOREIGN KEY(placement_id) REFERENCES internship_placement(id),FOREIGN KEY(evaluator_id) REFERENCES user_account(id),CHECK(type IN ('STUDENT','ENTERPRISE','TEACHER')),CHECK(status IN ('DRAFT','SUBMITTED','RETURNED')),CHECK(score BETWEEN 0 AND 100)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS evaluation_entry_event(id BIGINT AUTO_INCREMENT PRIMARY KEY,entry_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,action VARCHAR(32) NOT NULL,note TEXT NOT NULL,snapshot TEXT NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(entry_id) REFERENCES evaluation_entry(id),FOREIGN KEY(actor_id) REFERENCES user_account(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS grade_review_request(id BIGINT AUTO_INCREMENT PRIMARY KEY,placement_id BIGINT NOT NULL,reason TEXT NOT NULL,status VARCHAR(24) NOT NULL,original_snapshot TEXT NOT NULL,requested_snapshot TEXT NOT NULL,reviewer_id BIGINT,review_comment TEXT,FOREIGN KEY(placement_id) REFERENCES internship_placement(id),FOREIGN KEY(reviewer_id) REFERENCES user_account(id),CHECK(status IN ('PENDING','RETURNED','APPROVED','REJECTED'))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS grade_review_event(id BIGINT AUTO_INCREMENT PRIMARY KEY,request_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,action VARCHAR(24) NOT NULL,reason TEXT NOT NULL,comment TEXT,snapshot TEXT NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(request_id) REFERENCES grade_review_request(id),FOREIGN KEY(actor_id) REFERENCES user_account(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS placement_archive(id BIGINT AUTO_INCREMENT PRIMARY KEY,placement_id BIGINT NOT NULL UNIQUE,result_file_id BIGINT NOT NULL,summary TEXT NOT NULL,status VARCHAR(24) NOT NULL,snapshot LONGTEXT NOT NULL,reviewer_id BIGINT,review_comment TEXT,archived_at DATETIME,FOREIGN KEY(placement_id) REFERENCES internship_placement(id),FOREIGN KEY(result_file_id) REFERENCES stored_file(id),FOREIGN KEY(reviewer_id) REFERENCES user_account(id),CHECK(status IN ('PENDING','RETURNED','APPROVED'))) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS placement_archive_event(id BIGINT AUTO_INCREMENT PRIMARY KEY,archive_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,action VARCHAR(24) NOT NULL,summary TEXT NOT NULL,result_file_id BIGINT NOT NULL,comment TEXT,snapshot LONGTEXT NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(archive_id) REFERENCES placement_archive(id),FOREIGN KEY(actor_id) REFERENCES user_account(id),FOREIGN KEY(result_file_id) REFERENCES stored_file(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scoped_workflow_policy(id BIGINT AUTO_INCREMENT PRIMARY KEY,department_id BIGINT NOT NULL,batch_id BIGINT,batch_key BIGINT NOT NULL DEFAULT 0,major VARCHAR(100) NOT NULL DEFAULT '',version INT NOT NULL,settings TEXT NOT NULL,UNIQUE KEY uk_scope_rule(department_id,batch_key,major),FOREIGN KEY(department_id) REFERENCES department(id),FOREIGN KEY(batch_id) REFERENCES internship_batch(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS scoped_workflow_policy_event(id BIGINT AUTO_INCREMENT PRIMARY KEY,policy_id BIGINT NOT NULL,actor_id BIGINT NOT NULL,version INT NOT NULL,settings TEXT NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(policy_id) REFERENCES scoped_workflow_policy(id),FOREIGN KEY(actor_id) REFERENCES user_account(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS deadline_reminder(reminder_key VARCHAR(160) PRIMARY KEY,school_id BIGINT NOT NULL,placement_id BIGINT NOT NULL,notification_id BIGINT NOT NULL UNIQUE,FOREIGN KEY(school_id) REFERENCES school(id),FOREIGN KEY(placement_id) REFERENCES internship_placement(id),FOREIGN KEY(notification_id) REFERENCES notification(id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
