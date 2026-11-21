package com.shopmall.common.api;

import com.shopmall.common.error.ErrorCode;
import org.slf4j.MDC;

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

    private static final String TRACE_ID_KEY = "traceId";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data,
                currentTraceId(),
                currentTimestamp()
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
                currentTraceId(),
                currentTimestamp()
        );
    }

    private static String currentTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }

    private static String currentTimestamp() {
        return TIMESTAMP_FORMATTER.format(Instant.now());
    }
}
