package com.zhigangzong.mapper;

import com.zhigangzong.entity.ChangeRequest;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface ChangeRequestMapper {
    @Select("SELECT id, placement_id, change_type, original_snapshot, requested_snapshot, reason, status, reviewer_id, review_comment, created_at FROM change_request ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<ChangeRequest> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM change_request")
    long count();

    @Select("SELECT id, placement_id, change_type, original_snapshot, requested_snapshot, reason, status, reviewer_id, review_comment, created_at FROM change_request WHERE id = #{id}")
    ChangeRequest findById(Long id);
}
