package com.zhigangzong.mapper;

import com.zhigangzong.entity.AttendanceRecord;
import org.apache.ibatis.annotations.*;
import java.util.*;
import java.time.LocalDate;

public interface AttendanceWorkflowMapper {
    @Select("SELECT enabled FROM placement_attendance_policy WHERE placement_id=#{id} FOR UPDATE") Boolean enabled(long id);
    @Insert("INSERT INTO placement_attendance_policy(placement_id,enabled) VALUES(#{id},#{enabled}) ON DUPLICATE KEY UPDATE enabled=#{enabled}")
    int policy(@Param("id") long id,@Param("enabled") boolean enabled);
    @Insert("INSERT INTO attendance_policy_event(placement_id,actor_id,enabled,note) VALUES(#{id},#{actor},#{enabled},#{note})")
    int policyEvent(@Param("id") long id,@Param("actor") long actor,@Param("enabled") boolean enabled,@Param("note") String note);
    @Select("SELECT h.*,u.display_name AS actorName FROM attendance_policy_event h JOIN user_account u ON u.id=h.actor_id WHERE placement_id=#{id} ORDER BY h.id")
    List<Map<String,Object>> policyEvents(long id);
    @Select("SELECT * FROM attendance_record WHERE placement_id=#{id} ORDER BY attendance_date DESC,id DESC") List<AttendanceRecord> records(long id);
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM attendance_record WHERE id=#{id}") AttendanceRecord lookup(long id);
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM attendance_record WHERE id=#{id} FOR UPDATE") AttendanceRecord lock(long id);
    @Select("SELECT id FROM attendance_record WHERE placement_id=#{placement} AND attendance_date=#{date} AND id<>#{except} AND status IN ('PENDING','APPROVED','RETURNED') FOR UPDATE")
    List<Long> conflicts(@Param("placement") long placement,@Param("date") LocalDate date,@Param("except") long except);
    @Insert("INSERT INTO attendance_record(placement_id,attendance_date,record_type,status,note) VALUES(#{placementId},#{attendanceDate},#{recordType},#{status},#{note})")
    @Options(useGeneratedKeys=true,keyProperty="id") int create(AttendanceRecord r);
    @Update("UPDATE attendance_record SET status=#{status},note=#{note},reviewer_id=#{reviewerId} WHERE id=#{id}") int save(AttendanceRecord r);
    @Insert("INSERT INTO attendance_record_event(record_id,actor_id,action,attendance_date,record_type,note,feedback) VALUES(#{r.id},#{actor},#{r.status},#{r.attendanceDate},#{r.recordType},#{r.note},#{feedback})")
    int event(@Param("r") AttendanceRecord r,@Param("actor") long actor,@Param("feedback") String feedback);
    @Select("SELECT h.*,u.display_name AS actorName FROM attendance_record_event h JOIN user_account u ON u.id=h.actor_id WHERE record_id=#{id} ORDER BY h.id")
    List<Map<String,Object>> events(long id);
}
