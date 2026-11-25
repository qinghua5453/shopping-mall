package com.shopmall.util;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final Duration tokenTtl;

    public JwtUtil(AuthProperties properties) {
        byte[] secret = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET 至少需要 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.tokenTtl = properties.tokenTtl();
    }

    public String createToken(Long adminId, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim("sid", sessionId)
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenTtl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public AccessTokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"access".equals(claims.get("typ", String.class))) {
                throw new BizException(ErrorCode.TOKEN_INVALID);
            }
            return new AccessTokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get("sid", String.class)
            );
        } catch (ExpiredJwtException exception) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
    }

    public record AccessTokenClaims(Long adminId, String sessionId) {
    }
}
