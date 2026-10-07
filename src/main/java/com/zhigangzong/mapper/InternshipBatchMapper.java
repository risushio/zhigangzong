package com.zhigangzong.mapper;

import com.zhigangzong.entity.InternshipBatch;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface InternshipBatchMapper {
    @Select("SELECT id, department_id, name, start_date, end_date, learning_objectives, task_requirements, material_requirements, grading_criteria, created_at FROM internship_batch ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<InternshipBatch> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM internship_batch")
    long count();

    @Select("SELECT id, department_id, name, start_date, end_date, learning_objectives, task_requirements, material_requirements, grading_criteria, created_at FROM internship_batch WHERE id = #{id}")
    InternshipBatch findById(Long id);

    @Insert("INSERT INTO internship_batch (department_id, name, start_date, end_date, learning_objectives, task_requirements, material_requirements, grading_criteria) "
            + "VALUES (#{departmentId}, #{name}, #{startDate}, #{endDate}, #{learningObjectives}, #{taskRequirements}, #{materialRequirements}, #{gradingCriteria})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InternshipBatch entity);
}
