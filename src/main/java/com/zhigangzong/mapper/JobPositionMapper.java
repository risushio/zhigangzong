package com.zhigangzong.mapper;

import com.zhigangzong.entity.JobPosition;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface JobPositionMapper {
    @Select("SELECT id, enterprise_id, title, description, required_major, required_skills, city, start_date, end_date, days_per_week, headcount, monthly_pay, working_hours, application_deadline, review_status, publish_status, created_at FROM job_position ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<JobPosition> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM job_position")
    long count();

    @Select("SELECT id, enterprise_id, title, description, required_major, required_skills, city, start_date, end_date, days_per_week, headcount, monthly_pay, working_hours, application_deadline, review_status, publish_status, created_at FROM job_position WHERE id = #{id}")
    JobPosition findById(Long id);

    @Insert("INSERT INTO job_position (enterprise_id, title, description, required_major, required_skills, city, start_date, end_date, days_per_week, headcount, monthly_pay, working_hours, application_deadline) "
            + "VALUES (#{enterpriseId}, #{title}, #{description}, #{requiredMajor}, #{requiredSkills}, #{city}, #{startDate}, #{endDate}, #{daysPerWeek}, #{headcount}, #{monthlyPay}, #{workingHours}, #{applicationDeadline})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JobPosition entity);
}
