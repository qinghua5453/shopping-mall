package com.shopmall.security.application.service;

import com.shopmall.common.error.BizException;
import com.shopmall.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AdminLoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_TTL = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "auth:admin:login:fail:";

    private final StringRedisTemplate redisTemplate;

    public void assertAllowed(String username, String ip) {
        String value = redisTemplate.opsForValue().get(key(username, ip));
        if (value != null && Integer.parseInt(value) >= MAX_FAILURES) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    public void recordFailure(String username, String ip) {
        String key = key(username, ip);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, LOCK_TTL);
        }
    }

    public void reset(String username, String ip) {
        redisTemplate.delete(key(username, ip));
    }

    private String key(String username, String ip) {
        return KEY_PREFIX + username.toLowerCase() + ":" + ip;
    }
}
