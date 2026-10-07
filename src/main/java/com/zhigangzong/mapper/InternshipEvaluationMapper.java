package com.zhigangzong.mapper;

import com.zhigangzong.entity.InternshipEvaluation;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface InternshipEvaluationMapper {
    @Select("SELECT id, placement_id, evaluator_id, evaluation_type, score, comment, status, created_at FROM internship_evaluation ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<InternshipEvaluation> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM internship_evaluation")
    long count();

    @Select("SELECT id, placement_id, evaluator_id, evaluation_type, score, comment, status, created_at FROM internship_evaluation WHERE id = #{id}")
    InternshipEvaluation findById(Long id);
}
