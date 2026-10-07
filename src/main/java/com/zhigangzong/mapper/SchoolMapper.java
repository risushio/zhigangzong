package com.zhigangzong.mapper;

import com.zhigangzong.entity.School;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface SchoolMapper {
    @Select("SELECT id, name, code, created_at FROM school ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<School> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM school")
    long count();

    @Select("SELECT id, name, code, created_at FROM school WHERE id = #{id}")
    School findById(Long id);

    @Insert("INSERT INTO school (name, code) "
            + "VALUES (#{name}, #{code})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(School entity);
}
