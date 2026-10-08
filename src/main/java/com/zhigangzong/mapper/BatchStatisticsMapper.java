package com.zhigangzong.mapper;
import com.zhigangzong.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface BatchStatisticsMapper {
 @Select("SELECT s.* FROM student_profile s JOIN user_account u ON u.id=s.user_id JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.department_id=#{department} AND u.role='STUDENT' AND a.enabled=TRUE ORDER BY s.id")List<StudentProfile> students(@Param("school")long school,@Param("department")long department);
 @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
 @Select("SELECT p.*,ar.status AS archiveStatus FROM internship_placement p LEFT JOIN placement_termination t ON t.placement_id=p.id LEFT JOIN placement_replacement r ON r.previous_placement_id=p.id LEFT JOIN placement_archive ar ON ar.placement_id=p.id WHERE p.batch_id=#{batch} AND p.school_approval_status='APPROVED' AND t.placement_id IS NULL AND r.previous_placement_id IS NULL ORDER BY p.id DESC")List<InternshipPlacement> placements(long batch);
}
