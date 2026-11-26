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
    // ---- 优惠券相关 ----
    COUPON_NOT_FOUND(42000, "优惠券不存在", HttpStatus.NOT_FOUND),
    COUPON_NOT_STARTED(42001, "活动未开始", HttpStatus.CONFLICT),
    COUPON_EXPIRED(42002, "活动已结束", HttpStatus.CONFLICT),
    COUPON_SOLD_OUT(42003, "优惠券已领完", HttpStatus.CONFLICT),
    COUPON_OVER_LIMIT(42004, "超过每人限领数量", HttpStatus.CONFLICT),
    COUPON_NOT_OWNED(42005, "优惠券不属于当前用户", HttpStatus.NOT_FOUND),
    COUPON_CODE_INVALID(42006, "券码无效", HttpStatus.BAD_REQUEST),
    COUPON_CODE_USED(42007, "券码已被使用", HttpStatus.CONFLICT),
    COUPON_STATUS_INVALID(42008, "券状态不允许该操作", HttpStatus.CONFLICT),
    COUPON_CONDITION_NOT_MET(42009, "未满足使用条件", HttpStatus.BAD_REQUEST),
    COUPON_ALREADY_EXPIRED(42010, "优惠券已过期", HttpStatus.CONFLICT),
    COUPON_STACK_NOT_ALLOWED(42011, "不满足叠加规则", HttpStatus.CONFLICT),
    COUPON_FROZEN(42012, "优惠券已冻结", HttpStatus.CONFLICT),
    COUPON_CODE_VOIDED(42013, "券码已作废", HttpStatus.CONFLICT),
    IDEMPOTENCY_CONFLICT(42014, "幂等键已被不同请求使用", HttpStatus.CONFLICT),
    TEMPLATE_STATUS_INVALID(42020, "模板状态不允许该操作", HttpStatus.CONFLICT),
    TEMPLATE_PUBLISHED_FIELDS_LOCKED(42021, "已发布模板关键字段不可修改", HttpStatus.CONFLICT),
    TEMPLATE_HAS_RECORDS(42022, "模板存在领取记录，不可删除", HttpStatus.CONFLICT),
    BATCH_SIZE_TOO_LARGE(42030, "单次生成数量过大（上限 5000）", HttpStatus.BAD_REQUEST),
    BATCH_NOT_FOUND(42031, "批次不存在", HttpStatus.NOT_FOUND),
    RULE_DUPLICATE_GROUP(42040, "叠加规则重复", HttpStatus.CONFLICT),

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
