package com.shopmall.dto;

import jakarta.validation.constraints.*;

public record CouponTemplateCreateRequest(
        @NotBlank(message = "券名称不能为空") @Size(max = 128) String name,
        @NotNull(message = "券类型不能为空") Integer couponType,
        @NotNull(message = "适用范围类型不能为空") Integer scopeType,
        String scopeValues,
        @NotNull(message = "门槛金额不能为空") Long thresholdAmount,
        @NotNull(message = "优惠金额不能为空") Long discountAmount,
        Double discountRate,
        Long maxDiscountAmount,
        @NotNull(message = "发放总量不能为空") Integer totalQuantity,
        @NotNull(message = "每人限领不能为空") Integer perUserLimit,
        @NotNull(message = "有效期类型不能为空") Integer validityType,
        String validStart,
        String validEnd,
        Integer validDays,
        String receiveStart,
        String receiveEnd,
        @NotBlank(message = "叠加组不能为空") String stackGroup,
        Integer stackable,
        Integer refundPolicy,
        String receiveChannels,
        Integer sortWeight,
        String remark
) {
}
