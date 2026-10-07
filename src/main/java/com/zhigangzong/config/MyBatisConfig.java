package com.zhigangzong.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.zhigangzong.mapper")
public class MyBatisConfig {
}
