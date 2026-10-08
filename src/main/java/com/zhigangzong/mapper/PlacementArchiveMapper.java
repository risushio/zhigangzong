package com.zhigangzong.mapper;
import com.zhigangzong.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface PlacementArchiveMapper {
 @Select("SELECT * FROM change_request WHERE placement_id=#{id} ORDER BY id") List<ChangeRequest> changes(long id);
 @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,'结项待审核','请查看实习记录中的结项与归档申请。','PLACEMENT',#{id} FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='SCHOOL_ADMIN' AND a.enabled=TRUE") int notifySchool(@Param("school")long school,@Param("id")long id);

 @Select("SELECT p.batch_id AS batchId FROM internship_placement p JOIN student_profile s ON s.id=p.student_id JOIN user_account u ON u.id=s.user_id WHERE p.id=#{id} AND u.school_id=#{school}") Map<String,Object> scope(@Param("id")long id,@Param("school")long school);
 @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
 @Select("SELECT * FROM placement_archive WHERE placement_id=#{id} FOR UPDATE") PlacementArchive lock(long id);
 @Insert("INSERT INTO placement_archive(placement_id,result_file_id,summary,status,snapshot) VALUES(#{placementId},#{resultFileId},#{summary},'PENDING',#{snapshot})") @Options(useGeneratedKeys=true,keyProperty="id") int create(PlacementArchive r);
 @Update("UPDATE placement_archive SET result_file_id=#{resultFileId},summary=#{summary},status=#{status},snapshot=#{snapshot},reviewer_id=#{reviewerId},review_comment=#{reviewComment},archived_at=#{archivedAt} WHERE id=#{id}") int save(PlacementArchive r);
 @Insert("INSERT INTO placement_archive_event(archive_id,actor_id,action,summary,result_file_id,comment,snapshot) VALUES(#{r.id},#{actor},#{r.status},#{r.summary},#{r.resultFileId},#{r.reviewComment},#{r.snapshot})") int event(@Param("r")PlacementArchive r,@Param("actor")long actor);
 @Select("SELECT e.*,u.display_name AS actorName FROM placement_archive_event e JOIN user_account u ON u.id=e.actor_id WHERE archive_id=#{id} ORDER BY e.id") List<Map<String,Object>> events(long id);
 @Select("SELECT * FROM stored_file WHERE placement_id=#{id} ORDER BY id") List<StoredFile> files(long id);
 @Select("SELECT COUNT(*) FROM student_case WHERE placement_id=#{id} AND kind='WARNING' AND status!='CLOSED'") int warnings(long id);
}
