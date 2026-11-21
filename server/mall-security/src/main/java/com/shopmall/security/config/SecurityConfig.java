package com.shopmall.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmall.common.api.ApiResponse;
import com.shopmall.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AdminAuthenticationFilter authenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(), ApiResponse.error(ErrorCode.UNAUTHORIZED));
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(ErrorCode.FORBIDDEN.getHttpStatus().value());
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(), ApiResponse.error(ErrorCode.FORBIDDEN));
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/admin/auth/login",
                                "/api/v1/admin/auth/refresh",
                                "/actuator/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
