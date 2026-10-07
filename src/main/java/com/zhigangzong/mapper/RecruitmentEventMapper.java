package com.zhigangzong.mapper;

import com.zhigangzong.entity.RecruitmentEvent;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface RecruitmentEventMapper {
    @Select("SELECT id, application_id, actor_id, from_status, to_status, note, created_at FROM recruitment_event ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<RecruitmentEvent> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM recruitment_event")
    long count();

    @Select("SELECT id, application_id, actor_id, from_status, to_status, note, created_at FROM recruitment_event WHERE id = #{id}")
    RecruitmentEvent findById(Long id);
}
