package com.shopmall.security.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmall.common.error.BizException;
import com.shopmall.common.error.ErrorCode;
import com.shopmall.security.config.AuthProperties;
import com.shopmall.security.domain.model.AdminSession;
import com.shopmall.security.domain.model.SessionCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSessionService {
    private static final String SESSION_PREFIX = "auth:admin:session:";
    private static final String REFRESH_PREFIX = "auth:admin:refresh:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final RedisScript<Long> CREATE_SCRIPT = RedisScript.of("""
            redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
            redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])
            return 1
            """, Long.class);
    private static final RedisScript<Long> CONSUME_SCRIPT = RedisScript.of("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then
                return 0
            end
            redis.call('DEL', KEYS[1])
            redis.call('DEL', KEYS[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthProperties properties;

    public SessionCredential createSession(Long adminId, String deviceId, String clientType) {
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = generateRefreshToken();
        String refreshHash = hash(refreshToken);
        long now = Instant.now().getEpochSecond();
        long expires = now + properties.refreshTokenTtl().toSeconds();
        AdminSession session = new AdminSession(
                sessionId, adminId, deviceId, clientType, refreshHash, now, expires);

        redisTemplate.execute(
                CREATE_SCRIPT,
                List.of(SESSION_PREFIX + sessionId, REFRESH_PREFIX + refreshHash),
                toJson(session),
                sessionId,
                String.valueOf(properties.refreshTokenTtl().toSeconds()));
        return new SessionCredential(sessionId, refreshToken);
    }

    public void validateSession(Long adminId, String sessionId) {
        AdminSession session = getSession(sessionId);
        if (session == null || !session.adminId().equals(adminId)) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
    }

    public AdminSession consumeRefreshToken(String refreshToken, String deviceId) {
        String refreshHash = hash(refreshToken);
        String refreshKey = REFRESH_PREFIX + refreshHash;
        String sessionId = redisTemplate.opsForValue().get(refreshKey);
        if (sessionId == null) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        AdminSession session = getSession(sessionId);
        if (session == null
                || !session.refreshTokenHash().equals(refreshHash)
                || !session.deviceId().equals(deviceId)) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long consumed = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(refreshKey, SESSION_PREFIX + sessionId),
                sessionId);
        if (consumed == null || consumed != 1L) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        return session;
    }

    public void logout(Long adminId, String sessionId) {
        AdminSession session = getSession(sessionId);
        if (session == null || !session.adminId().equals(adminId)) {
            return;
        }
        redisTemplate.delete(List.of(
                SESSION_PREFIX + sessionId,
                REFRESH_PREFIX + session.refreshTokenHash()));
    }

    private AdminSession getSession(String sessionId) {
        String json = redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AdminSession.class);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String toJson(AdminSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "r_" + HexFormat.of().formatHex(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
