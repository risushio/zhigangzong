package com.zhigangzong.mapper;

import org.apache.ibatis.annotations.Select;

public interface HealthMapper {
    @Select("SELECT 1")
    int ping();

    @Select("SELECT DATABASE()")
    String database();

    @Select("SELECT VERSION()")
    String version();

    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'")
    int tableCount();
}
