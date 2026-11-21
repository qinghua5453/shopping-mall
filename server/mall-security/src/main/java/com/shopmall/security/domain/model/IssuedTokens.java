package com.shopmall.security.domain.model;

public record IssuedTokens(String accessToken, String refreshToken, long expiresIn) {
}
