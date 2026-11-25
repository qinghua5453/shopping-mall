package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdminSessionService {
    private static final String SESSION_KEY_PREFIX = "auth:admin:session:";

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;

    public String create(Long adminId) {
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                SESSION_KEY_PREFIX + sessionId,
                String.valueOf(adminId),
                authProperties.tokenTtl().toSeconds(),
                TimeUnit.SECONDS
        );
        return sessionId;
    }

    public void validate(String sessionId, Long adminId) {
        String value = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId);
        if (value == null || !value.equals(String.valueOf(adminId))) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
    }

    public void delete(String sessionId) {
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
    }
}
