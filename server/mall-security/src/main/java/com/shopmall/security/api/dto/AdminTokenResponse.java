package com.shopmall.security.api.dto;

public record AdminTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
