package com.zhigangzong.mapper;

import com.zhigangzong.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.*;

public interface TransferMapper {
    @Select("SELECT * FROM change_request WHERE placement_id=#{id} AND change_type IN ('CHANGE_JOB','CHANGE_ENTERPRISE') ORDER BY id DESC") List<ChangeRequest> list(long id);
    @Select("SELECT * FROM change_request WHERE id=#{id} AND change_type IN ('CHANGE_JOB','CHANGE_ENTERPRISE')") ChangeRequest lookup(long id);
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM change_request WHERE id=#{id} AND change_type IN ('CHANGE_JOB','CHANGE_ENTERPRISE') FOR UPDATE") ChangeRequest lock(long id);
    @Insert("INSERT INTO change_request(placement_id,change_type,original_snapshot,requested_snapshot,reason,status) VALUES(#{placementId},#{changeType},#{originalSnapshot},#{requestedSnapshot},#{reason},'PENDING')")
    @Options(useGeneratedKeys=true,keyProperty="id") int create(ChangeRequest r);
    @Update("UPDATE change_request SET change_type=#{changeType},requested_snapshot=#{requestedSnapshot},reason=#{reason},status=#{status},reviewer_id=#{reviewerId},review_comment=#{reviewComment} WHERE id=#{id}") int save(ChangeRequest r);
    @Select("SELECT COUNT(*) FROM internship_placement WHERE application_id=#{id}") int usedApplication(long id);
    @Insert("INSERT INTO placement_replacement(previous_placement_id,next_placement_id,request_id) VALUES(#{previous},#{next},#{request})")
    int link(@Param("previous")long previous,@Param("next")long next,@Param("request")long request);
    @Select("SELECT next_placement_id FROM placement_replacement WHERE request_id=#{id}") Long replacement(long id);
    @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,'换岗或换单位待审批','学生已提交重新安排申请，请查看原实习的换岗与换单位办理。','PLACEMENT',#{id} FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='SCHOOL_ADMIN' AND a.enabled=TRUE")
    int notifySchool(@Param("school")long school,@Param("id")long id);
}
