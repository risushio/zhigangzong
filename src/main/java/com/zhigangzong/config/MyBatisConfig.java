package com.zhigangzong.config;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScans({@MapperScan("com.zhigangzong.mapper"),
        @MapperScan(value="com.zhigangzong.agent",annotationClass=Mapper.class)})
public class MyBatisConfig {
}
