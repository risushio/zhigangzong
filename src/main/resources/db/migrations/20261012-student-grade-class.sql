-- V1 年级与班级名称扩展：只增加可空字段，不创建班级字典或 class_id。
-- 执行前需用户确认，并备份/检查目标库；应用启动不会自动执行此脚本。
-- 前置查询须确认 grade_year、class_name 均不存在；已有任一列时停止并核对状态。
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
-- FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='student_profile'
--   AND COLUMN_NAME IN ('grade_year','class_name');
-- 旧库须先完成 20261012-agent-profile.sql，新建库由 schema.sql 直接包含两列。
-- 旧记录两字段保持 NULL，不依据学号或专业推算；不删除/重建历史档案。
ALTER TABLE student_profile
    ADD COLUMN grade_year INT NULL,
    ADD COLUMN class_name VARCHAR(100) NULL;
-- 回退应用时保留新增可空列；不要删除已采集的年级、班级信息。
