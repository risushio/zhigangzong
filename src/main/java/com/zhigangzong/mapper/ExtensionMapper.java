package com.zhigangzong.mapper;

import com.zhigangzong.entity.ChangeRequest;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.*;

public interface ExtensionMapper {
    @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,'实习延期待审批','学生已提交延期申请，请在实习延期办理中查看。','PLACEMENT',#{id} FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='SCHOOL_ADMIN' AND a.enabled=TRUE")
    int notifySchool(@Param("school")long school,@Param("id")long id);
    @Select("SELECT * FROM change_request WHERE placement_id=#{id} AND change_type='EXTEND' ORDER BY id DESC") List<ChangeRequest> list(long id);
    @Select("SELECT * FROM change_request WHERE id=#{id} AND change_type='EXTEND'") ChangeRequest lookup(long id);
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM change_request WHERE id=#{id} AND change_type='EXTEND' FOR UPDATE") ChangeRequest lock(long id);
    @Select("SELECT id FROM change_request WHERE placement_id=#{id} AND status IN ('PENDING','RETURNED') FOR UPDATE") List<Long> active(long id);
    @Insert("INSERT INTO change_request(placement_id,change_type,original_snapshot,requested_snapshot,reason,status) VALUES(#{placementId},'EXTEND',#{originalSnapshot},#{requestedSnapshot},#{reason},'PENDING')")
    @Options(useGeneratedKeys=true,keyProperty="id") int create(ChangeRequest r);
    @Update("UPDATE change_request SET requested_snapshot=#{requestedSnapshot},reason=#{reason},status=#{status},reviewer_id=#{reviewerId},review_comment=#{reviewComment} WHERE id=#{id}") int save(ChangeRequest r);
    @Update("UPDATE internship_placement SET end_date=#{date} WHERE id=#{id}") int extend(@Param("id") long id,@Param("date") LocalDate date);
    @Insert("INSERT INTO change_request_event(request_id,actor_id,action,original_snapshot,requested_snapshot,reason,review_comment) VALUES(#{r.id},#{actor},#{r.status},#{r.originalSnapshot},#{r.requestedSnapshot},#{r.reason},#{r.reviewComment})")
    int event(@Param("r") ChangeRequest r,@Param("actor") long actor);
    @Select("SELECT h.*,u.display_name AS actorName FROM change_request_event h JOIN user_account u ON u.id=h.actor_id WHERE request_id=#{id} ORDER BY h.id") List<Map<String,Object>> events(long id);
}
