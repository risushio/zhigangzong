package com.zhigangzong.mapper;

import com.zhigangzong.entity.InternshipMaterial;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface InternshipMaterialMapper {
    @Select("SELECT id, placement_id, material_type, file_ref, review_status, review_comment, created_at FROM internship_material ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<InternshipMaterial> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM internship_material")
    long count();

    @Select("SELECT id, placement_id, material_type, file_ref, review_status, review_comment, created_at FROM internship_material WHERE id = #{id}")
    InternshipMaterial findById(Long id);
}
