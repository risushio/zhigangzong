package com.zhigangzong.mapper;

import com.zhigangzong.entity.StudentProfile;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface StudentProfileMapper {
    @Select("SELECT id, user_id, student_no, major, skills, project_experience, resume_ref, preferred_city, available_from, available_to, days_per_week, created_at FROM student_profile ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<StudentProfile> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM student_profile")
    long count();

    @Select("SELECT id, user_id, student_no, major, skills, project_experience, resume_ref, preferred_city, available_from, available_to, days_per_week, created_at FROM student_profile WHERE id = #{id}")
    StudentProfile findById(Long id);

    @Insert("INSERT INTO student_profile (user_id, student_no, major, skills, project_experience, resume_ref, preferred_city, available_from, available_to, days_per_week) "
            + "VALUES (#{userId}, #{studentNo}, #{major}, #{skills}, #{projectExperience}, #{resumeRef}, #{preferredCity}, #{availableFrom}, #{availableTo}, #{daysPerWeek})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudentProfile entity);
}
