package com.shopmall.dto;

import com.shopmall.entity.CouponTemplate;

import java.time.format.DateTimeFormatter;

public record CouponTemplateResponse(
        Long id, String templateNo, String name, Integer couponType,
        Integer scopeType, String scopeValues, Long thresholdAmount,
        Long discountAmount, Double discountRate, Long maxDiscountAmount,
        Integer totalQuantity, Integer issuedQuantity, Integer perUserLimit,
        Integer validityType, String validStart, String validEnd, Integer validDays,
        String receiveStart, String receiveEnd, String stackGroup, Integer stackable,
        Integer refundPolicy, String receiveChannels, Integer sortWeight,
        Integer status, String remark, String createdAt, String updatedAt
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CouponTemplateResponse from(CouponTemplate t) {
        return new CouponTemplateResponse(
                t.getId(), t.getTemplateNo(), t.getName(), t.getCouponType(),
                t.getScopeType(), t.getScopeValues(), t.getThresholdAmount(),
                t.getDiscountAmount(), t.getDiscountRate(), t.getMaxDiscountAmount(),
                t.getTotalQuantity(), t.getIssuedQuantity(), t.getPerUserLimit(),
                t.getValidityType(),
                t.getValidStart() == null ? null : t.getValidStart().format(FMT),
                t.getValidEnd() == null ? null : t.getValidEnd().format(FMT),
                t.getValidDays(),
                t.getReceiveStart() == null ? null : t.getReceiveStart().format(FMT),
                t.getReceiveEnd() == null ? null : t.getReceiveEnd().format(FMT),
                t.getStackGroup(), t.getStackable(),
                t.getRefundPolicy(), t.getReceiveChannels(), t.getSortWeight(),
                t.getStatus(), t.getRemark(),
                t.getCreatedAt() == null ? null : t.getCreatedAt().format(FMT),
                t.getUpdatedAt() == null ? null : t.getUpdatedAt().format(FMT)
        );
    }
}
