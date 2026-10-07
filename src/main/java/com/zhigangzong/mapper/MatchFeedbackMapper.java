package com.zhigangzong.mapper;

import com.zhigangzong.entity.MatchFeedback;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface MatchFeedbackMapper {
    @Select("SELECT id, student_id, job_id, feedback_type, reason, created_at FROM match_feedback ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<MatchFeedback> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM match_feedback")
    long count();

    @Select("SELECT id, student_id, job_id, feedback_type, reason, created_at FROM match_feedback WHERE id = #{id}")
    MatchFeedback findById(Long id);
}
