package com.shopmall.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
    public AuthProperties {
        if (jwtSecret == null || jwtSecret.getBytes().length < 32) {
            throw new IllegalArgumentException("app.auth.jwt-secret 至少需要 32 字节");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("app.auth.access-token-ttl 必须大于 0");
        }
        if (refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
            throw new IllegalArgumentException("app.auth.refresh-token-ttl 必须大于 0");
        }
    }
}
