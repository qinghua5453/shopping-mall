package com.shopmall.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication(scanBasePackages = "com.shopmall")
@ConfigurationPropertiesScan(basePackages = "com.shopmall")
@MapperScan("com.shopmall.security.infrastructure.persistence.mapper")
public class MallApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
    }
}
