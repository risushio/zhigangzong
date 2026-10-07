package com.zhigangzong.mapper;

import com.zhigangzong.entity.GuidanceRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface GuidanceRecordMapper {
    @Select("SELECT id, placement_id, mentor_id, contact_at, content, next_contact_at, created_at FROM guidance_record ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<GuidanceRecord> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM guidance_record")
    long count();

    @Select("SELECT id, placement_id, mentor_id, contact_at, content, next_contact_at, created_at FROM guidance_record WHERE id = #{id}")
    GuidanceRecord findById(Long id);
}
