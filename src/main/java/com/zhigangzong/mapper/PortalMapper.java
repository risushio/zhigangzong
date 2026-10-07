package com.zhigangzong.mapper;

import com.zhigangzong.dto.PortalRequests;
import com.zhigangzong.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.*;

public interface PortalMapper {
    record Actor(long id,long schoolId,Long departmentId,String role,Long enterpriseId) {}
    @Select("SELECT u.id,u.school_id,u.department_id,u.role,m.enterprise_id FROM auth_account a JOIN user_account u ON u.id=a.user_id LEFT JOIN enterprise_member m ON m.user_id=u.id WHERE a.username=#{username} AND a.enabled=TRUE")
    Actor actor(String username);
    @Select("SELECT * FROM user_account WHERE id=#{id} FOR UPDATE") UserAccount lockUser(long id);
    @Insert("INSERT INTO auth_account(user_id,username,password_hash) VALUES(#{userId},#{username},#{hash})")
    int account(@Param("userId") long userId,@Param("username") String username,@Param("hash") String hash);
    @Insert("INSERT INTO enterprise_member(user_id,enterprise_id) VALUES(#{userId},#{enterpriseId})")
    int member(@Param("userId") long userId,@Param("enterpriseId") long enterpriseId);
    @Select("SELECT * FROM student_profile WHERE user_id=#{userId}") StudentProfile student(long userId);
    @Select("SELECT * FROM student_profile WHERE id=#{id} FOR UPDATE") StudentProfile lockStudent(long id);
    @Update("UPDATE student_profile SET major=#{p.major},skills=#{p.skills},project_experience=#{p.projectExperience},resume_ref=#{p.resumeRef},preferred_city=#{p.preferredCity},available_from=#{p.availableFrom},available_to=#{p.availableTo},days_per_week=#{p.daysPerWeek} WHERE user_id=#{userId}")
    int profile(@Param("userId") long userId,@Param("p") PortalRequests.Profile p);

    String JOBS=" FROM job_position j JOIN enterprise e ON e.id=j.enterprise_id WHERE j.review_status='APPROVED' AND j.publish_status='PUBLISHED' AND e.review_status='APPROVED' AND (j.application_deadline IS NULL OR j.application_deadline>=CURRENT_DATE) AND (j.title LIKE CONCAT('%',#{q},'%') OR j.city LIKE CONCAT('%',#{q},'%') OR j.required_major LIKE CONCAT('%',#{q},'%') OR j.required_skills LIKE CONCAT('%',#{q},'%'))";
    @Select("SELECT j.id,j.title,j.description,j.city,j.required_major AS requiredMajor,j.required_skills AS requiredSkills,j.monthly_pay AS monthlyPay,j.start_date AS startDate,j.end_date AS endDate,j.days_per_week AS daysPerWeek,j.application_deadline AS applicationDeadline,e.name AS enterpriseName"+JOBS+" ORDER BY j.id DESC LIMIT #{size} OFFSET #{offset}")
    List<Map<String,Object>> jobs(@Param("q") String q,@Param("size") int size,@Param("offset") int offset);
    @Select("SELECT COUNT(*)"+JOBS) long jobCount(String q);

    String APPS=" FROM job_application a JOIN student_profile s ON s.id=a.student_id JOIN user_account u ON u.id=s.user_id JOIN job_position j ON j.id=a.job_id JOIN enterprise e ON e.id=j.enterprise_id WHERE u.school_id=#{actor.schoolId} <if test=\"actor.role == 'STUDENT'\"> AND u.id=#{actor.id}</if><if test=\"actor.role == 'RECRUITER'\"> AND j.enterprise_id=#{actor.enterpriseId}</if>";
    @Select("<script>SELECT a.id,a.student_id AS studentId,a.job_id AS jobId,a.resume_ref AS resumeRef,a.recruitment_status AS recruitmentStatus,a.interview_at AS interviewAt,a.offer_details AS offerDetails,a.student_confirmed AS studentConfirmed,u.display_name AS studentName,j.title,e.name AS enterpriseName"+APPS+" ORDER BY a.id DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String,Object>> applications(@Param("actor") Actor actor,@Param("size") int size,@Param("offset") int offset);
    @Select("<script>SELECT COUNT(*)"+APPS+"</script>") long applicationCount(@Param("actor") Actor actor);
    @Select("SELECT * FROM job_application WHERE id=#{id}") JobApplication application(long id);
    @Select("SELECT * FROM job_application WHERE id=#{id} FOR UPDATE") JobApplication lockApplication(long id);
    @Insert("INSERT INTO job_application(student_id,job_id,resume_ref) VALUES(#{studentId},#{jobId},#{resumeRef})")
    @Options(useGeneratedKeys=true,keyProperty="id") int apply(JobApplication application);
    @Update("UPDATE job_application SET recruitment_status=#{recruitmentStatus},interview_at=#{interviewAt},offer_details=#{offerDetails},student_confirmed=#{studentConfirmed} WHERE id=#{id}") int transition(JobApplication application);
    @Insert("INSERT INTO recruitment_event(application_id,actor_id,from_status,to_status,note) VALUES(#{id},#{actor},#{from},#{to},#{note})")
    int event(@Param("id") long id,@Param("actor") long actor,@Param("from") String from,@Param("to") String to,@Param("note") String note);
    @Select("SELECT r.id,r.from_status AS fromStatus,r.to_status AS toStatus,r.note,r.created_at AS createdAt,u.display_name AS actorName FROM recruitment_event r LEFT JOIN user_account u ON u.id=r.actor_id WHERE r.application_id=#{id} ORDER BY r.id")
    List<Map<String,Object>> events(long id);
    @Select("SELECT * FROM internship_batch WHERE id=#{id}") InternshipBatch batch(long id);
    @Select("SELECT * FROM internship_batch WHERE department_id=#{departmentId} ORDER BY id DESC") List<InternshipBatch> batches(Long departmentId);
    @Select("SELECT COUNT(*) FROM internship_placement WHERE application_id=#{app} OR (student_id=#{student} AND batch_id=#{batch})")
    int duplicatePlacement(@Param("app") long app,@Param("student") long student,@Param("batch") long batch);
    @Insert("INSERT INTO internship_placement(student_id,batch_id,source,application_id,enterprise_id,job_id,position_title,school_approval_status,start_date,end_date) VALUES(#{studentId},#{batchId},'PLATFORM',#{applicationId},#{enterpriseId},#{jobId},#{positionTitle},'PENDING',#{startDate},#{endDate})")
    @Options(useGeneratedKeys=true,keyProperty="id") int placement(InternshipPlacement placement);
    String PLACES=" FROM internship_placement p JOIN student_profile s ON s.id=p.student_id JOIN user_account u ON u.id=s.user_id JOIN enterprise e ON e.id=p.enterprise_id JOIN internship_batch b ON b.id=p.batch_id WHERE u.school_id=#{actor.schoolId}<if test=\"actor.role == 'STUDENT'\"> AND u.id=#{actor.id}</if><if test=\"actor.role == 'TEACHER'\"> AND p.teacher_id=#{actor.id}</if><if test=\"actor.role == 'ENTERPRISE_MENTOR'\"> AND p.enterprise_mentor_id=#{actor.id} AND p.enterprise_id=#{actor.enterpriseId}</if>";
    @Select("<script>SELECT p.id,p.source,p.student_id AS studentId,p.application_id AS applicationId,p.position_title AS positionTitle,p.school_approval_status AS schoolApprovalStatus,p.arrival_status AS arrivalStatus,p.teacher_id AS teacherId,p.enterprise_mentor_id AS enterpriseMentorId,p.start_date AS startDate,p.end_date AS endDate,u.display_name AS studentName,e.name AS enterpriseName,b.name AS batchName"+PLACES+" ORDER BY p.id DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<Map<String,Object>> placements(@Param("actor") Actor actor,@Param("size") int size,@Param("offset") int offset);
    @Select("<script>SELECT COUNT(*)"+PLACES+"</script>") long placementCount(@Param("actor") Actor actor);
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM internship_placement WHERE id=#{id} FOR UPDATE") InternshipPlacement lockPlacement(long id);
    @Update("UPDATE internship_placement SET school_approval_status=#{decision} WHERE id=#{id}") int approval(@Param("id")long id,@Param("decision")String decision);
    @Insert("INSERT INTO approval_record(placement_id,approver_id,decision,comment) VALUES(#{id},#{actor},#{decision},#{comment})")
    int approvalRecord(@Param("id")long id,@Param("actor")long actor,@Param("decision")String decision,@Param("comment")String comment);
    @Select("SELECT a.decision,a.comment,a.created_at AS createdAt,u.display_name AS approverName FROM approval_record a JOIN user_account u ON u.id=a.approver_id WHERE a.placement_id=#{id} ORDER BY a.id") List<Map<String,Object>> approvals(long id);
    @Update("UPDATE internship_placement SET school_approval_status='PENDING',start_date=#{startDate},end_date=#{endDate} WHERE id=#{id}") int resubmit(InternshipPlacement placement);
    @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) VALUES(#{user},#{title},#{content},#{type},#{id})")
    int notifyUser(@Param("user")long user,@Param("title")String title,@Param("content")String content,@Param("type")String type,@Param("id")long id);
    @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,#{title},#{content},'APPLICATION',#{id} FROM user_account u JOIN enterprise_member m ON m.user_id=u.id JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='RECRUITER' AND m.enterprise_id=#{enterprise} AND a.enabled=TRUE")
    int notifyRecruiters(@Param("school")long school,@Param("enterprise")long enterprise,@Param("id")long id,@Param("title")String title,@Param("content")String content);
    @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,'待办理实习审批','学生已提交实习申请，请在学校审批办理中查看。','PLACEMENT',#{id} FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='SCHOOL_ADMIN' AND a.enabled=TRUE")
    int notifySchool(@Param("school")long school,@Param("id")long id);
    @Select("SELECT id,title,content,read_at AS readAt,created_at AS createdAt FROM notification WHERE recipient_id=#{user} ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<Map<String,Object>> notifications(@Param("user")long user,@Param("size")int size,@Param("offset")int offset);
    @Select("SELECT COUNT(*) FROM notification WHERE recipient_id=#{user}") long notificationCount(long user);
    @Update("UPDATE notification SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE id=#{id} AND recipient_id=#{user}") int read(@Param("id")long id,@Param("user")long user);
}
