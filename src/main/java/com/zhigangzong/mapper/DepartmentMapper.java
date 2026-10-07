package com.zhigangzong.mapper;

import com.zhigangzong.entity.Department;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface DepartmentMapper {
    @Select("SELECT id, school_id, name, code, created_at FROM department ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Department> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM department")
    long count();

    @Select("SELECT id, school_id, name, code, created_at FROM department WHERE id = #{id}")
    Department findById(Long id);

    @Insert("INSERT INTO department (school_id, name, code) "
            + "VALUES (#{schoolId}, #{name}, #{code})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Department entity);
}
