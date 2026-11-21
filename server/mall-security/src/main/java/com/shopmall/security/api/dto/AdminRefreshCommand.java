package com.shopmall.security.api.dto;

public record AdminRefreshCommand(String refreshToken, String deviceId, String clientType) {
}
