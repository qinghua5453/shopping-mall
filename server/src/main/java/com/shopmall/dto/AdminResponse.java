package com.shopmall.dto;

import java.time.LocalDateTime;

public record AdminResponse(
        String id,
        String username,
        String nickname,
        String email,
        Integer status,
        Integer pwdResetRequired,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
