package com.zhigangzong.mapper;
import org.apache.ibatis.annotations.*;
import java.util.Map;

public interface AuthMapper {
    @Select("SELECT a.username, a.password_hash AS passwordHash, a.enabled, u.id, u.display_name AS displayName, u.role, u.school_id AS schoolId FROM auth_account a JOIN user_account u ON u.id=a.user_id WHERE a.username=#{username}")
    Map<String,Object> find(String username);
}
