package com.zhigangzong.mapper;
import com.zhigangzong.entity.StudentProfile;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface StudentSheetMapper {
 @Select("SELECT s.* FROM student_profile s WHERE s.school_id=#{school} AND s.user_id IS NOT NULL ORDER BY s.id LIMIT 10001")List<StudentProfile> students(long school);
 @Select("SELECT COUNT(*) FROM student_profile s WHERE s.school_id=#{school} AND s.student_no=#{no}")int studentNo(@Param("school")long school,@Param("no")String no);
}
