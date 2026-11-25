package com.shopmall.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.shopmall.mapper")
public class MyBatisConfig {
}
