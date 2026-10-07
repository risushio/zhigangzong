package com.zhigangzong.mapper;

import com.zhigangzong.entity.JobApplication;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface JobApplicationMapper {
    @Select("SELECT id, student_id, job_id, resume_ref, recruitment_status, interview_at, offer_details, student_confirmed, created_at FROM job_application ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<JobApplication> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM job_application")
    long count();

    @Select("SELECT id, student_id, job_id, resume_ref, recruitment_status, interview_at, offer_details, student_confirmed, created_at FROM job_application WHERE id = #{id}")
    JobApplication findById(Long id);
}
