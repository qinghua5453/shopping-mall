package com.shopmall.security.domain.model;

public record SessionCredential(String sessionId, String refreshToken) {
}
