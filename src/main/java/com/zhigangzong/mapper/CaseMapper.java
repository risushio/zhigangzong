package com.zhigangzong.mapper;
import com.zhigangzong.entity.StudentCase;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface CaseMapper {
    String SCOPE=" FROM student_case c WHERE c.school_id=#{a.schoolId}<if test=\"a.role == 'STUDENT'\"> AND c.student_id=(SELECT id FROM student_profile WHERE user_id=#{a.id})</if><if test=\"a.role == 'TEACHER'\"> AND c.owner_id=#{a.id}</if>";
    @Select("<script>SELECT c.*"+SCOPE+" ORDER BY c.id DESC LIMIT #{size} OFFSET #{offset}</script>") List<StudentCase> list(@Param("a")PortalMapper.Actor a,@Param("size")int size,@Param("offset")int offset);
    @Select("<script>SELECT COUNT(*)"+SCOPE+"</script>") long count(@Param("a")PortalMapper.Actor a);
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM student_case WHERE id=#{id} FOR UPDATE") StudentCase lock(long id);
    @Insert("INSERT INTO student_case(school_id,student_id,placement_id,kind,title,description,rule_key,evidence) VALUES(#{schoolId},#{studentId},#{placementId},#{kind},#{title},#{description},#{ruleKey},#{evidence})")
    @Options(useGeneratedKeys=true,keyProperty="id") int create(StudentCase c);
    @Update("UPDATE student_case SET owner_id=#{ownerId},status=#{status},resolution=#{resolution},student_confirmed=#{studentConfirmed} WHERE id=#{id}") int save(StudentCase c);
    @Insert("INSERT INTO student_case_event(case_id,actor_id,action,note,snapshot) VALUES(#{id},#{actor},#{action},#{note},#{snapshot})")
    int event(@Param("id")long id,@Param("actor")long actor,@Param("action")String action,@Param("note")String note,@Param("snapshot")String snapshot);
    @Select("SELECT h.*,u.display_name AS actorName FROM student_case_event h JOIN user_account u ON u.id=h.actor_id WHERE case_id=#{id} ORDER BY h.id") List<Map<String,Object>> events(long id);
    @Select("SELECT u.id,u.display_name AS name FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND a.enabled=TRUE AND u.role IN ('SCHOOL_ADMIN','TEACHER') ORDER BY u.id") List<Map<String,Object>> owners(long school);
    @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,'求助或预警待分派','请在求助与预警中核查并分派责任人。','CASE',#{id} FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='SCHOOL_ADMIN' AND a.enabled=TRUE") int notifySchool(@Param("school")long school,@Param("id")long id);
}
