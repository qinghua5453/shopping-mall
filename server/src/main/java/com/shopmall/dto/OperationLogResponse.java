package com.shopmall.dto;

import java.time.LocalDateTime;

public record OperationLogResponse(
        String id,
        String adminId,
        String username,
        String module,
        String operation,
        String method,
        String path,
        String params,
        Integer success,
        String errorMsg,
        Integer costMs,
        String traceId,
        String ip,
        String userAgent,
        LocalDateTime createdAt
) {
}
