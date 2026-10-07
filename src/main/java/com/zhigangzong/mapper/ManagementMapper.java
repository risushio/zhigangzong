package com.zhigangzong.mapper;
import com.zhigangzong.entity.*;
import org.apache.ibatis.annotations.*;
public interface ManagementMapper {
    @Select("SELECT (SELECT COUNT(*) FROM job_application WHERE job_id=#{id}) + (SELECT COUNT(*) FROM internship_placement WHERE job_id=#{id})")
    long jobReferences(long id);
    @Select("SELECT * FROM enterprise WHERE id=#{id} FOR UPDATE") Enterprise lockEnterprise(long id);
    @Select("SELECT * FROM job_position WHERE id=#{id} FOR UPDATE") JobPosition lockJob(long id);
    @Update("UPDATE enterprise SET name=#{name},credit_code=#{creditCode},contact_name=#{contactName},contact_phone=#{contactPhone},qualification_ref=#{qualificationRef},cooperation_notes=#{cooperationNotes},inspection_notes=#{inspectionNotes},review_status='PENDING',suspension_reason=NULL WHERE id=#{id}")
    int updateEnterprise(Enterprise entity);
    @Update("UPDATE job_position SET enterprise_id=#{enterpriseId},title=#{title},description=#{description},required_major=#{requiredMajor},required_skills=#{requiredSkills},city=#{city},start_date=#{startDate},end_date=#{endDate},days_per_week=#{daysPerWeek},headcount=#{headcount},monthly_pay=#{monthlyPay},working_hours=#{workingHours},application_deadline=#{applicationDeadline},review_status='PENDING',publish_status='DRAFT' WHERE id=#{id}")
    int updateJob(JobPosition entity);
    @Update("UPDATE enterprise SET review_status=#{decision},suspension_reason=#{note} WHERE id=#{id}")
    int reviewEnterprise(@Param("id") long id,@Param("decision") String decision,@Param("note") String note);
    @Update("UPDATE job_position SET review_status=#{decision},publish_status='DRAFT' WHERE id=#{id}")
    int reviewJob(@Param("id") long id,@Param("decision") String decision);
    @Update("UPDATE job_position SET publish_status=#{status} WHERE id=#{id}")
    int publish(@Param("id") long id,@Param("status") String status);
    @Update("UPDATE job_position SET publish_status='OFFLINE',review_status='PENDING' WHERE enterprise_id=#{id}")
    int offlineEnterpriseJobs(long id);
    @Insert("INSERT INTO audit_log(actor_id,action,resource_type,resource_id,description) VALUES(#{actorId},#{action},#{resource},#{id},#{note})")
    int audit(@Param("actorId") Long actorId,@Param("action") String action,@Param("resource") String resource,@Param("id") long id,@Param("note") String note);
}
