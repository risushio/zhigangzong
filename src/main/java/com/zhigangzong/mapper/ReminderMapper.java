package com.zhigangzong.mapper;
import com.zhigangzong.entity.InternshipPlacement;
import org.apache.ibatis.annotations.*;
import java.time.*;
import java.util.*;
public interface ReminderMapper {
 @Select("SELECT p.* FROM internship_placement p JOIN student_profile s ON s.id=p.student_id JOIN user_account u ON u.id=s.user_id LEFT JOIN placement_termination t ON t.placement_id=p.id LEFT JOIN placement_replacement r ON r.previous_placement_id=p.id LEFT JOIN placement_archive ar ON ar.placement_id=p.id WHERE u.school_id=#{school} AND p.school_approval_status='APPROVED' AND t.placement_id IS NULL AND r.previous_placement_id IS NULL AND (ar.status IS NULL OR ar.status<>'APPROVED') ORDER BY p.id")List<InternshipPlacement> placements(long school);
 @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id,due_at) VALUES(#{user},#{title},#{content},'DEADLINE',#{placement},#{due})")@Options(useGeneratedKeys=true,keyProperty="row.id")int notification(@Param("row")Map<String,Object> row,@Param("user")long user,@Param("title")String title,@Param("content")String content,@Param("placement")long placement,@Param("due")LocalDateTime due);
 @Select("SELECT notification_id FROM deadline_reminder WHERE reminder_key=#{key}")Long exists(String key);
 @Insert("INSERT INTO deadline_reminder(reminder_key,school_id,placement_id,notification_id) VALUES(#{key},#{school},#{placement},#{notification})")int link(@Param("key")String key,@Param("school")long school,@Param("placement")long placement,@Param("notification")long notification);
 @Select("SELECT r.reminder_key,r.notification_id FROM deadline_reminder r WHERE school_id=#{school}")List<Map<String,Object>> reminders(long school);
 @Update("UPDATE notification SET completed_at=COALESCE(completed_at,CURRENT_TIMESTAMP) WHERE id=#{id}")int resolved(long id);
 @Update("UPDATE notification SET completed_at=COALESCE(completed_at,CURRENT_TIMESTAMP) WHERE id=#{id} AND recipient_id=#{user}")int complete(@Param("id")long id,@Param("user")long user);
 @Select("SELECT id FROM school ORDER BY id")List<Long> schools();
 @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
 @Select("SELECT id FROM school WHERE id=#{id} FOR UPDATE")Long lockSchool(long id);
}
