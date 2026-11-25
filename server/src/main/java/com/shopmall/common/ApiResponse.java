package com.shopmall.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String traceId,
        String timestamp
) {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data,
                newTraceId(),
                TIMESTAMP_FORMATTER.format(Instant.now())
        );
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
                errorCode.getCode(),
                message,
                null,
                newTraceId(),
                TIMESTAMP_FORMATTER.format(Instant.now())
        );
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
