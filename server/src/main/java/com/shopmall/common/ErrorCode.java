package com.shopmall.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(0, "ok", HttpStatus.OK),
    PARAM_ERROR(40001, "参数校验失败", HttpStatus.BAD_REQUEST),
    BODY_INVALID(40002, "请求体格式错误", HttpStatus.BAD_REQUEST),
    CLIENT_TYPE_INVALID(40003, "不支持的客户端类型", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "未登录或缺少 Token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(40101, "Access Token 已过期", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(40102, "Token 已失效", HttpStatus.UNAUTHORIZED),
    BAD_CREDENTIALS(40103, "账号或密码错误", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(40104, "账号锁定，请稍后再试", HttpStatus.LOCKED),
    FORBIDDEN(40105, "无权限", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(40107, "账号已被禁用", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_INVALID(40108, "刷新令牌无效或已被重放", HttpStatus.UNAUTHORIZED),
    AUDIENCE_MISMATCH(40109, "身份类型不匹配", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    TOO_MANY_REQUESTS(42900, "请求过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS),
    SYSTEM_ERROR(50000, "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
