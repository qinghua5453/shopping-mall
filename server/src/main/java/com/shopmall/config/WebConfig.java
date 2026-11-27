package com.shopmall.config;

import com.shopmall.interceptor.LoginInterceptor;
import com.shopmall.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 先认证后鉴权：LoginInterceptor 注入 adminId，PermissionInterceptor 读取并校验权限
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/v1/admin/**")
                .excludePathPatterns("/api/v1/admin/auth/login");
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/v1/admin/**")
                .excludePathPatterns("/api/v1/admin/auth/login");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
