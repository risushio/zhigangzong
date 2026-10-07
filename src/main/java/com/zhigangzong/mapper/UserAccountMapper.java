package com.zhigangzong.mapper;

import com.zhigangzong.entity.UserAccount;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface UserAccountMapper {
    @Select("SELECT id, school_id, department_id, display_name, role, email, created_at FROM user_account ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<UserAccount> findPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_account")
    long count();

    @Select("SELECT id, school_id, department_id, display_name, role, email, created_at FROM user_account WHERE id = #{id}")
    UserAccount findById(Long id);

    @Insert("INSERT INTO user_account (school_id, department_id, display_name, role, email) "
            + "VALUES (#{schoolId}, #{departmentId}, #{displayName}, #{role}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount entity);
}
