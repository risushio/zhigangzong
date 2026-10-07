package com.zhigangzong.mapper;
import com.zhigangzong.dto.PortalRequests.SelfPlacement;
import com.zhigangzong.entity.*;
import org.apache.ibatis.annotations.*;
import java.util.*;

public interface SelfPlacementMapper {
    @Select("SELECT * FROM enterprise WHERE credit_code=#{code} FOR UPDATE") Enterprise enterprise(String code);
    @Insert("INSERT INTO enterprise(name,credit_code,contact_name,contact_phone,cooperation_notes) VALUES(#{name},#{creditCode},#{contactName},#{contactPhone},'学生自主申报单位，信息待学校核验')")
    @Options(useGeneratedKeys=true,keyProperty="id") int createEnterprise(Enterprise e);
    @Insert("INSERT INTO internship_placement(student_id,batch_id,source,enterprise_id,position_title,start_date,end_date) VALUES(#{studentId},#{batchId},'SELF',#{enterpriseId},#{positionTitle},#{startDate},#{endDate})")
    @Options(useGeneratedKeys=true,keyProperty="id") int create(InternshipPlacement p);
    @Insert("INSERT INTO self_placement_detail(placement_id,enterprise_name,credit_code,contact_name,contact_phone,address,duties) VALUES(#{id},#{r.enterpriseName},#{r.creditCode},#{r.contactName},#{r.contactPhone},#{r.address},#{r.duties})")
    int insertDetail(@Param("id")long id,@Param("r")SelfPlacement r);
    @Update("UPDATE self_placement_detail SET enterprise_name=#{r.enterpriseName},credit_code=#{r.creditCode},contact_name=#{r.contactName},contact_phone=#{r.contactPhone},address=#{r.address},duties=#{r.duties} WHERE placement_id=#{id}")
    int updateDetail(@Param("id")long id,@Param("r")SelfPlacement r);
    @Update("UPDATE internship_placement SET enterprise_id=#{enterpriseId},position_title=#{positionTitle},start_date=#{startDate},end_date=#{endDate} WHERE id=#{id}") int update(InternshipPlacement p);
    @Select("SELECT enterprise_name AS enterpriseName,credit_code AS creditCode,contact_name AS contactName,contact_phone AS contactPhone,address,duties FROM self_placement_detail WHERE placement_id=#{id}") Map<String,Object> detail(long id);
    @Insert("INSERT INTO self_placement_event(placement_id,actor_id,action,enterprise_id,enterprise_name,credit_code,contact_name,contact_phone,address,duties,position_title,start_date,end_date) SELECT p.id,#{actor},#{action},p.enterprise_id,d.enterprise_name,d.credit_code,d.contact_name,d.contact_phone,d.address,d.duties,p.position_title,p.start_date,p.end_date FROM internship_placement p JOIN self_placement_detail d ON d.placement_id=p.id WHERE p.id=#{id}")
    int event(@Param("id")long id,@Param("actor")long actor,@Param("action")String action);
    @Select("SELECT h.*,u.display_name AS actorName FROM self_placement_event h JOIN user_account u ON u.id=h.actor_id WHERE placement_id=#{id} ORDER BY h.id") List<Map<String,Object>> events(long id);
}
