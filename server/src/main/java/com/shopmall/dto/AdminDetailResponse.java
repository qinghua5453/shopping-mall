package com.shopmall.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminDetailResponse(
        String id,
        String username,
        String nickname,
        String email,
        Integer status,
        Integer pwdResetRequired,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        List<String> roleIds
) {
}
