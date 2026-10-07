package com.zhigangzong.mapper;

import com.zhigangzong.entity.Enterprise;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface EnterpriseMapper {
    @Select("SELECT id, name, credit_code, contact_name, contact_phone, qualification_ref, cooperation_notes, inspection_notes, review_status, suspension_reason, created_at FROM enterprise ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Enterprise> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM enterprise")
    long count();

    @Select("SELECT id, name, credit_code, contact_name, contact_phone, qualification_ref, cooperation_notes, inspection_notes, review_status, suspension_reason, created_at FROM enterprise WHERE id = #{id}")
    Enterprise findById(Long id);

    @Insert("INSERT INTO enterprise (name, credit_code, contact_name, contact_phone, qualification_ref, cooperation_notes, inspection_notes) "
            + "VALUES (#{name}, #{creditCode}, #{contactName}, #{contactPhone}, #{qualificationRef}, #{cooperationNotes}, #{inspectionNotes})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Enterprise entity);
}
