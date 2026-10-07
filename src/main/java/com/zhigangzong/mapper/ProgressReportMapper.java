package com.zhigangzong.mapper;

import com.zhigangzong.entity.ProgressReport;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface ProgressReportMapper {
    @Select("SELECT id, placement_id, title, period_start, period_end, content, attachment_ref, status, reviewer_id, feedback, created_at FROM progress_report ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<ProgressReport> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM progress_report")
    long count();

    @Select("SELECT id, placement_id, title, period_start, period_end, content, attachment_ref, status, reviewer_id, feedback, created_at FROM progress_report WHERE id = #{id}")
    ProgressReport findById(Long id);
}
