package com.zhigangzong.mapper;

import com.zhigangzong.entity.ChangeRequest;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface TerminationMapper {
    @Select("SELECT * FROM change_request WHERE placement_id=#{id} AND change_type='TERMINATE' ORDER BY id DESC") List<ChangeRequest> list(long id);
    @Select("SELECT * FROM change_request WHERE id=#{id} AND change_type='TERMINATE'") ChangeRequest lookup(long id);
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    @Select("SELECT * FROM change_request WHERE id=#{id} AND change_type='TERMINATE' FOR UPDATE") ChangeRequest lock(long id);
    @Insert("INSERT INTO change_request(placement_id,change_type,original_snapshot,requested_snapshot,reason,status) VALUES(#{placementId},'TERMINATE',#{originalSnapshot},#{requestedSnapshot},#{reason},'PENDING')")
    @Options(useGeneratedKeys=true,keyProperty="id") int create(ChangeRequest r);
    @Insert("INSERT INTO placement_termination(placement_id,request_id) VALUES(#{placement},#{request})")
    int terminate(@Param("placement")long placement,@Param("request")long request);
    @Insert("INSERT INTO notification(recipient_id,title,content,business_type,business_id) SELECT u.id,'实习终止待审批','学生已提交终止申请，请在实习终止办理中核查。','PLACEMENT',#{id} FROM user_account u JOIN auth_account a ON a.user_id=u.id WHERE u.school_id=#{school} AND u.role='SCHOOL_ADMIN' AND a.enabled=TRUE")
    int notifySchool(@Param("school")long school,@Param("id")long id);
}
