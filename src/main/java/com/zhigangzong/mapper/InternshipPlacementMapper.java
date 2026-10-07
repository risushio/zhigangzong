package com.zhigangzong.mapper;

import com.zhigangzong.entity.InternshipPlacement;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface InternshipPlacementMapper {
    @Select("SELECT id, student_id, batch_id, source, application_id, enterprise_id, job_id, position_title, school_approval_status, arrival_status, teacher_id, enterprise_mentor_id, start_date, end_date, created_at FROM internship_placement ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<InternshipPlacement> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM internship_placement")
    long count();

    @Select("SELECT id, student_id, batch_id, source, application_id, enterprise_id, job_id, position_title, school_approval_status, arrival_status, teacher_id, enterprise_mentor_id, start_date, end_date, created_at FROM internship_placement WHERE id = #{id}")
    InternshipPlacement findById(Long id);
}
