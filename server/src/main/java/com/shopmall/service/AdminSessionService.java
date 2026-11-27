package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdminSessionService {
    private static final String SESSION_KEY_PREFIX = "auth:admin:session:";
    private static final String ADMIN_SESSIONS_PREFIX = "auth:admin:sessions:";

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;

    private long ttlSeconds() {
        return authProperties.tokenTtl().toSeconds();
    }

    public String create(Long adminId) {
        String sessionId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                SESSION_KEY_PREFIX + sessionId,
                String.valueOf(adminId),
                ttlSeconds(),
                TimeUnit.SECONDS
        );
        // 维护该管理员的 sessionId 集合，便于禁用/删号时一键下线
        String setKey = ADMIN_SESSIONS_PREFIX + adminId;
        redisTemplate.opsForSet().add(setKey, sessionId);
        redisTemplate.expire(setKey, ttlSeconds(), TimeUnit.SECONDS);
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

    /** 清除该管理员的全部会话（禁用/删号/重置密码后立即下线）。 */
    public void deleteAllByAdmin(Long adminId) {
        String setKey = ADMIN_SESSIONS_PREFIX + adminId;
        Set<String> sessionIds = redisTemplate.opsForSet().members(setKey);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            redisTemplate.delete(sessionIds.stream().map(s -> SESSION_KEY_PREFIX + s).toList());
        }
        redisTemplate.delete(setKey);
    }
}
