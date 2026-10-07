package com.zhigangzong.mapper;

import com.zhigangzong.entity.ApprovalRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface ApprovalRecordMapper {
    @Select("SELECT id, placement_id, approver_id, decision, comment, created_at FROM approval_record ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<ApprovalRecord> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM approval_record")
    long count();

    @Select("SELECT id, placement_id, approver_id, decision, comment, created_at FROM approval_record WHERE id = #{id}")
    ApprovalRecord findById(Long id);
}
