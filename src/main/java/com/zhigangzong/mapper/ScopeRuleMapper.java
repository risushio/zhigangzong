package com.zhigangzong.mapper;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface ScopeRuleMapper {
 @Select("SELECT d.id,d.name FROM department d WHERE d.school_id=#{school} ORDER BY d.id")List<Map<String,Object>> departments(long school);
 @Select("SELECT r.*,d.name AS departmentName,b.name AS batchName FROM scoped_workflow_policy r JOIN department d ON d.id=r.department_id LEFT JOIN internship_batch b ON b.id=r.batch_id WHERE d.school_id=#{school} ORDER BY r.id DESC")List<Map<String,Object>> list(long school);
 @Select("SELECT * FROM scoped_workflow_policy WHERE department_id=#{department} AND batch_key=#{batch} AND major=#{major} FOR UPDATE")Map<String,Object> find(@Param("department")long department,@Param("batch")long batch,@Param("major")String major);
 @Insert("INSERT INTO scoped_workflow_policy(department_id,batch_id,batch_key,major,settings,version) VALUES(#{department},#{batch},#{key},#{major},#{settings},1) ON DUPLICATE KEY UPDATE settings=VALUES(settings),version=version+1")int save(@Param("department")long department,@Param("batch")Long batch,@Param("key")long key,@Param("major")String major,@Param("settings")String settings);
 @Insert("INSERT INTO scoped_workflow_policy_event(policy_id,actor_id,version,settings) SELECT id,#{actor},version,settings FROM scoped_workflow_policy WHERE id=#{id}")int event(@Param("id")long id,@Param("actor")long actor);
 @Select("SELECT e.*,u.display_name AS actorName FROM scoped_workflow_policy_event e JOIN scoped_workflow_policy r ON r.id=e.policy_id JOIN department d ON d.id=r.department_id JOIN user_account u ON u.id=e.actor_id WHERE d.school_id=#{school} ORDER BY e.id DESC LIMIT 100")List<Map<String,Object>> history(long school);
 @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
 @Select("SELECT r.* FROM scoped_workflow_policy r JOIN internship_batch b ON b.department_id=r.department_id JOIN student_profile s ON s.id=#{student} WHERE b.id=#{batch} AND (r.batch_id IS NULL OR r.batch_id=b.id) AND (r.major='' OR r.major COLLATE utf8mb4_unicode_ci=s.major COLLATE utf8mb4_unicode_ci) ORDER BY (r.batch_id IS NOT NULL) DESC,(r.major<>'') DESC,r.id DESC LIMIT 1")Map<String,Object> effective(@Param("batch")long batch,@Param("student")long student);
 @Select("SELECT department_id FROM internship_batch WHERE id=#{id}")Long batchDepartment(long id);
 @Select("SELECT school_id FROM department WHERE id=#{id}")Long school(long id);
}
