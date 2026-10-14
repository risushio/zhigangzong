package com.zhigangzong.mapper;

import com.zhigangzong.entity.StudentProfile;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface StudentProfileMapper {
    String VISIBLE="(user_id IS NOT NULL OR school_id=(SELECT u.school_id FROM auth_account a JOIN user_account u ON u.id=a.user_id WHERE a.username=#{username} AND a.enabled=TRUE AND u.role='SCHOOL_ADMIN'))";
    static String username() {
        var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication==null?null:authentication.getName();
    }
    default List<StudentProfile> findPage(int offset,int limit) {return findVisiblePage(offset,limit,username());}
    @Select("SELECT * FROM student_profile WHERE "+VISIBLE+" ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<StudentProfile> findVisiblePage(@Param("offset") int offset,@Param("limit") int limit,@Param("username") String username);

    default long count() {return countVisible(username());}
    @Select("SELECT COUNT(*) FROM student_profile WHERE "+VISIBLE)
    long countVisible(String username);

    default StudentProfile findById(Long id) {return findVisibleById(id,username());}
    @Select("SELECT * FROM student_profile WHERE id=#{id} AND "+VISIBLE)
    StudentProfile findVisibleById(@Param("id") Long id,@Param("username") String username);

    record Administrator(long id, long schoolId) {}
    @Select("SELECT u.id,u.school_id AS schoolId FROM auth_account a JOIN user_account u ON u.id=a.user_id WHERE a.username=#{username} AND a.enabled=TRUE AND u.role='SCHOOL_ADMIN'")
    Administrator administrator(String username);
    @Select("SELECT u.id,u.school_id AS schoolId FROM auth_account a JOIN user_account u ON u.id=a.user_id WHERE a.username=#{username} AND a.enabled=TRUE AND u.role='SCHOOL_ADMIN' FOR UPDATE")
    Administrator lockAdministrator(String username);
    @Select("SELECT id FROM school WHERE id=#{schoolId} FOR UPDATE")
    Long lockSchool(long schoolId);
    @Select("SELECT COUNT(*) FROM student_profile WHERE school_id=#{schoolId} AND student_no=#{studentNo}")
    int countStudentNumber(@Param("schoolId") long schoolId, @Param("studentNo") String studentNo);
    @Select("SELECT COUNT(*) FROM department WHERE id=#{departmentId} AND school_id=#{schoolId}")
    int departmentInSchool(@Param("departmentId") long departmentId, @Param("schoolId") long schoolId);

    @Insert("INSERT INTO student_profile (user_id, school_id, name, department_id, phone, student_no, major, grade_year, class_name, skills, project_experience, resume_ref, preferred_city, available_from, available_to, days_per_week) "
            + "VALUES (#{userId}, COALESCE(#{schoolId},(SELECT school_id FROM user_account WHERE id=#{userId})), "
            + "COALESCE(#{name},(SELECT display_name FROM user_account WHERE id=#{userId})), "
            + "COALESCE(#{departmentId},(SELECT department_id FROM user_account WHERE id=#{userId})), #{phone}, "
            + "#{studentNo}, #{major}, #{gradeYear}, #{className}, #{skills}, #{projectExperience}, #{resumeRef}, #{preferredCity}, #{availableFrom}, #{availableTo}, #{daysPerWeek})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudentProfile entity);
}
