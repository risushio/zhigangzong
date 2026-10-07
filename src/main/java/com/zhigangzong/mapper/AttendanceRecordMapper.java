package com.zhigangzong.mapper;

import com.zhigangzong.entity.AttendanceRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface AttendanceRecordMapper {
    @Select("SELECT id, placement_id, attendance_date, record_type, status, note, reviewer_id, created_at FROM attendance_record ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<AttendanceRecord> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM attendance_record")
    long count();

    @Select("SELECT id, placement_id, attendance_date, record_type, status, note, reviewer_id, created_at FROM attendance_record WHERE id = #{id}")
    AttendanceRecord findById(Long id);
}
