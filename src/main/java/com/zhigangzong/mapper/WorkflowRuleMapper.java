package com.zhigangzong.mapper;

import com.zhigangzong.entity.WorkflowRule;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface WorkflowRuleMapper {
    @Select("SELECT id, department_id, batch_id, major, attendance_enabled, report_frequency_days, approval_steps, material_template, evaluation_weights, created_at FROM workflow_rule ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<WorkflowRule> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM workflow_rule")
    long count();

    @Select("SELECT id, department_id, batch_id, major, attendance_enabled, report_frequency_days, approval_steps, material_template, evaluation_weights, created_at FROM workflow_rule WHERE id = #{id}")
    WorkflowRule findById(Long id);
}
