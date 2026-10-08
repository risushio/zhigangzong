package com.zhigangzong.mapper;
import com.zhigangzong.entity.MatchFeedback;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface MatchActionMapper {
 @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,'待办理推荐反馈','学生已提交岗位推荐反馈，请核查并答复。','MATCH_FEEDBACK',#{id} FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='SCHOOL_ADMIN' AND a.enabled=TRUE")int notifySchool(@Param("school")long school,@Param("id")long id);
 @Insert("INSERT INTO job_favorite(student_id,job_id) VALUES(#{student},#{job}) ON DUPLICATE KEY UPDATE id=id")int favorite(@Param("student")long student,@Param("job")long job);
 @Delete("DELETE FROM job_favorite WHERE student_id=#{student} AND job_id=#{job}")int unfavorite(@Param("student")long student,@Param("job")long job);
 @Select("SELECT j.id,j.title,j.city,e.name AS enterpriseName,j.publish_status AS publishStatus,j.review_status AS reviewStatus,j.application_deadline AS applicationDeadline,f.created_at AS createdAt FROM job_favorite f JOIN job_position j ON j.id=f.job_id JOIN enterprise e ON e.id=j.enterprise_id WHERE f.student_id=#{student} ORDER BY f.id DESC LIMIT #{size} OFFSET #{offset}")List<Map<String,Object>> favorites(@Param("student")long student,@Param("size")int size,@Param("offset")int offset);
 @Select("SELECT COUNT(*) FROM job_favorite WHERE student_id=#{student}")long count(long student);
 @Insert("INSERT INTO match_feedback(student_id,job_id,feedback_type,reason) VALUES(#{studentId},#{jobId},#{feedbackType},#{reason})")@Options(useGeneratedKeys=true,keyProperty="id")int feedback(MatchFeedback f);
 String SCOPE=" FROM match_feedback f JOIN student_profile s ON s.id=f.student_id JOIN user_account u ON u.id=s.user_id JOIN job_position j ON j.id=f.job_id LEFT JOIN match_feedback_resolution r ON r.feedback_id=f.id WHERE u.school_id=#{a.schoolId}<if test=\"a.role == 'STUDENT'\"> AND u.id=#{a.id}</if>";
 @Select("<script>SELECT f.id,f.job_id AS jobId,j.title,u.display_name AS studentName,f.feedback_type AS feedbackType,f.reason,f.created_at AS createdAt,r.note AS resolution,r.completed_at AS completedAt"+SCOPE+" ORDER BY f.id DESC LIMIT #{size} OFFSET #{offset}</script>")List<Map<String,Object>> feedbacks(@Param("a")PortalMapper.Actor a,@Param("size")int size,@Param("offset")int offset);
 @Select("<script>SELECT COUNT(*)"+SCOPE+"</script>")long feedbackCount(@Param("a")PortalMapper.Actor a);
 @Select("SELECT f.* FROM match_feedback f WHERE id=#{id} FOR UPDATE")MatchFeedback lock(long id);
 @Select("SELECT COUNT(*) FROM match_feedback_resolution WHERE feedback_id=#{id}")int resolved(long id);
 @Insert("INSERT INTO match_feedback_resolution(feedback_id,actor_id,note) VALUES(#{id},#{actor},#{note})")int resolve(@Param("id")long id,@Param("actor")long actor,@Param("note")String note);
}
