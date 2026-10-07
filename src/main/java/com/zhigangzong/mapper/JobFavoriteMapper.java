package com.zhigangzong.mapper;

import com.zhigangzong.entity.JobFavorite;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface JobFavoriteMapper {
    @Select("SELECT id, student_id, job_id, created_at FROM job_favorite ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<JobFavorite> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM job_favorite")
    long count();

    @Select("SELECT id, student_id, job_id, created_at FROM job_favorite WHERE id = #{id}")
    JobFavorite findById(Long id);
}
