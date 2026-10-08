package com.zhigangzong.mapper;
import com.zhigangzong.entity.GradeReview;
import org.apache.ibatis.annotations.*;
import java.util.*;
public interface GradeReviewMapper {
 @Select("SELECT * FROM grade_review_request WHERE id=#{id}") GradeReview lookup(long id);
 @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
 @Select("SELECT * FROM grade_review_request WHERE id=#{id} FOR UPDATE") GradeReview lock(long id);
 @Select("SELECT * FROM grade_review_request WHERE placement_id=#{id} ORDER BY id DESC") List<GradeReview> list(long id);
 @Select("SELECT id FROM grade_review_request WHERE placement_id=#{id} AND status IN ('PENDING','RETURNED') FOR UPDATE") List<Long> active(long id);
 @Insert("INSERT INTO grade_review_request(placement_id,reason,status,original_snapshot,requested_snapshot) VALUES(#{placementId},#{reason},'PENDING',#{originalSnapshot},#{requestedSnapshot})") @Options(useGeneratedKeys=true,keyProperty="id") int create(GradeReview r);
 @Update("UPDATE grade_review_request SET reason=#{reason},status=#{status},requested_snapshot=#{requestedSnapshot},reviewer_id=#{reviewerId},review_comment=#{reviewComment} WHERE id=#{id}") int save(GradeReview r);
 @Insert("INSERT INTO grade_review_event(request_id,actor_id,action,reason,comment,snapshot) VALUES(#{r.id},#{actor},#{r.status},#{r.reason},#{r.reviewComment},#{r.requestedSnapshot})") int event(@Param("r")GradeReview r,@Param("actor")long actor);
 @Select("SELECT e.*,u.display_name AS actorName FROM grade_review_event e JOIN user_account u ON u.id=e.actor_id WHERE request_id=#{id} ORDER BY e.id") List<Map<String,Object>> events(long id);
 @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,'成绩待复核','请查看实习记录中的成绩复核申请。','PLACEMENT',#{id} FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='SCHOOL_ADMIN' AND a.enabled=TRUE") int notifySchool(@Param("school")long school,@Param("id")long id);
}
