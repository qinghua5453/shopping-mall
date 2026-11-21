package com.shopmall.security.application.service;

import com.shopmall.common.error.BizException;
import com.shopmall.common.error.ErrorCode;
import com.shopmall.security.config.AuthProperties;
import com.shopmall.security.domain.model.AccessTokenClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {
    private final SecretKey secretKey;
    private final AuthProperties properties;

    public TokenService(AuthProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long adminId, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .audience().add("admin").and()
                .claim("sid", sessionId)
                .claim("typ", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(secretKey)
                .compact();
    }

    public AccessTokenClaims parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!claims.getAudience().contains("admin") || !"access".equals(claims.get("typ"))) {
                throw new BizException(ErrorCode.AUDIENCE_MISMATCH);
            }
            String sessionId = claims.get("sid", String.class);
            if (sessionId == null || sessionId.isBlank()) {
                throw new BizException(ErrorCode.TOKEN_INVALID);
            }
            return new AccessTokenClaims(Long.valueOf(claims.getSubject()), sessionId);
        } catch (ExpiredJwtException exception) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | NumberFormatException exception) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
    }

    public long getAccessTokenExpiresIn() {
        return properties.accessTokenTtl().toSeconds();
    }
}
