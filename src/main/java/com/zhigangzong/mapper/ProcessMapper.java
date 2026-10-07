package com.zhigangzong.mapper;

import com.zhigangzong.entity.ProgressReport;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.*;

public interface ProcessMapper {
    @Select("SELECT * FROM progress_report WHERE id=#{id}") ProgressReport reportsForLookup(long id);
    @Select("SELECT u.id,u.display_name AS name,u.role,m.enterprise_id AS enterpriseId FROM user_account u JOIN auth_account a ON a.user_id=u.id LEFT JOIN enterprise_member m ON m.user_id=u.id WHERE u.school_id=#{school} AND a.enabled=TRUE AND (u.role='TEACHER' OR (u.role='ENTERPRISE_MENTOR' AND m.enterprise_id=#{enterprise})) ORDER BY u.id")
    List<Map<String,Object>> mentors(@Param("school") long school,@Param("enterprise") long enterprise);
    @Update("UPDATE internship_placement SET teacher_id=#{teacher},enterprise_mentor_id=#{mentor} WHERE id=#{id}")
    int assign(@Param("id")long id,@Param("teacher")long teacher,@Param("mentor")long mentor);
    @Update("UPDATE internship_placement SET arrival_status='ARRIVED' WHERE id=#{id}") int arrive(long id);
    @Insert("INSERT INTO placement_process_event(placement_id,actor_id,action,note,teacher_id,enterprise_mentor_id,arrival_date) VALUES(#{id},#{actor},#{action},#{note},#{teacher},#{mentor},#{date})")
    int event(@Param("id")long id,@Param("actor")long actor,@Param("action")String action,@Param("note")String note,@Param("teacher")Long teacher,@Param("mentor")Long mentor,@Param("date")LocalDate date);
    @Select("SELECT h.*,u.display_name AS actorName,t.display_name AS teacherName,m.display_name AS enterpriseMentorName FROM placement_process_event h JOIN user_account u ON u.id=h.actor_id LEFT JOIN user_account t ON t.id=h.teacher_id LEFT JOIN user_account m ON m.id=h.enterprise_mentor_id WHERE h.placement_id=#{id} ORDER BY h.id")
    List<Map<String,Object>> events(long id);
    @Select("SELECT * FROM progress_report WHERE placement_id=#{id} ORDER BY period_start DESC,id DESC") List<ProgressReport> reports(long id);
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM progress_report WHERE id=#{id} FOR UPDATE") ProgressReport report(long id);
    @Insert("INSERT INTO progress_report(placement_id,title,period_start,period_end,content) VALUES(#{placementId},#{title},#{periodStart},#{periodEnd},#{content})")
    @Options(useGeneratedKeys=true,keyProperty="id") int create(ProgressReport r);
    @Update("UPDATE progress_report SET title=#{title},period_start=#{periodStart},period_end=#{periodEnd},content=#{content},status=#{status},reviewer_id=#{reviewerId},feedback=#{feedback} WHERE id=#{id}") int save(ProgressReport r);
    @Insert("INSERT INTO progress_report_event(report_id,actor_id,action,title,period_start,period_end,content,feedback) VALUES(#{r.id},#{actor},#{action},#{r.title},#{r.periodStart},#{r.periodEnd},#{r.content},#{r.feedback})")
    int reportEvent(@Param("r")ProgressReport r,@Param("actor")long actor,@Param("action")String action);
    @Select("SELECT h.*,u.display_name AS actorName FROM progress_report_event h JOIN user_account u ON u.id=h.actor_id WHERE report_id=#{id} ORDER BY h.id") List<Map<String,Object>> reportEvents(long id);
}
