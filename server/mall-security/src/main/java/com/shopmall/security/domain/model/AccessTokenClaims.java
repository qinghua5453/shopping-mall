package com.shopmall.security.domain.model;

public record AccessTokenClaims(Long adminId, String sessionId) {
}
