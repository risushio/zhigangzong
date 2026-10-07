package com.zhigangzong.mapper;

import org.apache.ibatis.annotations.*;

public interface BootstrapMapper {
    @Select("SELECT COUNT(*) FROM auth_account")
    long accountCount();

    @Select("SELECT id FROM school WHERE code='ZGZ-LOCAL'")
    Long schoolId();

    @Insert("INSERT INTO auth_account(user_id,username,password_hash) VALUES(#{userId},'admin',#{hash})")
    int insertAccount(@Param("userId") long userId, @Param("hash") String hash);
}
