-- 手动迁移，仅在完成备份和下列重复检查后执行。不得自动用于生产。
-- 前置检查，结果必须为空：
-- SELECT u.school_id, TRIM(s.student_no) student_no, COUNT(*) n
-- FROM student_profile s JOIN user_account u ON u.id=s.user_id
-- GROUP BY u.school_id, TRIM(s.student_no) HAVING COUNT(*) > 1;
-- 一次执行；MySQL DDL 不支持整体事务回滚。已有字段时停止并核对迁移状态。
ALTER TABLE student_profile
    MODIFY user_id BIGINT NULL,
    ADD school_id BIGINT NULL,
    ADD name VARCHAR(80) NULL,
    ADD department_id BIGINT NULL,
    ADD phone VARCHAR(20) NULL;
UPDATE student_profile s JOIN user_account u ON u.id=s.user_id
SET s.school_id=u.school_id, s.name=u.display_name,
    s.department_id=u.department_id, s.student_no=TRIM(s.student_no);
ALTER TABLE student_profile
    MODIFY school_id BIGINT NOT NULL,
    MODIFY name VARCHAR(80) NOT NULL,
    ADD UNIQUE KEY uk_student_school_no (school_id, student_no),
    ADD CONSTRAINT fk_student_school FOREIGN KEY (school_id) REFERENCES school(id),
    ADD CONSTRAINT fk_student_department_school FOREIGN KEY (department_id, school_id) REFERENCES department(id, school_id);
-- 新增 Agent 表使用同目录 ../agent-tables.sql。
-- 回退：关闭 app.agent.enabled，保留上述兼容字段和已创建学生；不删除学生或改回 user_id NOT NULL。
