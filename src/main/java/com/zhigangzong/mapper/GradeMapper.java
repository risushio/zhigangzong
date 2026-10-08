package com.zhigangzong.mapper;
import com.zhigangzong.entity.EvaluationEntry;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface GradeMapper {
 @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,'三方评价已提交','请查看实习记录中的三方评价与成绩复核。','PLACEMENT',#{id} FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='SCHOOL_ADMIN' AND a.enabled=TRUE") int notifySchool(@Param("school")long school,@Param("id")long id);

 @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
 @Select("SELECT * FROM batch_grade_policy WHERE batch_id=#{id} FOR UPDATE") Map<String,Object> policy(long id);
 @Insert("INSERT INTO batch_grade_policy(batch_id,version,settings) VALUES(#{id},1,#{settings}) ON DUPLICATE KEY UPDATE version=version+1,settings=VALUES(settings)") int savePolicy(@Param("id")long id,@Param("settings")String settings);
 @Insert("INSERT INTO batch_grade_policy_event(batch_id,actor_id,version,settings) SELECT batch_id,#{actor},version,settings FROM batch_grade_policy WHERE batch_id=#{id}") int policyEvent(@Param("id")long id,@Param("actor")long actor);
 @Select("SELECT e.*,u.display_name AS actorName FROM batch_grade_policy_event e JOIN user_account u ON u.id=e.actor_id WHERE batch_id=#{id} ORDER BY e.id") List<Map<String,Object>> policyEvents(long id);
 @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
 @Select("SELECT * FROM evaluation_entry WHERE placement_id=#{id} ORDER BY type FOR UPDATE") List<EvaluationEntry> entries(long id);
 @Insert("INSERT INTO evaluation_entry(placement_id,evaluator_id,type,status,score,comment,revision) VALUES(#{placementId},#{evaluatorId},#{type},'DRAFT',#{score},#{comment},1)") @Options(useGeneratedKeys=true,keyProperty="id") int create(EvaluationEntry e);
 @Update("UPDATE evaluation_entry SET evaluator_id=#{evaluatorId},status=#{status},score=#{score},comment=#{comment},revision=revision+1 WHERE id=#{id}") int save(EvaluationEntry e);
 @Insert("INSERT INTO evaluation_entry_event(entry_id,actor_id,action,note,snapshot) VALUES(#{id},#{actor},#{action},#{note},#{snapshot})") int event(@Param("id")long id,@Param("actor")long actor,@Param("action")String action,@Param("note")String note,@Param("snapshot")String snapshot);
 @Select("SELECT h.*,u.display_name AS actorName FROM evaluation_entry_event h JOIN user_account u ON u.id=h.actor_id WHERE entry_id=#{id} ORDER BY h.id") List<Map<String,Object>> events(long id);
}
