package com.zhigangzong.mapper;

import com.zhigangzong.entity.ArchiveRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface ArchiveRecordMapper {
    @Select("SELECT id, placement_id, summary_ref, appraisal_ref, status, reviewer_id, archived_at, created_at FROM archive_record ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<ArchiveRecord> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM archive_record")
    long count();

    @Select("SELECT id, placement_id, summary_ref, appraisal_ref, status, reviewer_id, archived_at, created_at FROM archive_record WHERE id = #{id}")
    ArchiveRecord findById(Long id);
}
