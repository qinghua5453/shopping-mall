package com.shopmall.security.api.dto;

public record AdminLoginCommand(
        String username,
        String password,
        String deviceId,
        String clientType,
        String ip,
        String userAgent
) {
}
