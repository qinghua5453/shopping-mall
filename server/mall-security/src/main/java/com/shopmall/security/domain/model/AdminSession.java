package com.shopmall.security.domain.model;

public record AdminSession(
        String sessionId,
        Long adminId,
        String deviceId,
        String clientType,
        String refreshTokenHash,
        long createdAtEpochSecond,
        long expiresAtEpochSecond
) {
}
