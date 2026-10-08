package com.zhigangzong.mapper;
import org.apache.ibatis.annotations.*;
import java.util.*;
import java.time.LocalDateTime;
public interface WarningMapper {
 @Select("SELECT b.id,b.name FROM internship_batch b JOIN department d ON d.id=b.department_id WHERE d.school_id=#{school} ORDER BY b.id DESC") List<Map<String,Object>> batches(long school);
 @Select("SELECT school_id FROM department WHERE id=#{id}") long departmentSchool(long id);
 @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
 @Select("SELECT settings FROM batch_warning_policy WHERE batch_id=#{id} FOR UPDATE") String policy(long id);
 @Insert("INSERT INTO batch_warning_policy(batch_id,settings) VALUES(#{id},#{settings}) ON DUPLICATE KEY UPDATE settings=VALUES(settings)") int save(@Param("id")long id,@Param("settings")String settings);
 @Insert("INSERT INTO batch_warning_policy_event(batch_id,actor_id,settings) VALUES(#{id},#{actor},#{settings})") int policyEvent(@Param("id")long id,@Param("actor")long actor,@Param("settings")String settings);
 @Select("SELECT e.*,u.display_name AS actorName FROM batch_warning_policy_event e JOIN user_account u ON u.id=e.actor_id WHERE batch_id=#{id} ORDER BY e.id") List<Map<String,Object>> events(long id);
 @Select("SELECT s.id FROM student_profile s JOIN user_account u ON u.id=s.user_id JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.department_id=#{department} AND u.role='STUDENT' AND a.enabled=TRUE ORDER BY s.id") List<Long> students(@Param("school")long school,@Param("department")long department);
 @Select("SELECT id FROM internship_placement WHERE student_id=#{student} AND batch_id=#{batch} ORDER BY id DESC") List<Long> placements(@Param("student")long student,@Param("batch")long batch);
 @Select("SELECT COUNT(*) FROM stored_file f WHERE f.placement_id=#{id} AND f.kind=#{kind} AND f.review_status='APPROVED' AND f.id=(SELECT MAX(id) FROM stored_file WHERE placement_id=#{id} AND kind=#{kind})") int material(@Param("id")long id,@Param("kind")String kind);
 @Select("SELECT * FROM progress_report WHERE placement_id=#{id} AND status IN ('SUBMITTED','REVIEWED')") List<com.zhigangzong.entity.ProgressReport> reports(long id);
 @Select("SELECT MAX(contact_at) FROM guidance_record WHERE placement_id=#{id}") LocalDateTime lastContact(long id);
 @Insert("INSERT INTO guidance_record(placement_id,mentor_id,contact_at,content) VALUES(#{id},#{actor},#{time},#{note})") int contact(@Param("id")long id,@Param("actor")long actor,@Param("time")LocalDateTime time,@Param("note")String note);
 @Select("SELECT g.*,u.display_name AS actorName FROM guidance_record g JOIN user_account u ON u.id=g.mentor_id WHERE placement_id=#{id} ORDER BY g.id") List<Map<String,Object>> contacts(long id);
 @Select("SELECT * FROM warning_detection WHERE batch_id=#{id} FOR UPDATE") List<Map<String,Object>> detections(long id);
 @Insert("INSERT INTO warning_detection(detection_key,batch_id,active,episode,case_id) VALUES(#{key},#{batch},TRUE,#{episode},#{caseId}) ON DUPLICATE KEY UPDATE active=TRUE,episode=VALUES(episode),case_id=VALUES(case_id)") int detected(@Param("key")String key,@Param("batch")long batch,@Param("episode")int episode,@Param("caseId")long caseId);
 @Update("UPDATE warning_detection SET active=FALSE WHERE detection_key=#{key}") int clear(String key);
 @Insert("INSERT INTO warning_scan(batch_id,actor_id,checked_students,created_cases,settings) VALUES(#{id},#{actor},#{students},#{cases},#{settings})") int scanEvent(@Param("id")long id,@Param("actor")long actor,@Param("students")int students,@Param("cases")int cases,@Param("settings")String settings);
 @Select("SELECT s.*,u.display_name AS actorName FROM warning_scan s JOIN user_account u ON u.id=s.actor_id WHERE batch_id=#{id} ORDER BY s.id") List<Map<String,Object>> scans(long id);
}
