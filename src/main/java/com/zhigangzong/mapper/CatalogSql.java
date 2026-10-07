package com.zhigangzong.mapper;

import com.zhigangzong.exception.BusinessException;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

/** 标识符来自固定白名单；用户搜索内容始终通过 MyBatis 参数绑定。 */
public class CatalogSql {
    record Resource(String table, String columns, String joins, String search, String status) {}
    private static final Map<String, Resource> RESOURCES = Map.ofEntries(
            Map.entry("schools", new Resource("school", "t.id AS id, t.name AS name, t.code AS code, t.created_at AS createdAt", "", "t.name,t.code", "")),
            Map.entry("departments", new Resource("department", "t.id AS id, t.school_id AS schoolId, t.name AS name, t.code AS code, t.created_at AS createdAt", "", "t.name,t.code", "")),
            Map.entry("users", new Resource("user_account", "t.id AS id, t.school_id AS schoolId, t.department_id AS departmentId, t.display_name AS displayName, t.role AS role, t.email AS email, t.created_at AS createdAt", "", "t.display_name,t.email", "role")),
            Map.entry("students", new Resource("student_profile", "t.id AS id, t.user_id AS userId, t.student_no AS studentNo, t.major AS major, t.skills AS skills, t.project_experience AS projectExperience, t.resume_ref AS resumeRef, t.preferred_city AS preferredCity, t.available_from AS availableFrom, t.available_to AS availableTo, t.days_per_week AS daysPerWeek, t.created_at AS createdAt, u.display_name AS studentName", " LEFT JOIN user_account u ON u.id=t.user_id", "t.student_no,t.major,t.skills,u.display_name", "")),
            Map.entry("enterprises", new Resource("enterprise", "t.id AS id, t.name AS name, t.credit_code AS creditCode, t.contact_name AS contactName, t.contact_phone AS contactPhone, t.qualification_ref AS qualificationRef, t.cooperation_notes AS cooperationNotes, t.inspection_notes AS inspectionNotes, t.review_status AS reviewStatus, t.suspension_reason AS suspensionReason, t.created_at AS createdAt", "", "t.name,t.credit_code,t.contact_name", "review_status")),
            Map.entry("jobs", new Resource("job_position", "t.id AS id, t.enterprise_id AS enterpriseId, t.title AS title, t.description AS description, t.required_major AS requiredMajor, t.required_skills AS requiredSkills, t.city AS city, t.start_date AS startDate, t.end_date AS endDate, t.days_per_week AS daysPerWeek, t.headcount AS headcount, t.monthly_pay AS monthlyPay, t.working_hours AS workingHours, t.application_deadline AS applicationDeadline, t.review_status AS reviewStatus, t.publish_status AS publishStatus, t.created_at AS createdAt, e.name AS enterpriseName", " LEFT JOIN enterprise e ON e.id=t.enterprise_id", "t.title,t.required_major,t.required_skills,t.city,e.name", "review_status")),
            Map.entry("job-favorites", new Resource("job_favorite", "t.id AS id, t.student_id AS studentId, t.job_id AS jobId, t.created_at AS createdAt", "", "", "")),
            Map.entry("applications", new Resource("job_application", "t.id AS id, t.student_id AS studentId, t.job_id AS jobId, t.resume_ref AS resumeRef, t.recruitment_status AS recruitmentStatus, t.interview_at AS interviewAt, t.offer_details AS offerDetails, t.student_confirmed AS studentConfirmed, t.created_at AS createdAt", "", "", "recruitment_status")),
            Map.entry("recruitment-events", new Resource("recruitment_event", "t.id AS id, t.application_id AS applicationId, t.actor_id AS actorId, t.from_status AS fromStatus, t.to_status AS toStatus, t.note AS note, t.created_at AS createdAt", "", "t.note", "to_status")),
            Map.entry("batches", new Resource("internship_batch", "t.id AS id, t.department_id AS departmentId, t.name AS name, t.start_date AS startDate, t.end_date AS endDate, t.learning_objectives AS learningObjectives, t.task_requirements AS taskRequirements, t.material_requirements AS materialRequirements, t.grading_criteria AS gradingCriteria, t.created_at AS createdAt", "", "t.name,t.learning_objectives", "")),
            Map.entry("placements", new Resource("internship_placement", "t.id AS id, t.student_id AS studentId, t.batch_id AS batchId, t.source AS source, t.application_id AS applicationId, t.enterprise_id AS enterpriseId, t.job_id AS jobId, t.position_title AS positionTitle, t.school_approval_status AS schoolApprovalStatus, t.arrival_status AS arrivalStatus, t.teacher_id AS teacherId, t.enterprise_mentor_id AS enterpriseMentorId, t.start_date AS startDate, t.end_date AS endDate, t.created_at AS createdAt, u.display_name AS studentName, e.name AS enterpriseName", " LEFT JOIN student_profile s ON s.id=t.student_id LEFT JOIN user_account u ON u.id=s.user_id LEFT JOIN enterprise e ON e.id=t.enterprise_id", "t.position_title,u.display_name,e.name", "school_approval_status")),
            Map.entry("approvals", new Resource("approval_record", "t.id AS id, t.placement_id AS placementId, t.approver_id AS approverId, t.decision AS decision, t.comment AS comment, t.created_at AS createdAt", "", "t.comment", "decision")),
            Map.entry("materials", new Resource("internship_material", "t.id AS id, t.placement_id AS placementId, t.material_type AS materialType, t.file_ref AS fileRef, t.review_status AS reviewStatus, t.review_comment AS reviewComment, t.created_at AS createdAt", "", "t.material_type", "review_status")),
            Map.entry("reports", new Resource("progress_report", "t.id AS id, t.placement_id AS placementId, t.title AS title, t.period_start AS periodStart, t.period_end AS periodEnd, t.content AS content, t.attachment_ref AS attachmentRef, t.status AS status, t.reviewer_id AS reviewerId, t.feedback AS feedback, t.created_at AS createdAt", "", "t.title,t.content", "status")),
            Map.entry("attendance", new Resource("attendance_record", "t.id AS id, t.placement_id AS placementId, t.attendance_date AS attendanceDate, t.record_type AS recordType, t.status AS status, t.note AS note, t.reviewer_id AS reviewerId, t.created_at AS createdAt", "", "t.note", "status")),
            Map.entry("changes", new Resource("change_request", "t.id AS id, t.placement_id AS placementId, t.change_type AS changeType, t.original_snapshot AS originalSnapshot, t.requested_snapshot AS requestedSnapshot, t.reason AS reason, t.status AS status, t.reviewer_id AS reviewerId, t.review_comment AS reviewComment, t.created_at AS createdAt", "", "t.reason", "status")),
            Map.entry("guidance", new Resource("guidance_record", "t.id AS id, t.placement_id AS placementId, t.mentor_id AS mentorId, t.contact_at AS contactAt, t.content AS content, t.next_contact_at AS nextContactAt, t.created_at AS createdAt", "", "t.content", "")),
            Map.entry("alerts", new Resource("risk_alert", "t.id AS id, t.student_id AS studentId, t.placement_id AS placementId, t.alert_type AS alertType, t.description AS description, t.owner_id AS ownerId, t.status AS status, t.resolution AS resolution, t.resolved_at AS resolvedAt, t.created_at AS createdAt", "", "t.description,t.alert_type", "status")),
            Map.entry("alert-follow-ups", new Resource("risk_follow_up", "t.id AS id, t.alert_id AS alertId, t.actor_id AS actorId, t.content AS content, t.result AS result, t.created_at AS createdAt", "", "t.content,t.result", "")),
            Map.entry("evaluations", new Resource("internship_evaluation", "t.id AS id, t.placement_id AS placementId, t.evaluator_id AS evaluatorId, t.evaluation_type AS evaluationType, t.score AS score, t.comment AS comment, t.status AS status, t.created_at AS createdAt", "", "t.comment", "status")),
            Map.entry("archives", new Resource("archive_record", "t.id AS id, t.placement_id AS placementId, t.summary_ref AS summaryRef, t.appraisal_ref AS appraisalRef, t.status AS status, t.reviewer_id AS reviewerId, t.archived_at AS archivedAt, t.created_at AS createdAt", "", "", "status")),
            Map.entry("notifications", new Resource("notification", "t.id AS id, t.recipient_id AS recipientId, t.title AS title, t.content AS content, t.business_type AS businessType, t.business_id AS businessId, t.due_at AS dueAt, t.read_at AS readAt, t.completed_at AS completedAt, t.created_at AS createdAt", "", "t.title,t.content", "")),
            Map.entry("workflow-rules", new Resource("workflow_rule", "t.id AS id, t.department_id AS departmentId, t.batch_id AS batchId, t.major AS major, t.attendance_enabled AS attendanceEnabled, t.report_frequency_days AS reportFrequencyDays, t.approval_steps AS approvalSteps, t.material_template AS materialTemplate, t.evaluation_weights AS evaluationWeights, t.created_at AS createdAt", "", "t.major", "")),
            Map.entry("audit-logs", new Resource("audit_log", "t.id AS id, t.actor_id AS actorId, t.action AS action, t.resource_type AS resourceType, t.resource_id AS resourceId, t.description AS description, t.created_at AS createdAt", "", "t.description,t.action,t.resource_type", "")),
            Map.entry("match-feedback", new Resource("match_feedback", "t.id AS id, t.student_id AS studentId, t.job_id AS jobId, t.feedback_type AS feedbackType, t.reason AS reason, t.created_at AS createdAt", "", "t.reason", "feedback_type"))
    );
    public static void validate(String resource) {
        if (!RESOURCES.containsKey(resource)) throw BusinessException.notFound("数据模块");
    }
    private static Resource resource(Map<String,Object> params) {
        String key = (String) params.get("resource");
        validate(key);
        return RESOURCES.get(key);
    }
    private static String where(Resource r, Map<String,Object> p) {
        String sql = " WHERE 1=1";
        if (p.get("q") != null && !p.get("q").toString().isBlank()) {
            var columns = r.search().isEmpty() ? new String[]{"CAST(t.id AS CHAR)"} : r.search().split(",");
            sql += " AND (" + Arrays.stream(columns).map(c -> c + " LIKE CONCAT('%', #{q}, '%')")
                    .collect(Collectors.joining(" OR ")) + ")";
        }
        if (p.get("status") != null && !p.get("status").toString().isBlank()) {
            if (r.status().isEmpty()) throw BusinessException.badRequest("此模块没有状态筛选");
            sql += " AND t." + r.status() + "=#{status}";
        }
        if (p.get("city") != null && !p.get("city").toString().isBlank()) {
            if (!"job_position".equals(r.table())) throw BusinessException.badRequest("此模块没有城市筛选");
            sql += " AND t.city LIKE CONCAT('%', #{city}, '%')";
        }
        return sql;
    }
    public String page(Map<String,Object> p) {
        var r = resource(p);
        return "SELECT " + r.columns() + " FROM " + r.table() + " t" + r.joins() + where(r,p)
                + " ORDER BY t.id DESC LIMIT #{size} OFFSET #{offset}";
    }
    public String count(Map<String,Object> p) {
        var r = resource(p);
        return "SELECT COUNT(*) FROM " + r.table() + " t" + r.joins() + where(r,p);
    }
}
